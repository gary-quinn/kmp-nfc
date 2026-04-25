package com.atruedev.kmpnfc.reader

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.atruedev.kmpnfc.adapter.resolveTagType
import com.atruedev.kmpnfc.adapter.resolveTechnologies
import com.atruedev.kmpnfc.error.NdefFormatError
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.ReadOnly
import com.atruedev.kmpnfc.error.TagLost
import com.atruedev.kmpnfc.error.TransceiveError
import com.atruedev.kmpnfc.error.UnsupportedOperation
import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.ndef.NdefRecord
import com.atruedev.kmpnfc.ndef.TypeNameFormat
import com.atruedev.kmpnfc.ndef.parseNdefRecord
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.nfc.tech.TagTechnology as AndroidTagTech

/**
 * Android NFC tag using connect-once semantics.
 *
 * Mutable connection state ([connectedTech]) is confined to [tagDispatcher]
 * (`limitedParallelism(1)`). [close] sets `@Volatile closed` from any thread and
 * dispatches native tech cleanup onto [tagDispatcher] to avoid cross-thread mutation.
 */
internal class AndroidNfcTag(
    private val tag: Tag,
    private val tagDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : NfcTag {
    override val identifier: ByteArray = tag.id

    override val type: TagType = tag.resolveTagType()

    override val technologies: Set<TagTechnology> = tag.resolveTechnologies()

    /** Only accessed from [tagDispatcher]. */
    private var connectedTech: AndroidTagTech? = null

    @Volatile
    private var closed = false

    override suspend fun readNdef(): NdefMessage? =
        withContext(tagDispatcher) {
            val ndef =
                ensureConnected(
                    android.nfc.tech.Ndef
                        .get(tag) ?: return@withContext null,
                )
            try {
                val ndefMessage = ndef.ndefMessage ?: return@withContext null
                ndefMessage.toKmpNdefMessage()
            } catch (e: android.nfc.TagLostException) {
                throw NfcException(TagLost(cause = e))
            } catch (e: java.io.IOException) {
                throw NfcException(NdefFormatError(message = e.message ?: "NDEF read failed", cause = e))
            }
        }

    override suspend fun writeNdef(message: NdefMessage) =
        withContext(tagDispatcher) {
            val rawNdef =
                android.nfc.tech.Ndef
                    .get(tag)
            if (rawNdef != null) {
                val ndef = ensureConnected(rawNdef)
                try {
                    if (!ndef.isWritable) throw NfcException(ReadOnly())
                    ndef.writeNdefMessage(message.toAndroidNdefMessage())
                } catch (e: NfcException) {
                    throw e
                } catch (e: android.nfc.TagLostException) {
                    throw NfcException(TagLost(cause = e))
                } catch (e: java.io.IOException) {
                    throw NfcException(NdefFormatError(message = e.message ?: "NDEF write failed", cause = e))
                }
            } else {
                val formatable =
                    ensureConnected(
                        android.nfc.tech.NdefFormatable
                            .get(tag)
                            ?: throw NfcException(UnsupportedOperation("writeNdef")),
                    )
                try {
                    formatable.format(message.toAndroidNdefMessage())
                } catch (e: NfcException) {
                    throw e
                } catch (e: android.nfc.TagLostException) {
                    throw NfcException(TagLost(cause = e))
                } catch (e: java.io.IOException) {
                    throw NfcException(NdefFormatError(message = e.message ?: "NDEF format failed", cause = e))
                }
            }
        }

    override suspend fun transceive(data: ByteArray): ByteArray =
        withContext(tagDispatcher) {
            val tech =
                ensureConnected(
                    resolveTransceiveTech()
                        ?: throw NfcException(UnsupportedOperation("transceive")),
                )
            try {
                transceiveWith(tech, data)
            } catch (e: NfcException) {
                throw e
            } catch (e: android.nfc.TagLostException) {
                throw NfcException(TagLost(cause = e))
            } catch (e: java.io.IOException) {
                throw NfcException(TransceiveError(message = e.message ?: "Transceive failed", cause = e))
            }
        }

    override fun close() {
        closed = true
        CoroutineScope(tagDispatcher).launch {
            connectedTech?.let { runCatching { it.close() } }
            connectedTech = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : AndroidTagTech> ensureConnected(tech: T): T {
        if (closed) throw NfcException(TagLost("Tag has been closed"))
        val current = connectedTech
        if (current != null && current.javaClass == tech.javaClass && current.isConnected) {
            return current as T
        }
        connectedTech = null
        current?.let { runCatching { it.close() } }
        tech.connect()
        connectedTech = tech
        return tech
    }

    private fun resolveTransceiveTech(): AndroidTagTech? =
        IsoDep.get(tag)
            ?: NfcA.get(tag)
            ?: NfcB.get(tag)
            ?: NfcF.get(tag)
            ?: NfcV.get(tag)

    private fun transceiveWith(
        tech: AndroidTagTech,
        data: ByteArray,
    ): ByteArray =
        when (tech) {
            is IsoDep -> tech.transceive(data)
            is NfcA -> tech.transceive(data)
            is NfcB -> tech.transceive(data)
            is NfcF -> tech.transceive(data)
            is NfcV -> tech.transceive(data)
            else -> throw NfcException(UnsupportedOperation("transceive"))
        }
}

private fun android.nfc.NdefMessage.toKmpNdefMessage(): NdefMessage =
    NdefMessage(records = records.map { it.toKmpNdefRecord() })

private fun android.nfc.NdefRecord.toKmpNdefRecord(): NdefRecord {
    val tnfValue =
        when (tnf) {
            android.nfc.NdefRecord.TNF_EMPTY -> TypeNameFormat.EMPTY
            android.nfc.NdefRecord.TNF_WELL_KNOWN -> TypeNameFormat.WELL_KNOWN
            android.nfc.NdefRecord.TNF_MIME_MEDIA -> TypeNameFormat.MIME_MEDIA
            android.nfc.NdefRecord.TNF_ABSOLUTE_URI -> TypeNameFormat.ABSOLUTE_URI
            android.nfc.NdefRecord.TNF_EXTERNAL_TYPE -> TypeNameFormat.EXTERNAL_TYPE
            android.nfc.NdefRecord.TNF_UNCHANGED -> TypeNameFormat.UNCHANGED
            else -> TypeNameFormat.UNKNOWN
        }
    return parseNdefRecord(tnfValue, type, payload)
}

private fun NdefMessage.toAndroidNdefMessage(): android.nfc.NdefMessage =
    android.nfc.NdefMessage(records.map { it.toAndroidNdefRecord() }.toTypedArray())

private fun NdefRecord.toAndroidNdefRecord(): android.nfc.NdefRecord {
    val tnfValue =
        when (tnf) {
            TypeNameFormat.EMPTY -> android.nfc.NdefRecord.TNF_EMPTY
            TypeNameFormat.WELL_KNOWN -> android.nfc.NdefRecord.TNF_WELL_KNOWN
            TypeNameFormat.MIME_MEDIA -> android.nfc.NdefRecord.TNF_MIME_MEDIA
            TypeNameFormat.ABSOLUTE_URI -> android.nfc.NdefRecord.TNF_ABSOLUTE_URI
            TypeNameFormat.EXTERNAL_TYPE -> android.nfc.NdefRecord.TNF_EXTERNAL_TYPE
            TypeNameFormat.UNKNOWN -> android.nfc.NdefRecord.TNF_UNKNOWN
            TypeNameFormat.UNCHANGED -> android.nfc.NdefRecord.TNF_UNCHANGED
        }
    // NDEF record ID is not modeled - optional per NFC Forum spec, rarely used in practice.
    return android.nfc.NdefRecord(tnfValue, type, byteArrayOf(), payload)
}
