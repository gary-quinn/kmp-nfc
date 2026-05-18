package com.atruedev.kmpnfc.adapter

import android.nfc.NfcAdapter
import com.atruedev.kmpnfc.tag.TagType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidNfcMappingsTest {
    @Test
    fun toPlatformTechnology_mapsEveryPollableType() {
        val expected =
            mapOf(
                TagType.NFC_A to "android.nfc.tech.NfcA",
                TagType.NFC_B to "android.nfc.tech.NfcB",
                TagType.NFC_F to "android.nfc.tech.NfcF",
                TagType.NFC_V to "android.nfc.tech.NfcV",
                TagType.ISO_DEP to "android.nfc.tech.IsoDep",
                TagType.MIFARE_CLASSIC to "android.nfc.tech.MifareClassic",
                TagType.MIFARE_ULTRALIGHT to "android.nfc.tech.MifareUltralight",
            )
        expected.forEach { (type, tech) -> assertEquals(tech, type.toPlatformTechnology()) }
    }

    @Test
    fun toPlatformTechnology_returnsNullForUnknown() {
        assertNull(TagType.UNKNOWN.toPlatformTechnology())
    }

    @Test
    fun toReaderFlag_mapsPollableTypes() {
        assertEquals(NfcAdapter.FLAG_READER_NFC_A, TagType.NFC_A.toReaderFlag())
        assertEquals(NfcAdapter.FLAG_READER_NFC_B, TagType.NFC_B.toReaderFlag())
        assertEquals(NfcAdapter.FLAG_READER_NFC_F, TagType.NFC_F.toReaderFlag())
        assertEquals(NfcAdapter.FLAG_READER_NFC_V, TagType.NFC_V.toReaderFlag())
    }

    @Test
    fun toReaderFlag_returnsZeroForUnsupportedReaderTypes() {
        assertEquals(0, TagType.ISO_DEP.toReaderFlag())
        assertEquals(0, TagType.MIFARE_CLASSIC.toReaderFlag())
        assertEquals(0, TagType.MIFARE_ULTRALIGHT.toReaderFlag())
        assertEquals(0, TagType.UNKNOWN.toReaderFlag())
    }

    @Test
    fun toReaderFlags_combinesViaBitwiseOr() {
        val combined = setOf(TagType.NFC_A, TagType.NFC_V).toReaderFlags()
        val expected = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_V
        assertEquals(expected, combined)
    }

    @Test
    fun toReaderFlags_emptySetReturnsZero() {
        assertEquals(0, emptySet<TagType>().toReaderFlags())
    }

    @Test
    fun toTechLists_filtersOutUnknownAndProducesPerTechArrays() {
        val techLists = setOf(TagType.NFC_A, TagType.UNKNOWN, TagType.ISO_DEP).toTechLists()
        assertTrue(techLists != null)
        val flat = techLists.map { it.single() }.toSet()
        assertEquals(setOf("android.nfc.tech.NfcA", "android.nfc.tech.IsoDep"), flat)
    }

    @Test
    fun toTechLists_returnsNullWhenAllUnknown() {
        assertNull(setOf(TagType.UNKNOWN).toTechLists())
        assertNull(emptySet<TagType>().toTechLists())
    }
}
