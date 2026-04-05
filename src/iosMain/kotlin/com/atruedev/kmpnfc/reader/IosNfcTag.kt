package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.TagLost
import com.atruedev.kmpnfc.error.TransceiveError
import com.atruedev.kmpnfc.error.UnsupportedOperation
import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.ndef.NdefRecord
import com.atruedev.kmpnfc.ndef.TypeNameFormat
import com.atruedev.kmpnfc.ndef.parseNdefRecord
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreNFC.NFCFeliCaTagProtocol
import platform.CoreNFC.NFCISO15693TagProtocol
import platform.CoreNFC.NFCISO7816TagProtocol
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFStatus
import platform.CoreNFC.NFCNDEFStatusNotSupported
import platform.CoreNFC.NFCNDEFStatusReadOnly
import platform.CoreNFC.NFCNDEFTagProtocol
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagReaderSession
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * iOS NFC tag implementation wrapping a Core NFC tag protocol object.
 *
 * Core NFC delivers tags as protocol types (NFCISO7816Tag, NFCISO15693Tag,
 * NFCFeliCaTag, NFCMiFareTag). This class resolves the actual tag protocol,
 * extracts the real identifier/type, and delegates operations accordingly.
 *
 * Connection to the tag is established lazily on first operation and reused
 * for subsequent calls — Core NFC does not support reconnecting to an
 * already-connected tag.
 */
