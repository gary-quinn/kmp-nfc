package com.atruedev.kmpnfc.error

/**
 * Root of the composable NFC error hierarchy.
 *
 * Uses sealed interfaces to enable composable error handling —
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

/** Errors related to Host Card Emulation. */
public sealed interface HceError : NfcError

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

/** User denied NFC permission. */
public data class Unauthorized(
    override val message: String = "NFC permission not granted",
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

/** Tag storage full — not enough space for NDEF message. */
public data class InsufficientSpace(
    override val message: String = "Tag does not have enough storage space",
    override val cause: Throwable? = null,
) : TagOperationError

/** HCE not available on this platform/region/configuration. */
public data class HceNotAvailable(
    val reason: String,
    override val message: String = "Host Card Emulation not available: $reason",
    override val cause: Throwable? = null,
) : HceError

/** iOS reader session invalidated by system (timeout, user dismissal, or system event). */
public data class SessionInvalidated(
    override val message: String,
    override val cause: Throwable? = null,
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
 */
public data class NfcException(
    public val error: NfcError,
    val errorMessage: String = error.toString(),
) : Exception(errorMessage)
