package com.atruedev.kmpnfc.hce

/**
 * Sends a response APDU back to the NFC controller.
 *
 * Bound while [KmpNfcHostApduService] is alive; outlives individual HCE sessions.
 */
internal fun interface HostApduBridge {
    fun sendResponseApdu(responseApdu: ByteArray)
}
