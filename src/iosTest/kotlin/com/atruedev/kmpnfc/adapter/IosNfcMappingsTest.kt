package com.atruedev.kmpnfc.adapter

import com.atruedev.kmpnfc.error.AdapterDisabled
import com.atruedev.kmpnfc.error.NotSupported
import com.atruedev.kmpnfc.error.SessionInvalidated
import com.atruedev.kmpnfc.error.SessionInvalidationReason
import com.atruedev.kmpnfc.error.Unauthorized
import platform.CoreNFC.NFCErrorDomain
import platform.CoreNFC.NFCReaderErrorRadioDisabled
import platform.CoreNFC.NFCReaderErrorSecurityViolation
import platform.CoreNFC.NFCReaderErrorUnsupportedFeature
import platform.CoreNFC.NFCReaderSessionInvalidationErrorSessionTimeout
import platform.CoreNFC.NFCReaderSessionInvalidationErrorSystemIsBusy
import platform.CoreNFC.NFCReaderSessionInvalidationErrorUserCanceled
import platform.CoreNFC.NFCReaderTransceiveErrorRetryExceeded
import platform.CoreNFC.NFCReaderTransceiveErrorTagConnectionLost
import platform.Foundation.NSError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class IosNfcMappingsTest {
    @Test
    fun mapsStartupFailuresToTheSameErrorsAndroidThrowsEagerly() {
        assertIs<NotSupported>(mapReaderError(nfcError(NFCReaderErrorUnsupportedFeature)))
        assertIs<AdapterDisabled>(mapReaderError(nfcError(NFCReaderErrorRadioDisabled)))
        assertIs<Unauthorized>(mapReaderError(nfcError(NFCReaderErrorSecurityViolation)))
    }

    @Test
    fun mapsUserCancelToItsOwnReason() {
        val error = mapReaderError(nfcError(NFCReaderSessionInvalidationErrorUserCanceled))
        assertEquals(
            SessionInvalidationReason.USER_CANCELED,
            assertIs<SessionInvalidated>(error).reason,
        )
    }

    @Test
    fun mapsRemainingSessionInvalidationReasons() {
        val timeout = mapReaderError(nfcError(NFCReaderSessionInvalidationErrorSessionTimeout))
        assertEquals(
            SessionInvalidationReason.SESSION_TIMEOUT,
            assertIs<SessionInvalidated>(timeout).reason,
        )

        val busy = mapReaderError(nfcError(NFCReaderSessionInvalidationErrorSystemIsBusy))
        assertEquals(
            SessionInvalidationReason.SYSTEM_BUSY,
            assertIs<SessionInvalidated>(busy).reason,
        )
    }

    @Test
    fun unrecognizedNfcCodeFallsBackToUnknownReason() {
        val error = mapReaderError(nfcError(code = 9999))
        assertEquals(SessionInvalidationReason.UNKNOWN, assertIs<SessionInvalidated>(error).reason)
    }

    @Test
    fun errorsOutsideNfcDomainAreNotMistakenForNfcCodes() {
        // Code 200 is NFCReaderSessionInvalidationErrorUserCanceled, but only within NFCErrorDomain.
        val error =
            mapReaderError(
                NSError.errorWithDomain(
                    domain = "com.example.SomeOtherDomain",
                    code = NFCReaderSessionInvalidationErrorUserCanceled,
                    userInfo = null,
                ),
            )
        assertEquals(SessionInvalidationReason.UNKNOWN, assertIs<SessionInvalidated>(error).reason)
    }

    @Test
    fun adapterErrorOrNullRecognizesSessionWideFailures() {
        // These reach per-operation callbacks too, so tag operations must prefer them over their
        // own fallback - a signed build missing the NFC entitlement reports code 2 from
        // connectToTag, which must not be reported as a lost tag.
        assertIs<Unauthorized>(adapterErrorOrNull(nfcError(NFCReaderErrorSecurityViolation)))
        assertIs<NotSupported>(adapterErrorOrNull(nfcError(NFCReaderErrorUnsupportedFeature)))
        assertIs<AdapterDisabled>(adapterErrorOrNull(nfcError(NFCReaderErrorRadioDisabled)))
        assertIs<SessionInvalidated>(
            adapterErrorOrNull(nfcError(NFCReaderSessionInvalidationErrorUserCanceled)),
        )
    }

    @Test
    fun adapterErrorOrNullDefersToTheCallerForOperationFailures() {
        // Tag-level trouble: the caller's own TagLost/TransceiveError/NdefFormatError wins.
        assertNull(adapterErrorOrNull(nfcError(NFCReaderTransceiveErrorTagConnectionLost)))
        assertNull(adapterErrorOrNull(nfcError(NFCReaderTransceiveErrorRetryExceeded)))
        assertNull(adapterErrorOrNull(nfcError(code = 9999)))
        assertNull(
            adapterErrorOrNull(
                NSError.errorWithDomain(
                    domain = "com.example.SomeOtherDomain",
                    code = NFCReaderErrorSecurityViolation,
                    userInfo = null,
                ),
            ),
        )
    }

    @Test
    fun preservesTheLocalizedDescriptionAsMessage() {
        val error = mapReaderError(nfcError(NFCReaderSessionInvalidationErrorUserCanceled))
        assertEquals(
            nfcError(NFCReaderSessionInvalidationErrorUserCanceled).localizedDescription,
            error.message,
        )
    }

    private fun nfcError(code: Long): NSError =
        NSError.errorWithDomain(domain = NFCErrorDomain, code = code, userInfo = null)
}
