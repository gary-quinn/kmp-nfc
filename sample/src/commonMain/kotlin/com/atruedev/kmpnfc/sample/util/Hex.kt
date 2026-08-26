package com.atruedev.kmpnfc.sample.util

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

fun ByteArray.toHexString(separator: String = " "): String =
    joinToString(separator) { byte ->
        val value = byte.toInt() and 0xFF
        "${HEX_CHARS[value ushr 4]}${HEX_CHARS[value and 0x0F]}"
    }

fun String.parseHexBytes(): ByteArray? {
    val cleaned = replace(" ", "").replace(":", "")
    if (cleaned.isEmpty() || cleaned.length % 2 != 0) return null
    if (!cleaned.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) return null
    return ByteArray(cleaned.length / 2) { index ->
        val start = index * 2
        cleaned.substring(start, start + 2).toInt(16).toByte()
    }
}

/** Validates an HCE AID: 5-16 bytes as hex (10-32 characters). */
fun String.isValidAidHex(): Boolean {
    val cleaned = replace(" ", "")
    return cleaned.length in 10..32 &&
        cleaned.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
}
