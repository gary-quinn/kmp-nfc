package com.atruedev.kmpnfc.sample

import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.ndef.NdefRecord
import com.atruedev.kmpnfc.ndef.ndefMessage
import com.atruedev.kmpnfc.reader.ReaderOptions

/**
 * Self-contained NFC lifecycle walkthrough.
 *
 * See [nfcQuickstartRead] and [nfcQuickstartWrite].
 */
object NfcQuickstart {
    suspend fun nfcQuickstartRead() {
        val adapter = NfcAdapter()
        try {
            adapter.tags(ReaderOptions(alertMessage = "Hold near tag")).collect { tag ->
                tag.use {
                    val ndef = it.readNdef()
                    ndef?.records?.forEach { record ->
                        when (record) {
                            is NdefRecord.Uri -> println("URL: ${record.uri}")
                            is NdefRecord.Text -> println("Text: ${record.text}")
                            is NdefRecord.MimeMedia -> println("MIME: ${record.mimeType}")
                            else -> println("Other record: $record")
                        }
                    }
                }
            }
        } finally {
            adapter.close()
        }
    }

    suspend fun nfcQuickstartWrite() {
        val adapter = NfcAdapter()
        try {
            adapter.tags(ReaderOptions(alertMessage = "Hold near writable tag")).collect { tag ->
                tag.use {
                    val message =
                        ndefMessage {
                            uri("https://github.com/gary-quinn/kmp-nfc")
                            text("Hello NFC", locale = "en")
                        }
                    it.writeNdef(message)
                }
            }
        } finally {
            adapter.close()
        }
    }
}
