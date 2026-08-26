package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse

/**
 * Routes an inbound APDU from [HostApduService.processCommandApdu] to the active
 * [HceSession].
 *
 * @return `null` when the command was dispatched asynchronously, or a synchronous
 *   error APDU (`6F00`) when there is no session or the bytes are malformed.
 */
internal fun processInboundApdu(
    commandApdu: ByteArray,
    session: HceSession?,
): ByteArray? {
    if (session == null) return ApduResponse.generalError().toBytes()
    val command =
        try {
            ApduCommand.fromBytes(commandApdu)
        } catch (_: IllegalArgumentException) {
            return ApduResponse.generalError().toBytes()
        }
    session.dispatch(command)
    return null
}
