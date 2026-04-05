package com.atruedev.kmpnfc.ndef

/**
 * An NFC Data Exchange Format (NDEF) message containing one or more records.
 *
 * Construct via [ndefMessage] builder DSL:
 * ```
 * val message = ndefMessage {
 *     uri("https://github.com/gary-quinn/kmp-nfc")
 *     text("Hello NFC", locale = "en")
 * }
 * ```
 */
public data class NdefMessage(
    val records: List<NdefRecord>,
)

/**
 * Builder DSL for constructing [NdefMessage] instances.
 *
 * ```
 * val message = ndefMessage {
 *     uri("https://example.com")
 *     text("Hello NFC")
 *     mimeMedia("application/json", """{"key": "value"}""".encodeToByteArray())
 *     externalType("com.example", "mytype", byteArrayOf(0x01, 0x02))
 * }
 * ```
 */
public class NdefMessageBuilder {
    private val records = mutableListOf<NdefRecord>()

    /** Add a URI record. */
    public fun uri(uri: String) {
        records.add(NdefRecord.Uri(uri))
    }

    /** Add a Text record with optional locale (defaults to "en"). */
    public fun text(
        text: String,
        locale: String = "en",
        encoding: NdefTextEncoding = NdefTextEncoding.UTF_8,
    ) {
        records.add(NdefRecord.Text(text, locale, encoding))
    }

    /** Add a MIME media record. */
    public fun mimeMedia(
        mimeType: String,
        data: ByteArray,
    ) {
        records.add(NdefRecord.MimeMedia(mimeType, data))
    }

    /** Add an NFC Forum external type record. */
    public fun externalType(
        domain: String,
        type: String,
        data: ByteArray,
    ) {
        records.add(NdefRecord.ExternalType(domain, type, data))
    }

    /** Add an arbitrary record. */
    public fun record(record: NdefRecord) {
        records.add(record)
    }

    internal fun build(): NdefMessage = NdefMessage(records.toList())
}

/** Build an [NdefMessage] using the builder DSL. */
public fun ndefMessage(block: NdefMessageBuilder.() -> Unit): NdefMessage = NdefMessageBuilder().apply(block).build()
