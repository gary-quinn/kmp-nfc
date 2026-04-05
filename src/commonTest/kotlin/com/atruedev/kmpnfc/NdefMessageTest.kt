package com.atruedev.kmpnfc

import com.atruedev.kmpnfc.ndef.NdefRecord
import com.atruedev.kmpnfc.ndef.NdefTextEncoding
import com.atruedev.kmpnfc.ndef.TypeNameFormat
import com.atruedev.kmpnfc.ndef.ndefMessage
import com.atruedev.kmpnfc.ndef.parseNdefRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NdefMessageTest {
    @Test
    fun builderCreatesUriRecord() {
        val message = ndefMessage { uri("https://example.com") }
        assertEquals(1, message.records.size)
        val record = message.records[0]
        assertIs<NdefRecord.Uri>(record)
        assertEquals("https://example.com", record.uri)
        assertEquals(TypeNameFormat.WELL_KNOWN, record.tnf)
    }

    @Test
    fun builderCreatesTextRecord() {
        val message = ndefMessage { text("Hello", locale = "en") }
        assertEquals(1, message.records.size)
        val record = message.records[0]
        assertIs<NdefRecord.Text>(record)
        assertEquals("Hello", record.text)
        assertEquals("en", record.locale)
        assertEquals(NdefTextEncoding.UTF_8, record.encoding)
    }

    @Test
    fun builderCreatesMimeMediaRecord() {
        val data = """{"key": "value"}""".encodeToByteArray()
        val message = ndefMessage { mimeMedia("application/json", data) }
        assertEquals(1, message.records.size)
        val record = message.records[0]
        assertIs<NdefRecord.MimeMedia>(record)
        assertEquals("application/json", record.mimeType)
        assertTrue(data.contentEquals(record.data))
    }

    @Test
    fun builderCreatesExternalTypeRecord() {
        val data = byteArrayOf(0x01, 0x02)
        val message = ndefMessage { externalType("com.example", "mytype", data) }
        assertEquals(1, message.records.size)
        val record = message.records[0]
        assertIs<NdefRecord.ExternalType>(record)
        assertEquals("com.example", record.domain)
        assertEquals("mytype", record.externalType)
        assertTrue(data.contentEquals(record.data))
    }

    @Test
    fun builderCreatesMultipleRecords() {
        val message =
            ndefMessage {
                uri("https://example.com")
                text("Hello NFC")
                mimeMedia("text/plain", "data".encodeToByteArray())
            }
        assertEquals(3, message.records.size)
        assertIs<NdefRecord.Uri>(message.records[0])
        assertIs<NdefRecord.Text>(message.records[1])
        assertIs<NdefRecord.MimeMedia>(message.records[2])
    }

    @Test
    fun uriRecordEncodesPayloadWithPrefix() {
        val record = NdefRecord.Uri("https://example.com")
        val payload = record.payload
        // 0x04 = "https://" prefix
        assertEquals(0x04, payload[0].toInt())
        assertEquals("example.com", payload.copyOfRange(1, payload.size).decodeToString())
    }

    @Test
    fun textRecordEncodesPayloadWithLocale() {
        val record = NdefRecord.Text("Hello", locale = "en")
        val payload = record.payload
        // Status byte: UTF-8, locale length 2
        assertEquals(2, payload[0].toInt() and 0x3F)
        assertEquals("en", payload.copyOfRange(1, 3).decodeToString())
        assertEquals("Hello", payload.copyOfRange(3, payload.size).decodeToString())
    }

    @Test
    fun uriRecordRoundtripThroughParseNdefRecord() {
        val original = NdefRecord.Uri("https://example.com/path?q=1")
        val parsed = parseNdefRecord(original.tnf, original.type, original.payload)
        assertIs<NdefRecord.Uri>(parsed)
        assertEquals(original.uri, parsed.uri)
    }

    @Test
    fun textRecordRoundtripThroughParseNdefRecord() {
        val original = NdefRecord.Text("Bonjour", locale = "fr", encoding = NdefTextEncoding.UTF_8)
        val parsed = parseNdefRecord(original.tnf, original.type, original.payload)
        assertIs<NdefRecord.Text>(parsed)
        assertEquals(original.text, parsed.text)
        assertEquals(original.locale, parsed.locale)
        assertEquals(original.encoding, parsed.encoding)
    }

    @Test
    fun mimeMediaRecordRoundtripThroughParseNdefRecord() {
        val data = """{"status":"ok"}""".encodeToByteArray()
        val original = NdefRecord.MimeMedia("application/json", data)
        val parsed = parseNdefRecord(original.tnf, original.type, original.payload)
        assertIs<NdefRecord.MimeMedia>(parsed)
        assertEquals(original.mimeType, parsed.mimeType)
        assertTrue(original.data.contentEquals(parsed.data))
    }

    @Test
    fun externalTypeRecordRoundtripThroughParseNdefRecord() {
        val data = byteArrayOf(0x01, 0x02, 0x03)
        val original = NdefRecord.ExternalType("com.example", "mytype", data)
        val parsed = parseNdefRecord(original.tnf, original.type, original.payload)
        assertIs<NdefRecord.ExternalType>(parsed)
        assertEquals(original.domain, parsed.domain)
        assertEquals(original.externalType, parsed.externalType)
        assertTrue(original.data.contentEquals(parsed.data))
    }

    @Test
    fun unknownRecordPreservedThroughParseNdefRecord() {
        val type = byteArrayOf(0xFF.toByte())
        val payload = byteArrayOf(0x01, 0x02)
        val parsed = parseNdefRecord(TypeNameFormat.UNKNOWN, type, payload)
        assertIs<NdefRecord.Unknown>(parsed)
        assertEquals(TypeNameFormat.UNKNOWN, parsed.tnf)
        assertTrue(type.contentEquals(parsed.type))
        assertTrue(payload.contentEquals(parsed.payload))
    }

    @Test
    fun multiRecordMessageRoundtripThroughParseNdefRecord() {
        val message =
            ndefMessage {
                uri("https://github.com")
                text("Hello NFC", locale = "en")
                mimeMedia("text/plain", "test".encodeToByteArray())
                externalType("com.test", "data", byteArrayOf(0x42))
            }
        val roundtripped =
            message.records.map { record ->
                parseNdefRecord(record.tnf, record.type, record.payload)
            }
        assertEquals(4, roundtripped.size)
        assertIs<NdefRecord.Uri>(roundtripped[0])
        assertEquals("https://github.com", (roundtripped[0] as NdefRecord.Uri).uri)
        assertIs<NdefRecord.Text>(roundtripped[1])
        assertEquals("Hello NFC", (roundtripped[1] as NdefRecord.Text).text)
        assertIs<NdefRecord.MimeMedia>(roundtripped[2])
        assertIs<NdefRecord.ExternalType>(roundtripped[3])
    }
}
