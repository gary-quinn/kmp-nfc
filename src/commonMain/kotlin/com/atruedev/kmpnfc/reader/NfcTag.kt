package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType

/**
 * Represents a discovered NFC tag with typed accessors.
 *
 * Platform implementations hold native tag references — this interface
 * provides a platform-agnostic surface. Always call [close] when done
 * to release the underlying connection.
 *
 * Operations throw [com.atruedev.kmpnfc.error.NfcException] on failure.
 */
public interface NfcTag : AutoCloseable {
    /** Tag unique identifier (UID). May be random on phone-emulated tags. */
    public val identifier: ByteArray

    /** Primary tag type. */
    public val type: TagType

    /** All technologies supported by this tag. */
    public val technologies: Set<TagTechnology>

    /**
     * Read the NDEF message stored on this tag.
     *
     * @return The NDEF message, or null if the tag does not contain one.
     * @throws com.atruedev.kmpnfc.error.NfcException on read failure.
     */
    public suspend fun readNdef(): NdefMessage?

    /**
     * Write an NDEF message to this tag.
     *
     * @throws com.atruedev.kmpnfc.error.NfcException if the tag is read-only,
     *   has insufficient space, or the write fails.
     */
    public suspend fun writeNdef(message: NdefMessage)

    /**
     * Send raw bytes to the tag and receive the response.
     *
     * Used for ISO 7816-4 APDU exchange (e.g., smart cards, Aliro NFC credentials).
     *
     * @throws com.atruedev.kmpnfc.error.NfcException on transceive failure or if
     *   the tag does not support raw communication.
     */
    public suspend fun transceive(data: ByteArray): ByteArray

    /** Close the connection to the tag and release platform resources. */
    override fun close()
}
