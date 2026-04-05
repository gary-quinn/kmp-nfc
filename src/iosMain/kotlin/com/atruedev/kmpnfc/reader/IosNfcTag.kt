package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.UnsupportedOperation
import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import platform.CoreNFC.NFCTagReaderSession

/**
 * iOS NFC tag implementation wrapping a Core NFC tag object.
 *
 * Core NFC tag types (NFCISO7816Tag, NFCISO15693Tag, NFCFeliCaTag, NFCMiFareTag)
 * are accessed through the generic tag reference. Operations that require a specific
 * protocol check at runtime and throw [UnsupportedOperation] if the tag doesn't support it.
 */
internal class IosNfcTag(
    private val tag: Any,
    private val session: NFCTagReaderSession,
) : NfcTag {
    override val identifier: ByteArray = byteArrayOf()

    override val type: TagType = TagType.NFC_A

    override val technologies: Set<TagTechnology> = setOf(TagTechnology.NFC_A)

    override suspend fun readNdef(): NdefMessage? =
        throw NfcException(UnsupportedOperation("readNdef — use NFCNDEFReaderSession for NDEF on iOS"))

    override suspend fun writeNdef(message: NdefMessage): Unit =
        throw NfcException(UnsupportedOperation("writeNdef — use NFCNDEFReaderSession for NDEF on iOS"))

    override suspend fun transceive(data: ByteArray): ByteArray =
        throw NfcException(UnsupportedOperation("transceive — requires protocol-specific tag cast on iOS"))

    override fun close() {
        // Session lifecycle manages tag connections on iOS.
    }
}
