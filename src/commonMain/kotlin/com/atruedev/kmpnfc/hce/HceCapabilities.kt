package com.atruedev.kmpnfc.hce

/**
 * HCE capabilities of the device.
 *
 * Query before calling [HceService.start] to avoid runtime surprises.
 *
 * | Platform | isSupported | canPaymentCategory |
 * |----------|------------|-------------------|
 * | Android  | true       | true              |
 * | iOS      | false      | false             |
 * | JVM      | false      | false             |
 */
public data class HceCapabilities(
    val isSupported: Boolean,
    val canPaymentCategory: Boolean,
) {
    public companion object {
        /** Capabilities for a device with no HCE support. */
        public val NONE: HceCapabilities =
            HceCapabilities(
                isSupported = false,
                canPaymentCategory = false,
            )
    }
}
