package com.atruedev.kmpnfc.ndef

/**
 * NDEF record Type Name Format (TNF) field.
 *
 * Indicates the structure of the type field in an NDEF record,
 * as defined in the NFC Forum NDEF specification.
 */
public enum class TypeNameFormat {
    /** Empty record with no type or payload. */
    EMPTY,

    /** NFC Forum well-known type (RTD). */
    WELL_KNOWN,

    /** MIME media type as defined in RFC 2046. */
    MIME_MEDIA,

    /** Absolute URI as defined in RFC 3986. */
    ABSOLUTE_URI,

    /** NFC Forum external type (reverse domain notation). */
    EXTERNAL_TYPE,

    /** Unknown record type — payload type is unknown. */
    UNKNOWN,

    /** Unchanged — used in chunked NDEF records. */
    UNCHANGED,
}
