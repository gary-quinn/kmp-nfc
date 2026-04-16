package com.atruedev.kmpnfc.ndef

import com.atruedev.kmpnfc.error.NdefFormatError
import com.atruedev.kmpnfc.error.NfcException

/**
 * URI prefix codes defined by the NFC Forum URI RTD specification.
 * Index 0 = no prefix, index 1 = "http://www.", etc.
 */
internal val URI_PREFIXES: List<String> =
    listOf(
        "", // 0x00
        "http://www.", // 0x01
        "https://www.", // 0x02
        "http://", // 0x03
        "https://", // 0x04
        "tel:", // 0x05
        "mailto:", // 0x06
        "ftp://anonymous:anonymous@", // 0x07
        "ftp://ftp.", // 0x08
        "ftps://", // 0x09
        "sftp://", // 0x0A
        "smb://", // 0x0B
        "nfs://", // 0x0C
        "ftp://", // 0x0D
        "dav://", // 0x0E
        "news:", // 0x0F
        "telnet://", // 0x10
        "imap:", // 0x11
        "rtsp://", // 0x12
        "urn:", // 0x13
        "pop:", // 0x14
        "sip:", // 0x15
        "sips:", // 0x16
        "tftp:", // 0x17
        "btspp://", // 0x18
        "btl2cap://", // 0x19
        "btgoep://", // 0x1A
        "tcpobex://", // 0x1B
        "irdaobex://", // 0x1C
        "file://", // 0x1D
        "urn:epc:id:", // 0x1E
        "urn:epc:tag:", // 0x1F
        "urn:epc:pat:", // 0x20
        "urn:epc:raw:", // 0x21
        "urn:epc:", // 0x22
        "urn:nfc:", // 0x23
    )

internal fun encodeUriPayload(uri: String): ByteArray {
    var bestPrefixIndex = 0
    var bestPrefixLength = 0
    for (i in 1 until URI_PREFIXES.size) {
        val prefix = URI_PREFIXES[i]
        if (uri.startsWith(prefix) && prefix.length > bestPrefixLength) {
            bestPrefixIndex = i
            bestPrefixLength = prefix.length
        }
    }
    val remainder = uri.substring(bestPrefixLength)
    return byteArrayOf(bestPrefixIndex.toByte()) + remainder.encodeToByteArray()
}

internal fun decodeUriPayload(payload: ByteArray): String {
    if (payload.isEmpty()) return ""
    val prefixIndex = payload[0].toInt() and 0xFF
    val prefix = if (prefixIndex < URI_PREFIXES.size) URI_PREFIXES[prefixIndex] else ""
    return prefix + payload.copyOfRange(1, payload.size).decodeToString()
}

internal fun encodeTextPayload(
    text: String,
    locale: String,
    encoding: NdefTextEncoding,
): ByteArray {
    val localeBytes = locale.encodeToByteArray()
    val textBytes =
        when (encoding) {
            NdefTextEncoding.UTF_8 -> text.encodeToByteArray()
            NdefTextEncoding.UTF_16 -> encodeUtf16Be(text)
        }
    val statusByte =
        when (encoding) {
            NdefTextEncoding.UTF_8 -> localeBytes.size.toByte()
            NdefTextEncoding.UTF_16 -> (localeBytes.size or 0x80).toByte()
        }
    return byteArrayOf(statusByte) + localeBytes + textBytes
}

internal fun decodeTextPayload(payload: ByteArray): Triple<String, String, NdefTextEncoding> {
    if (payload.isEmpty()) return Triple("", "en", NdefTextEncoding.UTF_8)
    val status = payload[0].toInt() and 0xFF
    val isUtf16 = (status and 0x80) != 0
    val localeLength = status and 0x3F
    val locale = payload.copyOfRange(1, 1 + localeLength).decodeToString()
    val textBytes = payload.copyOfRange(1 + localeLength, payload.size)
    val text = if (isUtf16) decodeUtf16Be(textBytes) else textBytes.decodeToString()
    val encoding = if (isUtf16) NdefTextEncoding.UTF_16 else NdefTextEncoding.UTF_8
    return Triple(text, locale, encoding)
}

private const val UTF16_BOM = '\uFEFF'

private fun encodeUtf16Be(text: String): ByteArray {
    val chars = text.toCharArray()
    val bytes = ByteArray(chars.size * 2)
    for (i in chars.indices) {
        val code = chars[i].code
        bytes[i * 2] = (code shr 8).toByte()
        bytes[i * 2 + 1] = (code and 0xFF).toByte()
    }
    return bytes
}

private fun decodeUtf16Be(bytes: ByteArray): String {
    if (bytes.size % 2 != 0) {
        throw NfcException(NdefFormatError("UTF-16BE requires even byte count, got ${bytes.size}"))
    }
    val chars = CharArray(bytes.size / 2)
    for (i in chars.indices) {
        chars[i] = ((bytes[i * 2].toInt() and 0xFF shl 8) or (bytes[i * 2 + 1].toInt() and 0xFF)).toChar()
    }
    val decoded = chars.concatToString()
    // Strip BOM if present — U+FEFF as literal content is indistinguishable and will be lost.
    return if (decoded.startsWith(UTF16_BOM)) decoded.substring(1) else decoded
}
