package com.atruedev.kmpnfc.adapter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Bundle
import com.atruedev.kmpnfc.error.AdapterDisabled
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.reader.AndroidNfcTag
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

internal class AndroidNfcAdapter(
    private val context: Context,
) : NfcAdapter {
    private val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
    private val androidAdapter = nfcManager?.defaultAdapter

    private val _state = MutableStateFlow(resolveAdapterState())
    override val state: StateFlow<NfcAdapterState> = _state.asStateFlow()

    override val capabilities: NfcCapabilities = resolveCapabilities()

    private val stateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action == android.nfc.NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    _state.value = resolveAdapterState()
                }
            }
        }

    init {
        context.registerReceiver(
            stateReceiver,
            IntentFilter(android.nfc.NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
        )
    }

    override fun tags(options: ReaderOptions): Flow<NfcTag> =
        callbackFlow {
            val activity =
                ActivityTracker.resumedActivity
                    ?: throw NfcException(
                        AdapterDisabled("NFC reader mode requires a resumed Activity in the foreground"),
                    )

            var flags = 0
            if (TagType.NFC_A in options.pollingTypes) flags = flags or android.nfc.NfcAdapter.FLAG_READER_NFC_A
            if (TagType.NFC_B in options.pollingTypes) flags = flags or android.nfc.NfcAdapter.FLAG_READER_NFC_B
            if (TagType.NFC_F in options.pollingTypes) flags = flags or android.nfc.NfcAdapter.FLAG_READER_NFC_F
            if (TagType.NFC_V in options.pollingTypes) flags = flags or android.nfc.NfcAdapter.FLAG_READER_NFC_V

            if (flags == 0) flags = android.nfc.NfcAdapter.FLAG_READER_NFC_A

            androidAdapter?.enableReaderMode(
                activity,
                { tag: Tag -> trySend(AndroidNfcTag(tag)) },
                flags,
                Bundle(),
            )

            awaitClose {
                androidAdapter?.disableReaderMode(activity)
            }
        }

    override fun close() {
        context.unregisterReceiver(stateReceiver)
    }

    private fun resolveAdapterState(): NfcAdapterState =
        when {
            androidAdapter == null -> NfcAdapterState.NOT_SUPPORTED
            androidAdapter.isEnabled -> NfcAdapterState.ON
            else -> NfcAdapterState.OFF
        }

    private fun resolveCapabilities(): NfcCapabilities {
        if (androidAdapter == null) return NfcCapabilities.NONE

        return NfcCapabilities(
            canReadNdef = true,
            canWriteNdef = true,
            canReadRawTag = true,
            // Android CAN dispatch NFC intents in the background via intent filters, but this
            // library uses reader mode which requires a foreground Activity. Background dispatch
            // is an app-manifest concern, not a library capability.
            canBackgroundRead = false,
            supportedTagTypes =
                setOf(
                    TagType.NFC_A,
                    TagType.NFC_B,
                    TagType.NFC_F,
                    TagType.NFC_V,
                    TagType.ISO_DEP,
                    TagType.MIFARE_CLASSIC,
                    TagType.MIFARE_ULTRALIGHT,
                ),
        )
    }
}

internal fun Tag.resolveTagType(): TagType =
    when {
        techList.contains(IsoDep::class.java.name) -> TagType.ISO_DEP
        techList.contains(NfcA::class.java.name) -> TagType.NFC_A
        techList.contains(NfcB::class.java.name) -> TagType.NFC_B
        techList.contains(NfcF::class.java.name) -> TagType.NFC_F
        techList.contains(NfcV::class.java.name) -> TagType.NFC_V
        techList.contains(MifareClassic::class.java.name) -> TagType.MIFARE_CLASSIC
        techList.contains(MifareUltralight::class.java.name) -> TagType.MIFARE_ULTRALIGHT
        else -> TagType.NFC_A
    }

internal fun Tag.resolveTechnologies(): Set<TagTechnology> =
    buildSet {
        for (tech in techList) {
            when (tech) {
                NfcA::class.java.name -> add(TagTechnology.NFC_A)
                NfcB::class.java.name -> add(TagTechnology.NFC_B)
                NfcF::class.java.name -> add(TagTechnology.NFC_F)
                NfcV::class.java.name -> add(TagTechnology.NFC_V)
                IsoDep::class.java.name -> add(TagTechnology.ISO_DEP)
                android.nfc.tech.Ndef::class.java.name -> add(TagTechnology.NDEF)
                android.nfc.tech.NdefFormatable::class.java.name -> add(TagTechnology.NDEF_FORMATABLE)
                MifareClassic::class.java.name -> add(TagTechnology.MIFARE_CLASSIC)
                MifareUltralight::class.java.name -> add(TagTechnology.MIFARE_ULTRALIGHT)
            }
        }
    }

public actual fun NfcAdapter(): NfcAdapter {
    val context = KmpNfc.requireContext()
    return AndroidNfcAdapter(context)
}
