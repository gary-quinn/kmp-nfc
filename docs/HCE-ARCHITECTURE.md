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

Invalid APDU bytes get a synchronous `6F00` response from `processCommandApdu()`.

## Registry

`HceServiceRegistry` links the system-created `KmpNfcHostApduService` to the
consumer's `AndroidHceService` singleton. `hostService` lifetime follows the
bound `HostApduService`; `hceService` is cleared on session cleanup only.

## Capabilities

| Field | Android meaning |
|-------|-----------------|
| `isSupported` | `FEATURE_NFC_HOST_CARD_EMULATION` + `CardEmulation` available |
| `canPaymentCategory` | This app is the default Tap & Pay wallet |

`NfcCapabilities.canHostCardEmulation` uses the same feature flag. Query
`hce.capabilities.canPaymentCategory` before registering `AidCategory.PAYMENT`
AIDs - `start()` throws `NfcException(Unauthorized)` otherwise.

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

- `FakeHceService` in `kmp-nfc-testing` - serial command processing, same
  deactivation/stop semantics as Android.
- `HceServiceTest` - common tests against the fake.
- `HceServiceRegistryTest` - Android host test for registry wiring.

No Robolectric coverage for `HostApduService` in v1; integration testing
requires a device or emulator.

## Platform status

| Platform | Status |
|----------|--------|
| Android | Full implementation |
| iOS | `NOT_SUPPORTED` (EEA entitlement gate) |
| JVM | `NOT_SUPPORTED` |
