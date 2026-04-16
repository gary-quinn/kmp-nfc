package com.atruedev.kmpnfc.testing

import com.atruedev.kmpnfc.error.NfcError
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.TagLost
import com.atruedev.kmpnfc.ndef.NdefMessage
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.tag.TagTechnology
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * Test double for [NfcTag] with configurable NDEF content, transceive responses,
 * error injection, and response delays.
 *
 * ```
 * val tag = FakeNfcTag {
 *     identifier(byteArrayOf(0x04, 0x12, 0x34, 0x56))
 *     type(TagType.NFC_A)
 *     ndef(ndefMessage { uri("https://example.com") })
 *     onTransceive { command -> byteArrayOf(0x90.toByte(), 0x00) }
 *     respondAfter(100.milliseconds)
 * }
 * ```
 */
public class FakeNfcTag(
    override val identifier: ByteArray = byteArrayOf(0x04, 0x00, 0x00, 0x00),
    override val type: TagType = TagType.NFC_A,
    override val technologies: Set<TagTechnology> = setOf(TagTechnology.NFC_A, TagTechnology.NDEF),
    private val ndefMessage: NdefMessage? = null,
    private val transceiveHandler: (suspend (ByteArray) -> ByteArray)? = null,
    private val error: NfcError? = null,
    private val responseDelay: Duration = Duration.ZERO,
) : NfcTag {
    private val writtenMessages = mutableListOf<NdefMessage>()
    private var closed = false

    /** Whether [close] has been called. */
    public val isClosed: Boolean get() = closed

    /** Messages written to this tag via [writeNdef]. */
    public val writtenNdefMessages: List<NdefMessage> get() = writtenMessages.toList()

    override suspend fun readNdef(): NdefMessage? {
        ensureOpen()
        maybeDelay()
        maybeThrow()
        return ndefMessage
    }

    override suspend fun writeNdef(message: NdefMessage) {
        ensureOpen()
        maybeDelay()
        maybeThrow()
        writtenMessages.add(message)
    }

    override suspend fun transceive(data: ByteArray): ByteArray {
        ensureOpen()
        maybeDelay()
        maybeThrow()
        return transceiveHandler?.invoke(data)
            ?: throw NfcException(
                com.atruedev.kmpnfc.error
                    .UnsupportedOperation("transceive — no handler configured"),
            )
    }

    override fun close() {
        closed = true
    }

    private fun ensureOpen() {
        if (closed) throw NfcException(TagLost("Tag has been closed"))
    }

    private suspend fun maybeDelay() {
        if (responseDelay > Duration.ZERO) delay(responseDelay)
    }

    private fun maybeThrow() {
        error?.let { throw NfcException(it) }
    }
}

/**
 * Builder DSL for constructing [FakeNfcTag] instances.
 *
 * ```
 * val tag = fakeNfcTag {
 *     identifier(byteArrayOf(0x04, 0x12, 0x34, 0x56))
 *     type(TagType.ISO_DEP)
 *     technologies(setOf(TagTechnology.NFC_A, TagTechnology.ISO_DEP))
 *     ndef(ndefMessage { uri("https://example.com") })
 *     onTransceive { command -> byteArrayOf(0x90.toByte(), 0x00) }
 *     respondAfter(100.milliseconds)
 *     failWith(TagLost())
 * }
 * ```
 */
public class FakeNfcTagBuilder {
    private var identifier: ByteArray = byteArrayOf(0x04, 0x00, 0x00, 0x00)
    private var type: TagType = TagType.NFC_A
    private var technologies: Set<TagTechnology> = setOf(TagTechnology.NFC_A, TagTechnology.NDEF)
    private var ndefMessage: NdefMessage? = null
    private var transceiveHandler: (suspend (ByteArray) -> ByteArray)? = null
    private var error: NfcError? = null
    private var responseDelay: Duration = Duration.ZERO

    public fun identifier(id: ByteArray) {
        identifier = id
    }

    public fun type(tagType: TagType) {
        type = tagType
    }

    public fun technologies(techs: Set<TagTechnology>) {
        technologies = techs
    }

    public fun ndef(message: NdefMessage) {
        ndefMessage = message
    }

    public fun onTransceive(handler: suspend (ByteArray) -> ByteArray) {
        transceiveHandler = handler
    }

    /** Inject an error — all operations will throw [NfcException] with this error. */
    public fun failWith(nfcError: NfcError) {
        error = nfcError
    }

    /** Add a delay before each operation response. */
    public fun respondAfter(delay: Duration) {
        responseDelay = delay
    }

    internal fun build(): FakeNfcTag =
        FakeNfcTag(
            identifier = identifier,
            type = type,
            technologies = technologies,
            ndefMessage = ndefMessage,
            transceiveHandler = transceiveHandler,
            error = error,
            responseDelay = responseDelay,
        )
}

/** Build a [FakeNfcTag] using the builder DSL. */
public fun fakeNfcTag(block: FakeNfcTagBuilder.() -> Unit): FakeNfcTag = FakeNfcTagBuilder().apply(block).build()
