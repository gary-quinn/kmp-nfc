# kmp-nfc Architecture

Internal design decisions and structure of kmp-nfc. Intended for contributors and anyone curious about how the library works.

---

## Overview

kmp-nfc is a Kotlin Multiplatform NFC library targeting Android and iOS. The core design principle is: **shared logic in `commonMain`, platform bridges in `expect/actual`, no platform details leaking into the public API.**

```
commonMain/
├── adapter/    NfcAdapter, NfcAdapterState, NfcCapabilities
├── reader/     NfcTag, ReaderOptions
├── ndef/       NdefMessage, NdefRecord, NdefRecordParser, encoding
├── tag/        TagType, TagTechnology, ApduCommand, ApduResponse
└── error/      NfcError sealed hierarchy, NfcException

androidMain/    AndroidNfcAdapter, AndroidNfcTag, ActivityTracker, KmpNfc
iosMain/        IosNfcAdapter, IosNfcTag
jvmMain/        JvmNfcAdapter (stub)
```

---

## Concurrency Model

### Per-Tag Serial Execution

NFC tag operations must be serialized — sending a second command while the first is in-flight corrupts the protocol. Each `IosNfcTag` owns a `limitedParallelism(1)` dispatcher:

```kotlin
internal class IosNfcTag(
    private val tag: NFCTagProtocol,
    private val session: NFCTagReaderSession,
    private val tagDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
)
```

Every public operation (`readNdef`, `writeNdef`, `transceive`) dispatches through `withContext(tagDispatcher)`, guaranteeing at most one operation runs at a time per tag. No locks, no atomics, no mutex.

On Android, tag operations dispatch to `Dispatchers.IO` because the Android NFC SDK uses blocking I/O internally. Each operation opens a tech connection, performs the operation, and closes it — serialization is inherent in the connect/close lifecycle.

### Tag Discovery Flow

`NfcAdapter.tags()` returns a cold `Flow<NfcTag>`:

```
Collect starts  →  Platform session begins  →  Tags emitted  →  Cancel collection  →  Session ends
```

- **Android:** `callbackFlow` wrapping `enableReaderMode`/`disableReaderMode`
- **iOS:** `callbackFlow` wrapping `NFCTagReaderSession.beginSession`/`invalidateSession`

---

## iOS Session Lifecycle

Core NFC requires a modal `NFCTagReaderSession` — the system displays an NFC reading sheet. Key constraints:

1. Only one session at a time
2. Session auto-invalidates after ~60 seconds or user dismissal
3. `connectToTag` must be called before any tag operation
4. A connected tag cannot be reconnected

`IosNfcTag.ensureConnected()` handles lazy connection — connects on first operation, no-ops on subsequent calls. The `connected` flag is safe because all access is serialized through `tagDispatcher`.

When the session invalidates (timeout, user dismissal, system event), the delegate calls `close(NfcException(SessionInvalidated(...)))` so flow collectors receive the reason.

---

## NDEF Codec

NDEF encoding/decoding is shared in `commonMain`. Both platform implementations map their native NDEF types to `TypeNameFormat` + raw bytes, then delegate to a single function:

```kotlin
// commonMain — single source of truth for NDEF parsing
internal fun parseNdefRecord(
    tnf: TypeNameFormat,
    type: ByteArray,
    payload: ByteArray,
): NdefRecord
```

This eliminates the duplication that would otherwise exist between Android's `NdefRecord` → kmp `NdefRecord` and iOS's `NFCNDEFPayload` → kmp `NdefRecord` conversion paths.

URI records use the NFC Forum URI RTD prefix table (36 entries) for compact encoding. Text records encode locale and UTF-8/UTF-16 flag in a status byte.

---

## Error Model

Composable sealed interfaces allow pattern-matching at the granularity the caller needs:

```
NfcError
├── AdapterError
│   ├── NotSupported
│   ├── AdapterDisabled
│   ├── Unauthorized
│   └── SessionInvalidated
└── TagOperationError
    ├── TagLost
    ├── UnsupportedOperation
    ├── NdefFormatError
    ├── TransceiveError
    ├── ReadOnly
    ├── InsufficientSpace
    └── Timeout
```

All errors carry `message` and `cause`. `NfcException` wraps `NfcError` as a throwable, chaining `error.cause` into `Exception(message, cause)` so platform stack traces are preserved.

`NfcException` is deliberately not a data class — exceptions use identity equality, not structural equality.

---

## Platform Bridge Pattern

The `expect fun NfcAdapter(): NfcAdapter` factory creates platform-specific implementations:

| Platform | Implementation | Notes |
|----------|---------------|-------|
| Android | `AndroidNfcAdapter` | Reader mode, broadcast receiver for state, AndroidX Startup |
| iOS | `IosNfcAdapter` | `NFCTagReaderSession`, Core NFC delegate |
| JVM | `JvmNfcAdapter` | Returns `NOT_SUPPORTED`, `emptyFlow()` |

Android uses `ActivityTracker` (a singleton `ActivityLifecycleCallbacks` registered once at init) to resolve the current foreground activity for reader mode. This avoids the anti-pattern of registering a new callback per `tags()` call.

---

## Testing Infrastructure

`FakeNfcAdapter` and `FakeNfcTag` mirror the patterns from kmp-ble's `FakeScanner`/`FakePeripheral`:

- **State simulation:** `simulateEnabled()`, `simulateDisabled()`, `simulateNotSupported()`
- **Tag emission:** `emitTag(tag)` pushes to a `MutableSharedFlow`
- **Error injection:** `failWith(TagLost())` makes all operations throw
- **Delay simulation:** `respondAfter(100.milliseconds)` adds latency
- **Write tracking:** `writtenNdefMessages` records all NDEF writes for assertion

The builder DSL keeps test setup concise:

```kotlin
val tag = fakeNfcTag {
    identifier(byteArrayOf(0x04, 0x12, 0x34, 0x56))
    type(TagType.ISO_DEP)
    ndef(ndefMessage { uri("https://example.com") })
    onTransceive { command -> byteArrayOf(0x90.toByte(), 0x00) }
}
```

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| `NfcCapabilities` over lowest-common-denominator | NFC platform asymmetry is too severe to hide. iOS lacks MIFARE Classic, Android lacks background read. Developers must query capabilities. |
| Cold `Flow<NfcTag>` over callback | Collecting starts the session, cancelling ends it. Structured concurrency manages the lifecycle. |
| `limitedParallelism(1)` over AtomicInt | Project-wide policy from kmp-ble/kmp-uwb. Coroutine serialization is more composable than atomics. |
| No HCE in v0.1 | Interface-only APIs with no implementation are vaporware. HCE ships when both Android and iOS implementations exist. |
| `NFCTagProtocol` typed constructor | The delegate callback delivers `List<*>` — casting happens at the call site where context is clear, not deferred into the tag class. |
| Shared `parseNdefRecord()` | DRY: one function in commonMain, both platforms extract (tnf, type, payload) and delegate. |
| `NfcException` as regular class | Exceptions use identity equality. `data class` generates `equals`/`hashCode`/`copy` that are semantically wrong for exceptions. |
| No `ReaderOptions.timeout` | Neither platform enforced it. False promise removed. Callers use `withTimeout {}` at the collection site. |
