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
import com.atruedev.kmpnfc.ndef.decodeTextPayload
import com.atruedev.kmpnfc.ndef.decodeUriPayload
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.nfc.tech.TagTechnology as AndroidTagTech

internal class AndroidNfcTag(
    private val tag: Tag,
) : NfcTag {
    override val identifier: ByteArray = tag.id

    override val type: TagType = tag.resolveTagType()

    override val technologies: Set<TagTechnology> = tag.resolveTechnologies()

    override suspend fun readNdef(): NdefMessage? =
        withContext(Dispatchers.IO) {
            val ndef =
                android.nfc.tech.Ndef
                    .get(tag) ?: return@withContext null
            try {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage ?: return@withContext null
                ndefMessage.toKmpNdefMessage()
            } catch (e: android.nfc.TagLostException) {
                throw NfcException(TagLost(cause = e))
            } catch (e: java.io.IOException) {
                throw NfcException(NdefFormatError(message = e.message ?: "NDEF read failed", cause = e))
            } finally {
                runCatching { ndef.close() }
            }
        }

    override suspend fun writeNdef(message: NdefMessage) =
        withContext(Dispatchers.IO) {
            val ndef =
                android.nfc.tech.Ndef
                    .get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    if (!ndef.isWritable) throw NfcException(ReadOnly())
                    ndef.writeNdefMessage(message.toAndroidNdefMessage())
                } catch (e: NfcException) {
                    throw e
                } catch (e: android.nfc.TagLostException) {
                    throw NfcException(TagLost(cause = e))
                } catch (e: java.io.IOException) {
                    throw NfcException(NdefFormatError(message = e.message ?: "NDEF write failed", cause = e))
                } finally {
                    runCatching { ndef.close() }
                }
            } else {
                val formatable =
                    android.nfc.tech.NdefFormatable
                        .get(tag)
                        ?: throw NfcException(UnsupportedOperation("writeNdef"))
                try {
                    formatable.connect()
                    formatable.format(message.toAndroidNdefMessage())
                } catch (e: NfcException) {
                    throw e
                } catch (e: android.nfc.TagLostException) {
                    throw NfcException(TagLost(cause = e))
                } catch (e: java.io.IOException) {
                    throw NfcException(NdefFormatError(message = e.message ?: "NDEF format failed", cause = e))
                } finally {
                    runCatching { formatable.close() }
                }
            }
        }

    override suspend fun transceive(data: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            val tech =
                resolveTransceiveTech()
                    ?: throw NfcException(UnsupportedOperation("transceive"))
            try {
                tech.connect()
                transceiveWith(tech, data)
            } catch (e: NfcException) {
                throw e
            } catch (e: android.nfc.TagLostException) {
                throw NfcException(TagLost(cause = e))
            } catch (e: java.io.IOException) {
                throw NfcException(TransceiveError(message = e.message ?: "Transceive failed", cause = e))
            } finally {
                runCatching { tech.close() }
            }
        }

    override fun close() {
        // Tag resources are cleaned up per-operation via tech.close() in each method.
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

    if (tnfValue == TypeNameFormat.WELL_KNOWN) {
        if (type.contentEquals(byteArrayOf(0x55))) {
            return NdefRecord.Uri(decodeUriPayload(payload))
        }
        if (type.contentEquals(byteArrayOf(0x54))) {
            val (text, locale, encoding) = decodeTextPayload(payload)
            return NdefRecord.Text(text, locale, encoding)
        }
    }

    if (tnfValue == TypeNameFormat.MIME_MEDIA) {
        return NdefRecord.MimeMedia(type.decodeToString(), payload)
    }

    if (tnfValue == TypeNameFormat.EXTERNAL_TYPE) {
        val fullType = type.decodeToString()
        val colonIndex = fullType.indexOf(':')
        return if (colonIndex >= 0) {
            NdefRecord.ExternalType(
                fullType.substring(0, colonIndex),
                fullType.substring(colonIndex + 1),
                payload,
            )
        } else {
            NdefRecord.ExternalType(fullType, "", payload)
        }
    }

    return NdefRecord.Unknown(tnfValue, type, payload)
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
    return android.nfc.NdefRecord(tnfValue, type, byteArrayOf(), payload)
}