internal class IosNfcTag(
    private val nfcTag: Any,
    private val session: NFCTagReaderSession,
) : NfcTag {
    private val tagProtocol: NFCTagProtocol? = nfcTag as? NFCTagProtocol
    private val iso7816Tag = nfcTag as? NFCISO7816TagProtocol
    private val iso15693Tag = nfcTag as? NFCISO15693TagProtocol
    private val felicaTag = nfcTag as? NFCFeliCaTagProtocol
    private val mifareTag = nfcTag as? NFCMiFareTagProtocol
    private val ndefTag = nfcTag as? NFCNDEFTagProtocol

    private val connected = kotlin.concurrent.AtomicInt(0)

    override val identifier: ByteArray = resolveIdentifier()

    override val type: TagType = resolveType()

    override val technologies: Set<TagTechnology> = resolveTechnologies()

    override suspend fun readNdef(): NdefMessage? {
        val ndef = ndefTag ?: throw NfcException(UnsupportedOperation("readNdef"))
        ensureConnected()
        val status = queryNdefStatus(ndef)
        if (status == NFCNDEFStatusNotSupported) return null
        return readNdefMessage(ndef)
    }

    override suspend fun writeNdef(message: NdefMessage) {
        val ndef = ndefTag ?: throw NfcException(UnsupportedOperation("writeNdef"))
        ensureConnected()
        val status = queryNdefStatus(ndef)
        if (status == NFCNDEFStatusNotSupported) {
            throw NfcException(UnsupportedOperation("writeNdef — tag does not support NDEF"))
        }
        if (status == NFCNDEFStatusReadOnly) {
            throw NfcException(
                com.atruedev.kmpnfc.error
                    .ReadOnly(),
            )
        }
        writeNdefMessage(ndef, message)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun transceive(data: ByteArray): ByteArray {
        ensureConnected()
        val iso7816 = iso7816Tag
        if (iso7816 != null) return sendApduToIso7816(iso7816, data)
        val mifareRef = mifareTag
        if (mifareRef != null) return sendDataToMiFare(mifareRef, data)
        throw NfcException(
            UnsupportedOperation("transceive — tag type does not support raw transceive on iOS"),
        )
    }

    override fun close() {
        // Session lifecycle manages tag connections on iOS.
    }

    private suspend fun ensureConnected() {
        if (!connected.compareAndSet(0, 1)) return
        val protocol =
            tagProtocol
                ?: throw NfcException(
                    UnsupportedOperation("Tag does not conform to NFCTagProtocol"),
                )
        suspendCoroutine { cont ->
            session.connectToTag(protocol) { error ->
                if (error != null) {
                    connected.value = 0
                    cont.resumeWithException(NfcException(TagLost(cause = error.toException())))
                } else {
                    cont.resume(Unit)
                }
            }
        }
    }

    private fun resolveIdentifier(): ByteArray =
        iso7816Tag
            ?.identifier
            ?.toByteArray()
            ?: mifareTag
                ?.identifier
                ?.toByteArray()
            ?: iso15693Tag
                ?.identifier
                ?.toByteArray()
            ?: felicaTag
                ?.currentIDm
                ?.toByteArray()
            ?: byteArrayOf()

    private fun resolveType(): TagType =
        when {
            iso7816Tag != null -> TagType.ISO_DEP
            mifareTag != null -> TagType.NFC_A
            iso15693Tag != null -> TagType.NFC_V
            felicaTag != null -> TagType.NFC_F
            else -> TagType.NFC_A
        }

    private fun resolveTechnologies(): Set<TagTechnology> =
        buildSet {
            if (iso7816Tag != null) {
                add(TagTechnology.ISO_DEP)
                add(TagTechnology.NFC_A)
            }
            if (mifareTag != null) {
                add(TagTechnology.NFC_A)
                when (mifareTag.mifareFamily) {
                    platform.CoreNFC.NFCMiFareUltralight -> add(TagTechnology.MIFARE_ULTRALIGHT)
                    platform.CoreNFC.NFCMiFarePlus,
                    platform.CoreNFC.NFCMiFareDESFire,
                    -> add(TagTechnology.MIFARE_CLASSIC)
                    else -> {}
                }
            }
            if (iso15693Tag != null) add(TagTechnology.NFC_V)
            if (felicaTag != null) add(TagTechnology.NFC_F)
            if (ndefTag != null) add(TagTechnology.NDEF)
        }

    private suspend fun queryNdefStatus(ndef: NFCNDEFTagProtocol): NFCNDEFStatus =
        suspendCoroutine { cont ->
            ndef.queryNDEFStatusWithCompletionHandler { status, _, error ->
                if (error != null) {
                    cont.resumeWithException(
                        NfcException(
                            TransceiveError(
                                message = error.localizedDescription,
                                cause = error.toException(),
                            ),
                        ),
                    )
                } else {
                    cont.resume(status)
                }
            }
        }

    private suspend fun readNdefMessage(ndef: NFCNDEFTagProtocol): NdefMessage? =
        suspendCoroutine { cont ->
            ndef.readNDEFWithCompletionHandler { ndefMessage, error ->
                if (error != null) {
                    cont.resumeWithException(
                        NfcException(
                            com.atruedev.kmpnfc.error.NdefFormatError(
                                message = error.localizedDescription,
                                cause = error.toException(),
                            ),
                        ),
                    )
                } else if (ndefMessage == null) {
                    cont.resume(null)
                } else {
                    cont.resume(ndefMessage.toKmpNdefMessage())
                }
            }
        }

    private suspend fun writeNdefMessage(
        ndef: NFCNDEFTagProtocol,
        message: NdefMessage,
    ) = suspendCoroutine { cont ->
        val nfcNdefMessage = message.toIosNdefMessage()
        ndef.writeNDEF(nfcNdefMessage) { error ->
            if (error != null) {
                cont.resumeWithException(
                    NfcException(
                        com.atruedev.kmpnfc.error.NdefFormatError(
                            message = error.localizedDescription,
                            cause = error.toException(),
                        ),
                    ),
                )
            } else {
                cont.resume(Unit)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun sendApduToIso7816(
        iso7816: NFCISO7816TagProtocol,
        data: ByteArray,
    ): ByteArray =
        suspendCoroutine { cont ->
            val nsData = data.toNSData()
            iso7816.sendCommandAPDU(
                platform.CoreNFC.NFCISO7816APDU(nsData),
            ) { responseData, sw1, sw2, error ->
                if (error != null) {
                    cont.resumeWithException(
                        NfcException(
                            TransceiveError(
                                message = error.localizedDescription,
                                cause = error.toException(),
                            ),
                        ),
                    )
                } else {
                    val responseBytes = responseData?.toByteArray() ?: byteArrayOf()
                    val result = responseBytes + byteArrayOf(sw1.toByte(), sw2.toByte())
                    cont.resume(result)
                }
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun sendDataToMiFare(
        mifareRef: NFCMiFareTagProtocol,
        data: ByteArray,
    ): ByteArray =
        suspendCoroutine { cont ->
            mifareRef.sendMiFareCommand(data.toNSData()) { responseData, error ->
                if (error != null) {
                    cont.resumeWithException(
                        NfcException(
                            TransceiveError(
                                message = error.localizedDescription,
                                cause = error.toException(),
                            ),
                        ),
                    )
                } else {
                    cont.resume(responseData?.toByteArray() ?: byteArrayOf())
                }
            }
        }
}

private fun NSError.toException(): Exception = Exception(localizedDescription)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val length = length.toInt()
    if (length == 0) return byteArrayOf()
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
    return bytes
}

private fun NFCNDEFMessage.toKmpNdefMessage(): NdefMessage {
    val records =
        (this.records as? List<*>)?.mapNotNull { payload ->
            (payload as? NFCNDEFPayload)?.toKmpNdefRecord()
        } ?: emptyList()
    return NdefMessage(records)
}

private fun NFCNDEFPayload.toKmpNdefRecord(): NdefRecord {
    val tnfValue =
        when (typeNameFormat) {
            platform.CoreNFC.NFCTypeNameFormatEmpty -> TypeNameFormat.EMPTY
            platform.CoreNFC.NFCTypeNameFormatNFCWellKnown -> TypeNameFormat.WELL_KNOWN
            platform.CoreNFC.NFCTypeNameFormatMedia -> TypeNameFormat.MIME_MEDIA
            platform.CoreNFC.NFCTypeNameFormatAbsoluteURI -> TypeNameFormat.ABSOLUTE_URI
            platform.CoreNFC.NFCTypeNameFormatNFCExternal -> TypeNameFormat.EXTERNAL_TYPE
            platform.CoreNFC.NFCTypeNameFormatUnchanged -> TypeNameFormat.UNCHANGED
            else -> TypeNameFormat.UNKNOWN
        }
    return parseNdefRecord(tnfValue, type.toByteArray(), payload.toByteArray())
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun NdefMessage.toIosNdefMessage(): NFCNDEFMessage {
    val payloads =
        records.map { record ->
            NFCNDEFPayload(
                format = record.tnf.toIosFormat(),
                type = record.type.toNSData(),
                identifier = NSData(),
                payload = record.payload.toNSData(),
            )
        }
    return NFCNDEFMessage(nDEFRecords = payloads)
}

private fun TypeNameFormat.toIosFormat(): platform.CoreNFC.NFCTypeNameFormat =
    when (this) {
        TypeNameFormat.EMPTY -> platform.CoreNFC.NFCTypeNameFormatEmpty
        TypeNameFormat.WELL_KNOWN -> platform.CoreNFC.NFCTypeNameFormatNFCWellKnown
        TypeNameFormat.MIME_MEDIA -> platform.CoreNFC.NFCTypeNameFormatMedia
        TypeNameFormat.ABSOLUTE_URI -> platform.CoreNFC.NFCTypeNameFormatAbsoluteURI
        TypeNameFormat.EXTERNAL_TYPE -> platform.CoreNFC.NFCTypeNameFormatNFCExternal
        TypeNameFormat.UNKNOWN -> platform.CoreNFC.NFCTypeNameFormatUnknown
        TypeNameFormat.UNCHANGED -> platform.CoreNFC.NFCTypeNameFormatUnchanged
    }
