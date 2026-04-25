package com.atruedev.kmpnfc.tag

/**
 * NFC tag technology types.
 *
 * Each type corresponds to a specific ISO standard or proprietary protocol.
 * Platform support varies - check [com.atruedev.kmpnfc.adapter.NfcCapabilities.supportedTagTypes]
 * before relying on a specific type.
 */
public enum class TagType {
    /** ISO 14443-3A - most common NFC tag type (NTAG, MIFARE Ultralight). */
    NFC_A,

    /** ISO 14443-3B - used in some government ID cards and transit systems. */
    NFC_B,

    /** JIS X 6319-4 (FeliCa) - prevalent in Japan for transit and payment. */
    NFC_F,

    /** ISO 15693 - vicinity cards with longer read range. */
    NFC_V,

    /** ISO 14443-4 - smart cards, passports, contactless payment. Required for Aliro. */
    ISO_DEP,

    /** NXP MIFARE Classic - Android only, proprietary. */
    MIFARE_CLASSIC,

    /** NXP MIFARE Ultralight - Android only, proprietary. */
    MIFARE_ULTRALIGHT,

    /** Unrecognized tag technology reported by the platform. */
    UNKNOWN,
    ;

    public companion object {
        /** Tag types that can be polled for. Excludes [UNKNOWN]. */
        public val pollable: Set<TagType> = entries.toSet() - UNKNOWN
    }
}
