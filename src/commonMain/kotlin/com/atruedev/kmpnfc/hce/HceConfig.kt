package com.atruedev.kmpnfc.hce

/**
 * Configuration for [HceService.start].
 *
 * @property aids AIDs to register for card emulation.
 */
public data class HceConfig(
    val aids: List<AidRegistration>,
)

/**
 * Registration entry for a single AID (Application ID).
 *
 * An AID is a hex string identifying the card application, e.g. "F0010203040506".
 * The NFC controller uses AIDs to route incoming SELECT commands to the correct
 * HCE service.
 *
 * @property aid Hex-encoded AID (5-16 bytes). Case-insensitive.
 * @property category [AidCategory.OTHER] for loyalty/access/transit;
 *   [AidCategory.PAYMENT] requires the app to be the default wallet.
 */
public data class AidRegistration(
    val aid: String,
    val category: AidCategory = AidCategory.OTHER,
) {
    init {
        require(aid.length in 10..32) {
            "AID must be 5-16 bytes as hex (10-32 chars), got ${aid.length}"
        }
        require(aid.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
            "AID must be hex-encoded, got: $aid"
        }
    }
}

/**
 * NFC card emulation category.
 *
 * Determines how the system routes and prioritizes the AID.
 */
public enum class AidCategory {
    /**
     * Payment applications (e.g. credit/debit cards).
     *
     * The user must set the app as the default Tap & Pay wallet in system
     * settings. Only one payment service can be active at a time.
     */
    PAYMENT,

    /**
     * Non-payment applications (loyalty, access control, transit, identity).
     *
     * Multiple apps can register the same AID; the user is prompted to choose
     * on first use.
     */
    OTHER,
}
