package com.atruedev.kmpnfc

import com.atruedev.kmpnfc.ndef.NdefTextEncoding
import com.atruedev.kmpnfc.ndef.decodeTextPayload
import com.atruedev.kmpnfc.ndef.decodeUriPayload
import com.atruedev.kmpnfc.ndef.encodeTextPayload
import com.atruedev.kmpnfc.ndef.encodeUriPayload
import kotlin.test.Test
import kotlin.test.assertEquals
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
