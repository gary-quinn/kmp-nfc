package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType

/**
 * A discovered NFC tag.
 *
 * Platform implementations hold native tag references. Always call [close]
 * when done to release the underlying connection.
 *
 * All suspend operations signal failure via [com.atruedev.kmpnfc.error.NfcException]
 * wrapping a typed [com.atruedev.kmpnfc.error.NfcError].
 */
public interface NfcTag : AutoCloseable {
    /** Tag unique identifier (UID). May be random on phone-emulated tags. */
    public val identifier: ByteArray

    /** Primary tag type. */
    public val type: TagType

    /** All technologies supported by this tag. */
    public val technologies: Set<TagTechnology>

    /** Read the NDEF message stored on this tag, or null if none. */
    public suspend fun readNdef(): NdefMessage?

    /** Write an NDEF message to this tag. */
    public suspend fun writeNdef(message: NdefMessage)

    /**
     * Send raw bytes and receive the response.
     *
     * On iOS ISO 7816 tags, the response includes trailing SW1/SW2 status bytes.
     */
    public suspend fun transceive(data: ByteArray): ByteArray

    /**
     * Mark this tag as closed. Subsequent operations throw [com.atruedev.kmpnfc.error.TagLost].
     *
     * On Android the native tech connection is released asynchronously on the tag dispatcher.
     * On iOS, CoreNFC releases connections when the NFCTagReaderSession invalidates.
     *
     * Idempotent - safe to call multiple times.
     */
    override fun close()
}
