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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.Volatile

/**
 * Test double for [HceService].
 *
 * Allows tests to simulate incoming APDU commands and deactivation events.
 *
 * ```
 * val hce = FakeHceService()
 * hce.simulateStart(aids)
 *
 * val response = hce.simulateCommand(selectAid)
 * assertEquals(ApduResponse.success(), response)
 *
 * hce.simulateDeactivation(DeactivationReason.LINK_LOSS)
 * ```
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

        try {
            suspendCancellableCoroutine<Unit> { cont ->
                continuation = cont
                cont.invokeOnCancellation {
                    cleanup()
                }
            }
        } catch (e: CancellationException) {
            val cause = e.cause
            if (cause is DeactivationException) throw cause
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
     * Suspends until the processor returns a response.
     */
    public suspend fun simulateCommand(command: ApduCommand): ApduResponse {
        val proc = checkNotNull(processor) { "HCE not started. Call start() or simulateStart() first." }
        val response = proc(command)
        responses.add(response)
        return response
    }

    /**
     * Simulate deactivation (reader moved away or deselected).
     *
     * Cancels the processor coroutine. If [start] was suspended,
     * it resumes with [DeactivationException].
     */
    public fun simulateDeactivation(reason: DeactivationReason) {
        continuation?.resumeWith(Result.failure(DeactivationException(reason)))
    }

    /** Convenience: start HCE for testing without a real processor. */
    public suspend fun simulateStart(vararg aids: String) {
        val aidRegistrations = aids.map { AidRegistration(it) }
        registeredAids.clear()
        registeredAids.addAll(aidRegistrations)
        isStarted = true
    }

    /** Convenience: set the processor after simulateStart. */
    public fun setProcessor(processor: suspend (ApduCommand) -> ApduResponse) {
        this.processor = processor
    }

    private fun cleanup() {
        isStarted = false
        processor = null
        continuation = null
    }
}
