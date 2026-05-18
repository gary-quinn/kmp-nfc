package com.atruedev.kmpnfc.adapter

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import com.atruedev.kmpnfc.tag.TagType

internal fun TagType.toReaderFlag(): Int =
    when (this) {
        TagType.NFC_A -> NfcAdapter.FLAG_READER_NFC_A
        TagType.NFC_B -> NfcAdapter.FLAG_READER_NFC_B
        TagType.NFC_F -> NfcAdapter.FLAG_READER_NFC_F
        TagType.NFC_V -> NfcAdapter.FLAG_READER_NFC_V
        else -> 0
    }

internal fun TagType.toPlatformTechnology(): String? =
    when (this) {
        TagType.NFC_A -> NfcA::class.java.name
        TagType.NFC_B -> NfcB::class.java.name
        TagType.NFC_F -> NfcF::class.java.name
        TagType.NFC_V -> NfcV::class.java.name
        TagType.ISO_DEP -> IsoDep::class.java.name
        TagType.MIFARE_CLASSIC -> MifareClassic::class.java.name
        TagType.MIFARE_ULTRALIGHT -> MifareUltralight::class.java.name
        TagType.UNKNOWN -> null
    }

internal fun Set<TagType>.toReaderFlags(): Int = fold(0) { acc, type -> acc or type.toReaderFlag() }

internal fun Set<TagType>.toTechLists(): Array<Array<String>>? =
    mapNotNull { it.toPlatformTechnology()?.let { tech -> arrayOf(tech) } }
        .toTypedArray()
        .takeIf { it.isNotEmpty() }

internal fun Intent.extractNfcTag(): Tag? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(NfcAdapter.EXTRA_TAG)
    }
