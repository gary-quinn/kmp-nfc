package com.atruedev.kmpnfc

import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.TagLost
import com.atruedev.kmpnfc.ndef.NdefRecord
import com.atruedev.kmpnfc.ndef.ndefMessage
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import com.atruedev.kmpnfc.testing.FakeNfcTag
import com.atruedev.kmpnfc.testing.fakeNfcTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NfcTagTest {
    @Test
    fun readNdefReturnsConfiguredMessage() =
        runTest {
            val message = ndefMessage { uri("https://example.com") }
            val tag = FakeNfcTag(ndefMessage = message)

            val result = tag.readNdef()
            assertNotNull(result)
            assertEquals(1, result.records.size)
            assertIs<NdefRecord.Uri>(result.records[0])
            assertEquals("https://example.com", (result.records[0] as NdefRecord.Uri).uri)
        }

    @Test
    fun readNdefReturnsNullWhenNoMessage() =
        runTest {
            val tag = FakeNfcTag()
            assertNull(tag.readNdef())
        }

    @Test
    fun writeNdefRecordsMessage() =
        runTest {
            val tag = FakeNfcTag()
            val message = ndefMessage { text("Hello NFC") }

            tag.writeNdef(message)
            assertEquals(1, tag.writtenNdefMessages.size)
            assertEquals(message, tag.writtenNdefMessages[0])
        }

    @Test
    fun transceiveUsesConfiguredHandler() =
        runTest {
            val tag =
                fakeNfcTag {
                    onTransceive { command ->
                        byteArrayOf(0x90.toByte(), 0x00)
                    }
                }

            val response = tag.transceive(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00))
            assertEquals(2, response.size)
            assertEquals(0x90.toByte(), response[0])
            assertEquals(0x00, response[1])
        }

    @Test
    fun failWithInjectsError() =
        runTest {
            val tag =
                fakeNfcTag {
                    failWith(TagLost())
                }

            val ex = assertFailsWith<NfcException> { tag.readNdef() }
            assertIs<TagLost>(ex.error)
        }

    @Test
    fun builderConfiguresAllProperties() {
        val tag =
            fakeNfcTag {
                identifier(byteArrayOf(0x01, 0x02, 0x03, 0x04))
                type(TagType.ISO_DEP)
                technologies(setOf(TagTechnology.NFC_A, TagTechnology.ISO_DEP, TagTechnology.NDEF))
            }

        assertTrue(tag.identifier.contentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04)))
        assertEquals(TagType.ISO_DEP, tag.type)
        assertTrue(TagTechnology.ISO_DEP in tag.technologies)
    }

    @Test
    fun closePreventsRead() =
        runTest {
            val tag = FakeNfcTag(ndefMessage = ndefMessage { uri("https://example.com") })
            tag.readNdef()
            tag.close()
            val ex = assertFailsWith<NfcException> { tag.readNdef() }
            assertIs<TagLost>(ex.error)
        }

    @Test
    fun closePreventsWrite() =
        runTest {
            val tag = FakeNfcTag()
            tag.close()
            val ex = assertFailsWith<NfcException> { tag.writeNdef(ndefMessage { text("x") }) }
            assertIs<TagLost>(ex.error)
        }

    @Test
    fun closePreventsTransceive() =
        runTest {
            val tag = fakeNfcTag { onTransceive { byteArrayOf(0x90.toByte(), 0x00) } }
            tag.close()
            val ex = assertFailsWith<NfcException> { tag.transceive(byteArrayOf(0x00)) }
            assertIs<TagLost>(ex.error)
        }

    @Test
    fun closeIsIdempotent() {
        val tag = FakeNfcTag()
        tag.close()
        tag.close()
        assertTrue(tag.isClosed)
    }

    @Test
    fun isClosedReflectsState() {
        val tag = FakeNfcTag()
        assertFalse(tag.isClosed)
        tag.close()
        assertTrue(tag.isClosed)
    }

    @Test
    fun unknownExcludedFromPollable() {
        assertFalse(TagType.UNKNOWN in TagType.pollable)
        assertTrue(TagType.NFC_A in TagType.pollable)
    }
}
