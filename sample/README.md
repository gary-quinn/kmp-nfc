# kmp-nfc Sample App

Compose Multiplatform demo for Android and iOS.

## Build and run

### Android

```bash
./gradlew :sample-android:installDebug
```

### iOS

1. Build the shared framework (first time, or after Kotlin changes):

```bash
./gradlew :sample:embedAndSignAppleFrameworkForXcode
```

2. Open the Xcode project and run on a **physical device** (NFC does not work in Simulator):

```bash
open iosApp/iosApp.xcodeproj
```

3. In Xcode: select the **iosApp** scheme, set your **Development Team** under Signing & Capabilities, then Run.

The Xcode project links `KmpNfcSample.framework` from `sample/build/xcode-frameworks/`. A **Compile Kotlin Framework** build phase runs Gradle automatically when building from Xcode locally.

NFC requires a physical iPhone with the Near Field Communication Tag Reading capability enabled for your team.

## Features

| Screen | APIs demonstrated |
|--------|-------------------|
| Home | `NfcAdapter.tags()`, `ReaderOptions`, simulate mode |
| Tag detail | `NfcTag` metadata, `TagType`, `TagTechnology` |
| NDEF reader/writer | `readNdef()`, `writeNdef()`, `ndefMessage {}` |
| APDU console | `transceive()`, `parseHexBytes()` |
| HCE server | `HceService`, `HceConfig`, `ApduResponse` (Android) |
| Capabilities | `NfcCapabilities` |
| Simulate tag | `FakeNfcAdapter`, `fakeNfcTag {}` |

## Quickstart code

See [NfcQuickstart.kt](src/commonMain/kotlin/com/atruedev/kmpnfc/sample/NfcQuickstart.kt) for copy-paste examples without running the full UI.

## Simulate mode

1. Enable **Simulate mode** on Home.
2. Tap **Start scan**.
3. Open **Simulate tag** and tap **Emit simulated tag**.
4. Return to Home and open the tag from **Last tag**.

The scan session lives in `App` (via `ScanSession` + `StateFlow`) so it stays active while you navigate.

## Two-phone HCE demo

1. Android phone A: **HCE server** -> Start HCE (AID `F0010203040506`).
2. Phone B: **Start scan** -> open tag -> **APDU console** -> send SELECT or custom command (INS `CA`).

## Note on kmp-nfc-testing

The sample depends on `:kmp-nfc-testing` at runtime for simulate mode only. Production apps should keep that module in `test` dependencies.
