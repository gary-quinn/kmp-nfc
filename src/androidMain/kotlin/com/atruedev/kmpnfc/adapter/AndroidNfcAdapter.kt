package com.atruedev.kmpnfc.adapter

import android.app.Activity
import android.app.PendingIntent
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
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.atruedev.kmpnfc.error.AdapterDisabled
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.NoForegroundActivity
import com.atruedev.kmpnfc.error.NotSupported
import com.atruedev.kmpnfc.reader.AndroidNfcTag
import com.atruedev.kmpnfc.reader.AndroidScanMode
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.uuid.Uuid
import android.nfc.NfcAdapter as PlatformNfcAdapter

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
                if (intent.action == PlatformNfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    _state.value = resolveAdapterState()
                }
            }
        }

    init {
        context.registerReceiver(
            stateReceiver,
            IntentFilter(PlatformNfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
        )
    }

    override fun tags(options: ReaderOptions): Flow<NfcTag> =
        callbackFlow {
            val adapter = androidAdapter ?: throw NfcException(NotSupported())
            if (!adapter.isEnabled) throw NfcException(AdapterDisabled())
            val activity =
                ActivityTracker.resumedActivity
                    ?: throw NfcException(NoForegroundActivity())

            val pollingTypes = options.pollingTypes.ifEmpty { setOf(TagType.NFC_A) }
            val emit: (Tag) -> Unit = { trySend(AndroidNfcTag(it)) }
            val dispose =
                when (options.androidScanMode) {
                    AndroidScanMode.ReaderMode ->
                        startReaderMode(adapter, activity, pollingTypes, emit)
                    AndroidScanMode.ForegroundDispatch ->
                        startForegroundDispatch(adapter, activity, pollingTypes, emit)
                }

            awaitClose(dispose)
        }.flowOn(Dispatchers.Main.immediate)

    override fun close() {
        runCatching { context.unregisterReceiver(stateReceiver) }
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
            // Manifest intent filters can dispatch NFC in the background, but this library
            // owns a foreground reader session - that is an app-manifest concern, not a
            // library capability.
            canBackgroundRead = false,
            canHostCardEmulation = true,
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

private fun startReaderMode(
    adapter: PlatformNfcAdapter,
    activity: Activity,
    pollingTypes: Set<TagType>,
    emit: (Tag) -> Unit,
): () -> Unit {
    adapter.enableReaderMode(activity, emit, pollingTypes.toReaderFlags(), Bundle())
    return { runCatching { adapter.disableReaderMode(activity) } }
}

private fun startForegroundDispatch(
    adapter: PlatformNfcAdapter,
    activity: Activity,
    pollingTypes: Set<TagType>,
    emit: (Tag) -> Unit,
): () -> Unit {
    val action = "${activity.packageName}.kmpnfc.TAG_DISCOVERED.${Uuid.random()}"
    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                intent?.extractNfcTag()?.let(emit)
            }
        }
    ContextCompat.registerReceiver(
        activity,
        receiver,
        IntentFilter(action),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )

    val intent = Intent(action).setPackage(activity.packageName)
    val pendingFlags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    val pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, pendingFlags)

    adapter.enableForegroundDispatch(activity, pendingIntent, null, pollingTypes.toTechLists())

    return {
        runCatching { adapter.disableForegroundDispatch(activity) }
        runCatching { activity.unregisterReceiver(receiver) }
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
        else -> TagType.UNKNOWN
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
