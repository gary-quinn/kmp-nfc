package com.atruedev.kmpnfc.hce

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import com.atruedev.kmpnfc.adapter.KmpNfc
import com.atruedev.kmpnfc.error.AdapterDisabled
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.NotSupported
import com.atruedev.kmpnfc.error.Unauthorized
import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android [HceService] implementation.
 *
 * Wires the common [HceService] interface to Android's [HostApduService]
 * infrastructure via [HceServiceRegistry].
 *
 * The [actual fun HceService] factory returns a single shared instance per
 * process, scoped to [KmpNfc.appContext].
 */
internal class AndroidHceService private constructor(
    private val appContext: Context,
) : HceService,
    HceSession {
    private val nfcAdapter: NfcAdapter? =
        appContext.getSystemService(Context.NFC_SERVICE) as? NfcAdapter

    private val cardEmulation: CardEmulation? =
        nfcAdapter?.let { CardEmulation.getInstance(it) }

    private val hostComponent: ComponentName =
        ComponentName(
            appContext.packageName,
            KmpNfcHostApduService::class.java.name,
        )

    /** Non-null when [start] is suspended, null otherwise. */
    @Volatile
    private var session: Session? = null

    override val capabilities: HceCapabilities
        get() = resolveCapabilities()

    override suspend fun start(
        config: HceConfig,
        processor: suspend (ApduCommand) -> ApduResponse,
    ) {
        check(session == null) {
            "HCE session already active. Call stop() before starting a new one."
        }

        val adapter = nfcAdapter ?: throw NfcException(NotSupported())
        if (!adapter.isEnabled) throw NfcException(AdapterDisabled())

        val emulation =
            cardEmulation
                ?: throw NfcException(NotSupported("CardEmulation not available"))

        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            throw NfcException(NotSupported("Host card emulation is not supported on this device"))
        }

        val paymentAids = config.aids.filter { it.category == AidCategory.PAYMENT }
        val otherAids = config.aids.filter { it.category == AidCategory.OTHER }

        if (paymentAids.isNotEmpty() &&
            !emulation.isDefaultServiceForCategory(hostComponent, CardEmulation.CATEGORY_PAYMENT)
        ) {
            throw NfcException(
                Unauthorized(
                    "Payment AIDs require the app to be the default Tap & Pay wallet. " +
                        "Use AidCategory.OTHER or prompt the user to set this app as default.",
                ),
            )
        }

        if (otherAids.isNotEmpty()) {
            val success =
                emulation.registerAidsForService(
                    hostComponent,
                    CardEmulation.CATEGORY_OTHER,
                    otherAids.map { it.aid.uppercase() },
                )
            if (!success) {
                throw NfcException(
                    Unauthorized("Failed to register AIDs. Another app may own these AIDs."),
                )
            }
        }

        if (paymentAids.isNotEmpty()) {
            val success =
                emulation.registerAidsForService(
                    hostComponent,
                    CardEmulation.CATEGORY_PAYMENT,
                    paymentAids.map { it.aid.uppercase() },
                )
            if (!success) {
                emulation.removeAidsForService(hostComponent, CardEmulation.CATEGORY_OTHER)
                throw NfcException(Unauthorized("Failed to register payment AIDs."))
            }
        }

        val commandDispatcher = Dispatchers.IO.limitedParallelism(1)
        val scope = CoroutineScope(commandDispatcher + Job())
        val current = Session(scope, processor, emulation, hostComponent, paymentAids, otherAids)
        session = current

        HceServiceRegistry.register(this)

        try {
            suspendCancellableCoroutine<Unit> { cont ->
                current.continuation = cont
            }
        } catch (e: CancellationException) {
            throw unwrapStartCancellation(e)
        } finally {
            current.cleanup()
        }
    }

    override fun stop() {
        session?.continuation?.cancel()
    }

    // --- Called from KmpNfcHostApduService ---

    override fun dispatch(command: ApduCommand) {
        val s = session ?: return
        val proc = s.processor
        s.scope.launch {
            try {
                if (!s.scope.isActive) return@launch
                val response = proc(command)
                if (!s.scope.isActive) return@launch
                HceServiceRegistry.getHostBridge()?.sendResponseApdu(response.toBytes())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                s.continuation?.cancel(CancellationException("Processor failed", e))
            }
        }
    }

    override fun onDeactivated(reason: DeactivationReason) {
        session?.continuation?.cancel(
            CancellationException("Deactivated: $reason", DeactivationException(reason)),
        )
    }

    private fun resolveCapabilities(): HceCapabilities {
        if (nfcAdapter == null || cardEmulation == null) return HceCapabilities.NONE
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            return HceCapabilities.NONE
        }
        return HceCapabilities(
            isSupported = true,
            canPaymentCategory =
                cardEmulation.isDefaultServiceForCategory(
                    hostComponent,
                    CardEmulation.CATEGORY_PAYMENT,
                ),
        )
    }

    private fun unwrapStartCancellation(e: CancellationException): Throwable {
        var current: Throwable? = e.cause
        while (current != null) {
            when (current) {
                is DeactivationException -> return current
                is CancellationException -> current = current.cause
                else -> return current
            }
        }
        return e
    }

    // --- Session ---

    private inner class Session(
        val scope: CoroutineScope,
        val processor: suspend (ApduCommand) -> ApduResponse,
        private val emulation: CardEmulation,
        private val component: ComponentName,
        private val paymentAids: List<AidRegistration>,
        private val otherAids: List<AidRegistration>,
    ) {
        @Volatile
        var continuation: CancellableContinuation<Unit>? = null

        fun cleanup() {
            HceServiceRegistry.unregister(this@AndroidHceService)
            if (otherAids.isNotEmpty()) {
                emulation.removeAidsForService(component, CardEmulation.CATEGORY_OTHER)
            }
            if (paymentAids.isNotEmpty()) {
                emulation.removeAidsForService(component, CardEmulation.CATEGORY_PAYMENT)
            }
            scope.cancel()
            session = null
        }
    }

    // --- Singleton ---

    internal companion object {
        @Volatile
        private var instance: AndroidHceService? = null

        fun get(): AndroidHceService =
            instance ?: synchronized(this) {
                instance ?: AndroidHceService(KmpNfc.requireContext()).also { instance = it }
            }
    }
}

public actual fun HceService(): HceService = AndroidHceService.get()
