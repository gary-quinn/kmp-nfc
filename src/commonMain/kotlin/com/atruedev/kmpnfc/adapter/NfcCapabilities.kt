package com.atruedev.kmpnfc.adapter

import com.atruedev.kmpnfc.tag.TagType

/**
 * Capabilities of the device's NFC hardware.
 *
 * Query before using features to avoid runtime surprises.
 * Platform asymmetry is significant — Android supports all features,
 * while iOS support varies by version, region, and entitlement.
 */
public data class NfcCapabilities(
    /** Whether NDEF tag reading is supported. True on both platforms. */
    val canReadNdef: Boolean,
    /** Whether NDEF tag writing is supported. True on both platforms (iOS 13+). */
    val canWriteNdef: Boolean,
    /** Whether raw tag access (transceive) is supported. True on both (iOS 13+ with specific tag types). */
    val canReadRawTag: Boolean,
    /** Whether background tag reading is supported. False on Android (needs foreground); true on iOS (URL tags only). */
    val canBackgroundRead: Boolean,
    /** Tag types supported by this device's NFC hardware. */
    val supportedTagTypes: Set<TagType>,
) {
    public companion object {
        /** Capabilities for a device with no NFC support. */
        public val NONE: NfcCapabilities =
            NfcCapabilities(
                canReadNdef = false,
                canWriteNdef = false,
                canReadRawTag = false,
                canBackgroundRead = false,
                supportedTagTypes = emptySet(),
            )
    }
}
