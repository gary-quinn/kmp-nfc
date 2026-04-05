package com.atruedev.kmpnfc.hce

import kotlinx.coroutines.flow.Flow

/**
 * Host Card Emulation — phone acts as a contactless smart card.
 *
 * Full support on Android (since 4.4). On iOS, requires Apple entitlement
 * and is limited to specific use cases (payments, transit, access, loyalty)
 * in approved regions (iOS 18.1+ globally, iOS 17.4+ in EEA).
 *
 * Check [com.atruedev.kmpnfc.adapter.NfcCapabilities.canHostCardEmulation]
 * before using. [startEmulation] throws [com.atruedev.kmpnfc.error.NfcException]
 * with [com.atruedev.kmpnfc.error.HceNotAvailable] if HCE is unavailable.
 */
public interface HceService : AutoCloseable {
    /**
     * Register an APDU command handler for a specific AID (Application Identifier).
     *
     * When an external reader selects this AID via SELECT command, the [handler]
     * receives all subsequent APDU commands for that application.
     */
    public fun registerAid(
        aid: ByteArray,
        handler: ApduHandler,
    )

    /**
     * Start card emulation — phone is now discoverable by NFC readers.
     *
     * @throws com.atruedev.kmpnfc.error.NfcException with [com.atruedev.kmpnfc.error.HceNotAvailable]
     *   if HCE is not available on this platform/region/configuration.
     */
    public suspend fun startEmulation()

    /** Stop card emulation. */
    public fun stopEmulation()

    /** Incoming APDU command stream from external readers. */
    public val commands: Flow<ApduCommand>

    override fun close()
}

/**
 * Processes an incoming APDU command and returns a response.
 */
public fun interface ApduHandler {
    public suspend fun processCommand(command: ApduCommand): ApduResponse
}
