package com.atruedev.kmpnfc.ndef

/**
 * Parse raw NDEF record fields into a typed [NdefRecord].
 *
 * Shared across Android and iOS — platform code maps platform TNF to [TypeNameFormat],
 * extracts type/payload bytes, then delegates here.
 */
internal fun parseNdefRecord(
    tnf: TypeNameFormat,
    type: ByteArray,
    payload: ByteArray,
): NdefRecord {
    if (tnf == TypeNameFormat.WELL_KNOWN) {
        if (type.contentEquals(RTD_URI)) {
            return NdefRecord.Uri(decodeUriPayload(payload))
        }
        if (type.contentEquals(RTD_TEXT)) {
            val (text, locale, encoding) = decodeTextPayload(payload)
            return NdefRecord.Text(text, locale, encoding)
        }
    }

    if (tnf == TypeNameFormat.MIME_MEDIA) {
        return NdefRecord.MimeMedia(type.decodeToString(), payload)
    }

    if (tnf == TypeNameFormat.EXTERNAL_TYPE) {
        val fullType = type.decodeToString()
        val colonIndex = fullType.indexOf(':')
        return if (colonIndex >= 0) {
            NdefRecord.ExternalType(
                fullType.substring(0, colonIndex),
                fullType.substring(colonIndex + 1),
                payload,
            )
        } else {
            NdefRecord.ExternalType(fullType, "", payload)
        }
    }

    return NdefRecord.Unknown(tnf, type, payload)
}

private val RTD_URI = byteArrayOf(0x55) // 'U'
private val RTD_TEXT = byteArrayOf(0x54) // 'T'
