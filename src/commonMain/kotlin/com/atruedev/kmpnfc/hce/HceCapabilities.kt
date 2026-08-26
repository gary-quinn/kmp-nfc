package com.atruedev.kmpnfc.hce

/**
 * HCE capabilities of the device.
 *
 * Query before calling [HceService.start] to avoid runtime surprises.
 *
 * On Android, [HceService.capabilities] is recomputed on each read so
 * [canPaymentCategory] reflects the current default Tap & Pay wallet state.
 *
 * | Field | Meaning |
 * |-------|---------|
 * | [isSupported] | Device has HCE hardware and this app can register AIDs |
 * | [canPaymentCategory] | This app is the default Tap & Pay wallet (required for [AidCategory.PAYMENT]) |
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
