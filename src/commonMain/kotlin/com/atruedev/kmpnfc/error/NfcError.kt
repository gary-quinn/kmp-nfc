package com.atruedev.kmpnfc.error

/**
 * Root of the composable NFC error hierarchy.
 *
 * Uses sealed interfaces to enable composable error handling -
 * an error can implement multiple interfaces, allowing callers
 * to pattern-match at the granularity they need.
 *
 * All errors carry a human-readable [message] and an optional
 * [cause] for chaining platform exceptions.
 */
public sealed interface NfcError {
    public val message: String
    public val cause: Throwable?
}

/** Errors related to the NFC adapter or session lifecycle. */
public sealed interface AdapterError : NfcError

/** Errors occurring during tag read/write operations. */
public sealed interface TagOperationError : NfcError

/** NFC hardware not present on this device. */
public data class NotSupported(
    override val message: String = "NFC is not supported on this device",
    override val cause: Throwable? = null,
) : AdapterError

/** NFC is turned off in system settings. */
public data class AdapterDisabled(
    override val message: String = "NFC is disabled in system settings",
    override val cause: Throwable? = null,
) : AdapterError

/**
 * NFC access denied.
 *
 * On iOS this is thrown from [com.atruedev.kmpnfc.adapter.NfcAdapter.tags] when the app is
 * missing the required entitlement or privacy permission (`NFCReaderErrorSecurityViolation`).
 * Core NFC only reports it once a session is attempted, so it cannot be predicted up front as an
 * [com.atruedev.kmpnfc.adapter.NfcAdapterState].
 *
 * On Android it is thrown only from HCE AID registration; ordinary NFC reading has no revocable
 * runtime permission to deny.
 */
public data class Unauthorized(
    override val message: String = "NFC permission not granted",
    override val cause: Throwable? = null,
) : AdapterError

/** NFC reader mode requires a foreground Activity. Only thrown on Android. */
public data class NoForegroundActivity(
    override val message: String = "NFC reader mode requires a resumed Activity in the foreground",
    override val cause: Throwable? = null,
) : AdapterError

/** Tag was lost during operation (moved away from reader). */
public data class TagLost(
    override val message: String = "Tag connection lost",
    override val cause: Throwable? = null,
) : TagOperationError

/** Tag does not support the requested operation. */
public data class UnsupportedOperation(
    val operation: String,
    override val message: String = "Tag does not support operation: $operation",
    override val cause: Throwable? = null,
) : TagOperationError

/** NDEF format error during read or write. */
public data class NdefFormatError(
    override val message: String,
    override val cause: Throwable? = null,
) : TagOperationError

/** Raw transceive operation failed. */
public data class TransceiveError(
    override val message: String,
    override val cause: Throwable? = null,
) : TagOperationError

/** Tag is read-only, cannot write. */
public data class ReadOnly(
    override val message: String = "Tag is read-only",
    override val cause: Throwable? = null,
) : TagOperationError

/** Tag storage full - not enough space for NDEF message. */
public data class InsufficientSpace(
    override val message: String = "Tag does not have enough storage space",
    override val cause: Throwable? = null,
) : TagOperationError

/** Why an iOS reader session was invalidated. */
public enum class SessionInvalidationReason {
    /** The person using the app dismissed the system NFC sheet - not a failure. */
    USER_CANCELED,

    /** Core NFC ended the session on its own after roughly 60 seconds without a read. */
    SESSION_TIMEOUT,

    /** Core NFC was temporarily unavailable due to system resource constraints. */
    SYSTEM_BUSY,

    /** Any other invalidation, including errors outside `NFCErrorDomain`. */
    UNKNOWN,
}

/** iOS reader session invalidated by system (timeout, user dismissal, or system event). */
public data class SessionInvalidated(
    override val message: String,
    override val cause: Throwable? = null,
    val reason: SessionInvalidationReason = SessionInvalidationReason.UNKNOWN,
) : AdapterError

/** Operation timed out. */
public data class Timeout(
    override val message: String = "NFC operation timed out",
    override val cause: Throwable? = null,
) : TagOperationError

/**
 * Exception wrapper for [NfcError] values, allowing them to be thrown as exceptions.
 * Used by [com.atruedev.kmpnfc.testing.FakeNfcTag] error injection
 * and catchable in test assertions.
 *
 * Not a data class - exceptions use identity equality, not structural equality.
 */
public class NfcException(
    public val error: NfcError,
) : Exception(error.message, error.cause) {
    override fun toString(): String = "NfcException($error)"
}
