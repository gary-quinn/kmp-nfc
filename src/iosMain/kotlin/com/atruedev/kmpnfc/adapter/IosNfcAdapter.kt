package com.atruedev.kmpnfc.adapter

import com.atruedev.kmpnfc.error.AdapterDisabled
import com.atruedev.kmpnfc.error.AdapterError
import com.atruedev.kmpnfc.error.NfcError
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.NotSupported
import com.atruedev.kmpnfc.error.SessionInvalidated
import com.atruedev.kmpnfc.error.SessionInvalidationReason
import com.atruedev.kmpnfc.error.Unauthorized
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
import platform.CoreNFC.NFCErrorDomain
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCPollingISO15693
import platform.CoreNFC.NFCPollingISO18092
import platform.CoreNFC.NFCReaderErrorRadioDisabled
import platform.CoreNFC.NFCReaderErrorSecurityViolation
import platform.CoreNFC.NFCReaderErrorUnsupportedFeature
import platform.CoreNFC.NFCReaderSessionInvalidationErrorSessionTimeout
import platform.CoreNFC.NFCReaderSessionInvalidationErrorSystemIsBusy
import platform.CoreNFC.NFCReaderSessionInvalidationErrorUserCanceled
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
                            val protocol = tag as? platform.CoreNFC.NFCTagProtocol ?: continue
                            trySend(IosNfcTag(protocol, session))
                        }
                        if (!options.isMultiTagSession) {
                            session.invalidateSession()
                        }
                    }

                    override fun tagReaderSession(
                        session: NFCTagReaderSession,
                        didInvalidateWithError: NSError,
                    ) {
                        close(NfcException(mapReaderError(didInvalidateWithError)))
                    }

                    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) = Unit
                }

            val session = NFCTagReaderSession(pollingOption, delegate, null)
            options.alertMessage?.let { session.alertMessage = it }
            session.beginSession()

            awaitClose {
                session.invalidateSession()
                // NFCTagReaderSession holds its delegate weakly - prevent GC.
                delegate.description()
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
            canBackgroundRead = true,
            canHostCardEmulation = false,
            supportedTagTypes =
                if (isIos13Plus) {
                    setOf(TagType.NFC_A, TagType.NFC_B, TagType.NFC_F, TagType.NFC_V, TagType.ISO_DEP)
                } else {
                    emptySet()
                },
        )
    }
}

/**
 * Recognizes the Core NFC errors that describe the adapter or the session as a whole rather than
 * the operation in flight, or `null` when the error belongs to the operation.
 *
 * These can arrive through *any* Core NFC callback, not just session invalidation - a missing
 * entitlement, for instance, is reported by `connectToTag` as `NFCErrorDomain` code 2 long after
 * the session opened successfully. Callers should prefer this over their own fallback so that a
 * broken session is not reported as, say, a lost tag.
 *
 * Unsupported hardware, a disabled radio, and a missing entitlement become the same
 * [NotSupported] / [AdapterDisabled] / [Unauthorized] errors that `AndroidNfcAdapter` throws
 * eagerly when collection starts. iOS can only learn about them by attempting a session, so they
 * arrive here instead of being resolvable up front from adapter state.
 */
internal fun adapterErrorOrNull(error: NSError): AdapterError? {
    if (error.domain != NFCErrorDomain) return null
    val cause = Exception(error.localizedDescription)
    return when (error.code) {
        NFCReaderErrorUnsupportedFeature -> NotSupported(cause = cause)
        NFCReaderErrorRadioDisabled -> AdapterDisabled(cause = cause)
        NFCReaderErrorSecurityViolation -> Unauthorized(cause = cause)
        NFCReaderSessionInvalidationErrorUserCanceled ->
            SessionInvalidated(
                message = error.localizedDescription,
                reason = SessionInvalidationReason.USER_CANCELED,
                cause = cause,
            )
        NFCReaderSessionInvalidationErrorSessionTimeout ->
            SessionInvalidated(
                message = error.localizedDescription,
                reason = SessionInvalidationReason.SESSION_TIMEOUT,
                cause = cause,
            )
        NFCReaderSessionInvalidationErrorSystemIsBusy ->
            SessionInvalidated(
                message = error.localizedDescription,
                reason = SessionInvalidationReason.SYSTEM_BUSY,
                cause = cause,
            )
        else -> null
    }
}

/**
 * Maps a Core NFC `NFCTagReaderSessionDelegate.didInvalidateWithError` [NSError] to the
 * [NfcError] it represents, falling back to an unattributed [SessionInvalidated] for codes that
 * [adapterErrorOrNull] does not recognize.
 */
internal fun mapReaderError(error: NSError): NfcError =
    adapterErrorOrNull(error)
        ?: SessionInvalidated(
            message = error.localizedDescription,
            cause = Exception(error.localizedDescription),
        )

public actual fun NfcAdapter(): NfcAdapter = IosNfcAdapter()
