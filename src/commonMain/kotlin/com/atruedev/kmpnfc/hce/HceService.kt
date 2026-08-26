package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse

/**
 * Host Card Emulation entry point.
 *
 * Enables the device to act as a contactless smart card. An external NFC reader
 * sends ISO 7816-4 APDU commands, and the [processor] responds.
 *
 * Obtain an instance via the [HceService] factory function.
 *
 * ```
 * val hce = HceService()
 * if (!hce.capabilities.isSupported) return
 *
 * try {
 *     hce.start(HceConfig(aids = listOf(AidRegistration("F0010203040506")))) { command ->
 *         when {
 *             command.isSelectAid() -> ApduResponse.success()
 *             else -> ApduResponse.instructionNotSupported()
 *         }
 *     }
 * } catch (e: DeactivationException) {
 *     // Reader disconnected
 * }
 * ```
 */
public interface HceService {
    /** Platform HCE capabilities. Query before calling [start]. */
    public val capabilities: HceCapabilities

    /**
     * Start card emulation. Suspends until [stop] is called or the external
     * reader disconnects.
     *
     * The [processor] runs on a background dispatcher - callers can perform
     * I/O, cryptography, or database lookups without blocking the NFC stack.
     *
     * Only one HCE session can be active at a time.
     *
     * @param config AIDs to register and emulation settings.
     * @param processor Called for each incoming APDU command. Must return
     *   an [ApduResponse].
     * @throws DeactivationException if the reader disconnects.
     * @throws IllegalStateException if an HCE session is already active.
     * @throws Exception if the [processor] throws (propagates to the caller of [start]).
     */
    public suspend fun start(
        config: HceConfig,
        processor: suspend (ApduCommand) -> ApduResponse,
    )

    /**
     * Stop card emulation. [start] returns normally.
     *
     * Safe to call when no session is active (no-op).
     */
    public fun stop()
}

/** Create a platform-specific [HceService] instance. */
public expect fun HceService(): HceService
