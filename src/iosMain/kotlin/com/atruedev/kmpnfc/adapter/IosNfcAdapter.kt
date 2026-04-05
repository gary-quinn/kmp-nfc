package com.atruedev.kmpnfc.adapter

import com.atruedev.kmpnfc.reader.IosNfcTag
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCPollingISO15693
import platform.CoreNFC.NFCPollingISO18092
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
import platform.Foundation.NSClassFromString
import platform.Foundation.NSError
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.darwin.NSObject

internal class IosNfcAdapter : NfcAdapter {
    private val _state = MutableStateFlow(resolveAdapterState())

    override val state: StateFlow<NfcAdapterState> = _state.asStateFlow()

    override val capabilities: NfcCapabilities = resolveCapabilities()

    @OptIn(ExperimentalForeignApi::class)
    override fun tags(options: ReaderOptions): Flow<NfcTag> =
        callbackFlow {
            var pollingOption: Long = 0
            if (options.pollingTypes.any { it == TagType.NFC_A || it == TagType.NFC_B || it == TagType.ISO_DEP }) {
                pollingOption = pollingOption or NFCPollingISO14443
            }
            if (options.pollingTypes.any { it == TagType.NFC_V }) {
                pollingOption = pollingOption or NFCPollingISO15693
            }
            if (options.pollingTypes.any { it == TagType.NFC_F }) {
                pollingOption = pollingOption or NFCPollingISO18092
            }

            if (pollingOption == 0L) {
                pollingOption = NFCPollingISO14443 or NFCPollingISO15693 or NFCPollingISO18092
            }

            val delegate =
                object : NSObject(), NFCTagReaderSessionDelegateProtocol {
                    override fun tagReaderSession(
                        session: NFCTagReaderSession,
                        didDetectTags: List<*>,
                    ) {
                        for (tag in didDetectTags) {
                            if (tag != null) {
                                trySend(IosNfcTag(tag, session))
                            }
                        }
                        if (!options.isMultiTagSession) {
                            session.invalidateSession()
                        }
                    }

                    override fun tagReaderSession(
                        session: NFCTagReaderSession,
                        didInvalidateWithError: NSError,
                    ) {
                        close()
                    }

                    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) = Unit
                }

            val session = NFCTagReaderSession(pollingOption, delegate, null)
            options.alertMessage?.let { session.alertMessage = it }
            session.beginSession()

            awaitClose {
                session.invalidateSession()
            }
        }

    override fun close() = Unit

    private fun resolveAdapterState(): NfcAdapterState {
        if (NSClassFromString("NFCNDEFReaderSession") == null) {
            return NfcAdapterState.NOT_SUPPORTED
        }
        return if (NFCNDEFReaderSession.readingAvailable) {
            NfcAdapterState.ON
        } else {
            NfcAdapterState.NOT_SUPPORTED
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun resolveCapabilities(): NfcCapabilities {
        if (_state.value == NfcAdapterState.NOT_SUPPORTED) return NfcCapabilities.NONE

        val isIos13Plus =
            NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
                cValue<NSOperatingSystemVersion> {
                    majorVersion = 13
                    minorVersion = 0
                    patchVersion = 0
                },
            )

        return NfcCapabilities(
            canReadNdef = true,
            canWriteNdef = isIos13Plus,
            canReadRawTag = isIos13Plus,
            canHostCardEmulation = false,
            canBackgroundRead = true,
            supportedTagTypes =
                if (isIos13Plus) {
                    setOf(TagType.NFC_A, TagType.NFC_B, TagType.NFC_F, TagType.NFC_V, TagType.ISO_DEP)
                } else {
                    emptySet()
                },
        )
    }
}

public actual fun NfcAdapter(): NfcAdapter = IosNfcAdapter()
