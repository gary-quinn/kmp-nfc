# HCE Architecture

Android Host Card Emulation for `kmp-nfc`. iOS and JVM ship `NOT_SUPPORTED`
stubs until platform support exists.

## Scope

`HceService` lets an Android app answer ISO 7816-4 APDUs from an external NFC
reader. This is the inverse of `NfcAdapter.tags()` (reader mode).

```
External reader --APDU--> KmpNfcHostApduService --dispatch--> AndroidHceService
                                                              scope.launch { processor(cmd) }
                                                              sendResponseApdu(bytes)
```

## API

```kotlin
val hce = HceService()
if (!hce.capabilities.isSupported) return

try {
    hce.start(HceConfig(aids = listOf(AidRegistration("F0010203040506")))) { command ->
        when {
            command.isSelectAid() -> ApduResponse.success()
            else -> ApduResponse.instructionNotSupported()
        }
    }
} catch (e: DeactivationException) {
    // Reader disconnected (LINK_LOSS or DESELECTED)
} catch (e: Exception) {
    // Processor threw - exception propagates here
}

hce.stop() // [start] returns normally, no DeactivationException
```

## Threading

`HostApduService.processCommandApdu()` runs on the **UI thread**. The library:

1. Returns `null` immediately (never blocks the UI thread).
2. Dispatches work on `Dispatchers.IO.limitedParallelism(1)` - one APDU at a
   time, same policy as `AndroidNfcTag`.
3. Calls `sendResponseApdu()` from that serial dispatcher when the processor
   completes.

Invalid APDU bytes or commands received before [HceService.start] suspend get a
synchronous `6F00` response from `processCommandApdu()`.

## Registry

`HceServiceRegistry` links the system-created `KmpNfcHostApduService` to the
consumer's `AndroidHceService` singleton. `hostBridge` is bound for the
`HostApduService` lifetime; `session` is cleared on session cleanup only.

## Capabilities

| Field | Android meaning |
|-------|-----------------|
| `isSupported` | `FEATURE_NFC_HOST_CARD_EMULATION` + `CardEmulation` available |
| `canPaymentCategory` | This app is the default Tap & Pay wallet (live read) |

`NfcCapabilities.canHostCardEmulation` uses the same feature flag (snapshot at
`NfcAdapter` construction). `hce.capabilities` is recomputed on each read.

## Errors

**Before `start()` suspends** - `NfcException` with `NotSupported`,
`AdapterDisabled`, or `Unauthorized` (see prior error table in CHANGELOG).

**During `start()`** - `DeactivationException` when the reader disconnects.
`stop()` cancels the session without throwing.

**Processor failures** - any non-cancellation exception from the processor
cancels `start()` and rethrows the original exception to the `start()` caller.

## Manifest

The library manifest merges `KmpNfcHostApduService` with an empty static AID
group. AIDs are registered at runtime via `CardEmulation.registerAidsForService()`.

## Testing

- `HceServiceTest` - session lifecycle against `FakeHceService`
- `HceConfigTest` - AID validation
- `HceServiceRegistryTest` - registry and host-bridge lifetime
- `HceInboundApduTest` - malformed/no-session APDU returns `6F00`

Integration testing for the full `HostApduService` stack requires a device or
emulator.

## Platform status

| Platform | Status |
|----------|--------|
| Android | Full implementation |
| iOS | `NOT_SUPPORTED` (EEA entitlement gate) |
| JVM | `NOT_SUPPORTED` |
