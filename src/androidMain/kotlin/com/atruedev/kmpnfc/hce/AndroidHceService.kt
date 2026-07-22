package com.atruedev.kmpnfc.hce

import android.content.ComponentName
import android.content.Context
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
    context: Context,
) : HceService {
    private val nfcAdapter: NfcAdapter? =
        context.getSystemService(Context.NFC_SERVICE) as? NfcAdapter

    private val cardEmulation: CardEmulation? =
        nfcAdapter?.let { CardEmulation.getInstance(it) }

    /** Non-null when [start] is suspended, null otherwise. */
    @Volatile
    private var session: Session? = null

    override val capabilities: HceCapabilities =
        if (nfcAdapter != null && cardEmulation != null) {
            HceCapabilities(isSupported = true, canPaymentCategory = true)
        } else {
            HceCapabilities.NONE
        }

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

        val appContext = context.applicationContext
        val component =
            ComponentName(
                appContext.packageName,
                KmpNfcHostApduService::class.java.name,
            )

        val paymentAids = config.aids.filter { it.category == AidCategory.PAYMENT }
        val otherAids = config.aids.filter { it.category == AidCategory.OTHER }

        if (paymentAids.isNotEmpty() &&
            !emulation.isDefaultServiceForCategory(component, CardEmulation.CATEGORY_PAYMENT)
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
                    component,
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
                    component,
                    CardEmulation.CATEGORY_PAYMENT,
                    paymentAids.map { it.aid.uppercase() },
                )
            if (!success) {
                emulation.removeAidsForService(component, CardEmulation.CATEGORY_OTHER)
                throw NfcException(Unauthorized("Failed to register payment AIDs."))
            }
        }

        val scope = CoroutineScope(Dispatchers.IO + Job())
        val current = Session(scope, processor, emulation, component, paymentAids, otherAids)
        session = current

        HceServiceRegistry.register(this)

        try {
            suspendCancellableCoroutine<Unit> { cont ->
                current.continuation = cont
            }
        } catch (e: CancellationException) {
            val cause = e.cause
            if (cause is DeactivationException) throw cause
        } finally {
            current.cleanup()
        }
    }

    override fun stop() {
        val s = session ?: return
        s.continuation?.cancel()
    }

    // --- Called from KmpNfcHostApduService ---

    internal fun dispatch(command: ApduCommand) {
        val s = session ?: return
        val proc = s.processor
        s.scope.launch {
            try {
                val response = proc(command)
                HceServiceRegistry.getHostService()?.sendResponseApdu(response.toBytes())
            } catch (_: CancellationException) {
                // Session ended - discard.
            }
        }
    }

    internal fun onDeactivated(reason: DeactivationReason) {
        val s = session ?: return
        s.continuation?.cancel(
            CancellationException("Deactivated: $reason", DeactivationException(reason)),
        )
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
