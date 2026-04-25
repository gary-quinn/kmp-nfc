package com.atruedev.kmpnfc

import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.ndef.NdefTextEncoding
import com.atruedev.kmpnfc.ndef.decodeTextPayload
import com.atruedev.kmpnfc.ndef.decodeUriPayload
import com.atruedev.kmpnfc.ndef.encodeTextPayload
import com.atruedev.kmpnfc.ndef.encodeUriPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NdefEncodingTest {
    @Test
    fun uriPrefixHttpsWww() {
        val payload = encodeUriPayload("https://www.example.com")
        assertEquals(0x02, payload[0].toInt())
        assertEquals("example.com", payload.copyOfRange(1, payload.size).decodeToString())
    }

    @Test
    fun uriPrefixHttps() {
        val payload = encodeUriPayload("https://example.com")
        assertEquals(0x04, payload[0].toInt())
        assertEquals("example.com", payload.copyOfRange(1, payload.size).decodeToString())
    }

    @Test
    fun uriPrefixTel() {
        val payload = encodeUriPayload("tel:+1234567890")
        assertEquals(0x05, payload[0].toInt())
        assertEquals("+1234567890", payload.copyOfRange(1, payload.size).decodeToString())
    }

    @Test
    fun uriPrefixMailto() {
        val payload = encodeUriPayload("mailto:user@example.com")
        assertEquals(0x06, payload[0].toInt())
        assertEquals("user@example.com", payload.copyOfRange(1, payload.size).decodeToString())
    }

    @Test
    fun uriNoMatchingPrefixUsesZero() {
        val payload = encodeUriPayload("custom://something")
        assertEquals(0x00, payload[0].toInt())
        assertEquals("custom://something", payload.copyOfRange(1, payload.size).decodeToString())
    }

    @Test
    fun uriRoundtripWithPrefix() {
        val original = "https://www.example.com/path?q=1"
        val payload = encodeUriPayload(original)
        val decoded = decodeUriPayload(payload)
        assertEquals(original, decoded)
    }

    @Test
    fun uriRoundtripNoPrefix() {
        val original = "custom://arbitrary"
        val payload = encodeUriPayload(original)
        val decoded = decodeUriPayload(payload)
        assertEquals(original, decoded)
    }

    @Test
    fun uriDecodeEmptyPayload() {
        assertEquals("", decodeUriPayload(byteArrayOf()))
    }

    @Test
    fun textEncodeUtf8() {
        val payload = encodeTextPayload("Hello", "en", NdefTextEncoding.UTF_8)
        val statusByte = payload[0].toInt() and 0xFF
        assertEquals(0, statusByte and 0x80)
        assertEquals(2, statusByte and 0x3F)
        assertEquals("en", payload.copyOfRange(1, 3).decodeToString())
        assertEquals("Hello", payload.copyOfRange(3, payload.size).decodeToString())
    }

    @Test
    fun textEncodeUtf16SetsHighBit() {
        val payload = encodeTextPayload("Hello", "en", NdefTextEncoding.UTF_16)
        val statusByte = payload[0].toInt() and 0xFF
        assertTrue((statusByte and 0x80) != 0)
        assertEquals(2, statusByte and 0x3F)
    }

    @Test
    fun textRoundtripUtf8() {
        val payload = encodeTextPayload("Test message", "en", NdefTextEncoding.UTF_8)
        val (text, locale, encoding) = decodeTextPayload(payload)
        assertEquals("Test message", text)
        assertEquals("en", locale)
        assertEquals(NdefTextEncoding.UTF_8, encoding)
    }

    @Test
    fun textRoundtripUtf16() {
        val payload = encodeTextPayload("日本語", "ja", NdefTextEncoding.UTF_16)
        val (text, locale, encoding) = decodeTextPayload(payload)
        assertEquals("日本語", text)
        assertEquals("ja", locale)
        assertEquals(NdefTextEncoding.UTF_16, encoding)
    }

    @Test
    fun textRoundtripLongLocale() {
        val payload = encodeTextPayload("Bonjour", "fr-FR", NdefTextEncoding.UTF_8)
        val (text, locale, encoding) = decodeTextPayload(payload)
        assertEquals("Bonjour", text)
        assertEquals("fr-FR", locale)
        assertEquals(NdefTextEncoding.UTF_8, encoding)
    }

    @Test
    fun textDecodeEmptyPayload() {
        val (text, locale, encoding) = decodeTextPayload(byteArrayOf())
        assertEquals("", text)
        assertEquals("en", locale)
        assertEquals(NdefTextEncoding.UTF_8, encoding)
    }

    @Test
    fun textEncodeUtf16ProducesCorrectBytes() {
        val payload = encodeTextPayload("A", "en", NdefTextEncoding.UTF_16)
        // status(1) + "en"(2) + UTF-16BE "A"(2) = 5 bytes
        assertEquals(5, payload.size)
        val textStart = 1 + 2 // skip status + locale
        // UTF-16BE for 'A' (U+0041) = 0x00, 0x41
        assertEquals(0x00, payload[textStart].toInt() and 0xFF)
        assertEquals(0x41, payload[textStart + 1].toInt() and 0xFF)
    }

    @Test
    fun textDecodeUtf16FromRealPayload() {
        // Manually construct UTF-16BE payload: status=0x82 (UTF-16, locale len 2), "en", UTF-16BE "Hi"
        val status = 0x82.toByte() // UTF-16 flag + locale length 2
        val locale = "en".encodeToByteArray()
        val textUtf16Be = byteArrayOf(0x00, 0x48, 0x00, 0x69) // "Hi" in UTF-16BE
        val payload = byteArrayOf(status) + locale + textUtf16Be

        val (text, decodedLocale, encoding) = decodeTextPayload(payload)
        assertEquals("Hi", text)
        assertEquals("en", decodedLocale)
        assertEquals(NdefTextEncoding.UTF_16, encoding)
    }

    @Test
    fun textRoundtripUtf16CJK() {
        val original = "漢字"
        val payload = encodeTextPayload(original, "zh", NdefTextEncoding.UTF_16)
        // Text portion should be 2 chars * 2 bytes = 4 bytes for BMP characters
        val textStart = 1 + 2 // status + "zh"
        assertEquals(4, payload.size - textStart)
        val (text, locale, encoding) = decodeTextPayload(payload)
        assertEquals(original, text)
        assertEquals("zh", locale)
        assertEquals(NdefTextEncoding.UTF_16, encoding)
    }

    @Test
    fun textRoundtripUtf16SurrogatePair() {
        // U+1F600 (😀) is a supplementary character - stored as 2 surrogate chars in Kotlin,
        // producing 4 bytes in UTF-16BE (D83D DE00).
        val original = "\uD83D\uDE00"
        val payload = encodeTextPayload(original, "en", NdefTextEncoding.UTF_16)
        val textStart = 1 + 2 // status + "en"
        assertEquals(4, payload.size - textStart)
        val (text, locale, encoding) = decodeTextPayload(payload)
        assertEquals(original, text)
        assertEquals("en", locale)
        assertEquals(NdefTextEncoding.UTF_16, encoding)
    }

    @Test
    fun textRoundtripUtf16EmptyString() {
        val payload = encodeTextPayload("", "en", NdefTextEncoding.UTF_16)
        val (text, locale, encoding) = decodeTextPayload(payload)
        assertEquals("", text)
        assertEquals("en", locale)
        assertEquals(NdefTextEncoding.UTF_16, encoding)
    }

    @Test
    fun textDecodeUtf16WithBomStripsIt() {
        // Some writers prepend BOM (0xFE 0xFF) to UTF-16BE text.
        val status = 0x82.toByte() // UTF-16 flag + locale length 2
        val locale = "en".encodeToByteArray()
        val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val textBytes = byteArrayOf(0x00, 0x41) // "A"
        val payload = byteArrayOf(status) + locale + bom + textBytes

        val (text, _, encoding) = decodeTextPayload(payload)
        assertEquals("A", text)
        assertEquals(NdefTextEncoding.UTF_16, encoding)
    }

    @Test
    fun textDecodeUtf16OddByteCountThrowsNfcException() {
        val status = 0x82.toByte()
        val locale = "en".encodeToByteArray()
        val oddBytes = byteArrayOf(0x00, 0x41, 0x00) // 3 bytes - invalid
        val payload = byteArrayOf(status) + locale + oddBytes

        val ex = assertFailsWith<NfcException> { decodeTextPayload(payload) }
        assertIs<com.atruedev.kmpnfc.error.NdefFormatError>(ex.error)
    }

    @Test
    fun uriPrefixSelectsBestMatch() {
        val payload = encodeUriPayload("http://www.example.com")
        assertEquals(0x01, payload[0].toInt())

        val payload2 = encodeUriPayload("http://example.com")
        assertEquals(0x03, payload2[0].toInt())
    }

    @Test
    fun uriAllPrefixesRoundtrip() {
        val testUris =
            listOf(
                "http://www.example.com",
                "https://www.example.com",
                "http://example.com",
                "https://example.com",
                "tel:123",
                "mailto:a@b.c",
                "ftp://ftp.example.com",
            )
        for (uri in testUris) {
            val roundtripped = decodeUriPayload(encodeUriPayload(uri))
            assertEquals(uri, roundtripped, "Roundtrip failed for: $uri")
        }
    }
}
