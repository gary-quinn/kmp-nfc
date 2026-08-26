package com.atruedev.kmpnfc.testing

import com.atruedev.kmpnfc.hce.AidRegistration
import com.atruedev.kmpnfc.hce.DeactivationException
import com.atruedev.kmpnfc.hce.DeactivationReason
import com.atruedev.kmpnfc.hce.HceCapabilities
import com.atruedev.kmpnfc.hce.HceConfig
import com.atruedev.kmpnfc.hce.HceService
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
import kotlin.concurrent.Volatile

/**
 * Test double for [HceService].
 *
 * Launch [start] in a background coroutine, then drive commands with
 * [simulateCommand] and [simulateDeactivation].
 */
public class FakeHceService(
    override val capabilities: HceCapabilities =
        HceCapabilities(
            isSupported = true,
            canPaymentCategory = true,
        ),
) : HceService {
    /** AIDs registered via the last [start] call. */
    public val registeredAids: MutableList<AidRegistration> = mutableListOf()

    /** Responses sent to the reader (in order). */
    public val responses: MutableList<ApduResponse> = mutableListOf()

    /** Whether [start] is currently suspended. */
    public var isStarted: Boolean = false
        private set

    private var processor: (suspend (ApduCommand) -> ApduResponse)? = null
    private var scope: CoroutineScope? = null

    @Volatile
    private var continuation: CancellableContinuation<Unit>? = null

    override suspend fun start(
        config: HceConfig,
        processor: suspend (ApduCommand) -> ApduResponse,
    ) {
        check(!isStarted) { "HCE session already active" }
        registeredAids.clear()
        registeredAids.addAll(config.aids)
        this.processor = processor
        isStarted = true

        val commandDispatcher = Dispatchers.Default.limitedParallelism(1)
        scope = CoroutineScope(commandDispatcher + Job())

        try {
            suspendCancellableCoroutine<Unit> { cont ->
                continuation = cont
            }
        } catch (e: CancellationException) {
            throw unwrapStartCancellation(e)
        } finally {
            cleanup()
        }
    }

    override fun stop() {
        continuation?.cancel()
    }

    /**
     * Simulate an incoming APDU command from the external reader.
     *
     * Commands are processed serially, matching [AndroidHceService].
     */
    public suspend fun simulateCommand(command: ApduCommand): ApduResponse {
        val activeScope = checkNotNull(scope) { "HCE not started. Call start() first." }
        val proc = checkNotNull(processor) { "HCE not started. Call start() first." }

        return suspendCancellableCoroutine { cont ->
            activeScope.launch {
                try {
                    if (!activeScope.isActive) return@launch
                    val response = proc(command)
                    responses.add(response)
                    cont.resumeWith(Result.success(response))
                } catch (e: CancellationException) {
                    cont.cancel(e)
                } catch (e: Throwable) {
                    val wrapped = CancellationException("Processor failed", e)
                    continuation?.cancel(wrapped)
                    cont.cancel(wrapped)
                }
            }
        }
    }

    /** Simulate reader deactivation (LINK_LOSS or DESELECTED). */
    public fun simulateDeactivation(reason: DeactivationReason) {
        continuation?.cancel(
            CancellationException("Deactivated: $reason", DeactivationException(reason)),
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

    private fun cleanup() {
        scope?.cancel()
        scope = null
        isStarted = false
        processor = null
        continuation = null
    }
}
