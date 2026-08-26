package com.atruedev.kmpnfc.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Android [HostApduService] subclass registered in the library manifest.
 *
 * The Android system creates this service when an external NFC reader sends
 * a SELECT command matching one of the dynamically registered AIDs. This
 * class delegates to [AndroidHceService] via [HceServiceRegistry].
 *
 * Consumers do not instantiate or subclass this directly - the library
 * manifest auto-merges the service declaration into the host app.
 */
public class KmpNfcHostApduService : HostApduService() {
    private lateinit var hostBridge: HostApduBridge

    override fun onCreate() {
        super.onCreate()
        hostBridge = HostApduBridge { responseApdu -> sendResponseApdu(responseApdu) }
        HceServiceRegistry.bindHost(hostBridge)
    }

    override fun onDestroy() {
        HceServiceRegistry.unbindHost(hostBridge)
        super.onDestroy()
    }

    /**
     * Called by the NFC controller on the UI thread when an APDU command
     * is received from the external reader.
     *
     * Returns `null` for valid commands (response sent asynchronously via
     * [sendResponseApdu]). Returns `6F00` synchronously when there is no active
     * session or the bytes are malformed.
     */
    override fun processCommandApdu(
        commandApdu: ByteArray,
        extras: Bundle?,
    ): ByteArray? = processInboundApdu(commandApdu, HceServiceRegistry.get())

    /**
     * Called by the NFC controller when the reader disconnects or
     * deselects the current AID.
     */
    override fun onDeactivated(reason: Int) {
        val deactivationReason =
            when (reason) {
                DEACTIVATION_LINK_LOSS -> DeactivationReason.LINK_LOSS
                DEACTIVATION_DESELECTED -> DeactivationReason.DESELECTED
                else -> DeactivationReason.LINK_LOSS
            }
        HceServiceRegistry.get()?.onDeactivated(deactivationReason)
    }
}
