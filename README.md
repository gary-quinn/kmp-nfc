# kmp-nfc

[![CI](https://github.com/gary-quinn/kmp-nfc/actions/workflows/ci.yml/badge.svg)](https://github.com/gary-quinn/kmp-nfc/actions/workflows/ci.yml)
[![Publish](https://github.com/gary-quinn/kmp-nfc/actions/workflows/publish.yml/badge.svg)](https://github.com/gary-quinn/kmp-nfc/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.atruedev/kmp-nfc)](https://central.sonatype.com/artifact/com.atruedev/kmp-nfc)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg)](https://kotlinlang.org)

Kotlin Multiplatform NFC library for Android and iOS.

Part of the wireless trifecta: [kmp-ble](https://github.com/gary-quinn/kmp-ble) (BLE) + [kmp-uwb](https://github.com/gary-quinn/kmp-uwb) (UWB) + **kmp-nfc** (NFC).

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| **kmp-nfc** | `com.atruedev:kmp-nfc` | Core NFC — tag reading, NDEF read/write, raw transceive (ISO 7816-4 APDU) |
| **kmp-nfc-testing** | `com.atruedev:kmp-nfc-testing` | Test doubles — `FakeNfcAdapter`, `FakeNfcTag` with error injection and delay simulation |

## Setup

### Android / KMP (Gradle)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.atruedev:kmp-nfc:0.0.2")

            // Optional: test doubles for unit testing
            // testImplementation("com.atruedev:kmp-nfc-testing:0.1.0")
        }
    }
}
```

Android initialization happens automatically via AndroidX App Startup. To initialize manually:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KmpNfc.init(this)
    }
}
```

### iOS (Swift Package Manager)

In Xcode: **File > Add Package Dependencies** and enter:

```
https://github.com/gary-quinn/kmp-nfc
```

Select the version and add `KmpNfc` to your target.

```swift
import KmpNfc
```

## Usage

### Monitor adapter state

```kotlin
val adapter = NfcAdapter()

adapter.state.collect { state ->
    when (state) {
        NfcAdapterState.ON -> println("NFC ready")
        NfcAdapterState.OFF -> println("NFC disabled")
        NfcAdapterState.NOT_SUPPORTED -> println("No NFC hardware")
        NfcAdapterState.UNAUTHORIZED -> println("Permission denied")
    }
}
```

### Check capabilities

```kotlin
val adapter = NfcAdapter()
val caps = adapter.capabilities

if (caps.canReadNdef) { /* NDEF reading available */ }
if (caps.canWriteNdef) { /* NDEF writing available (iOS 13+) */ }
if (caps.canReadRawTag) { /* Raw transceive available */ }
```

### Read NFC tags

```kotlin
val adapter = NfcAdapter()

adapter.tags(ReaderOptions(alertMessage = "Hold near tag")).collect { tag ->
    tag.use {
        val ndef = it.readNdef()
        ndef?.records?.forEach { record ->
            when (record) {
                is NdefRecord.Uri -> println("URL: ${record.uri}")
                is NdefRecord.Text -> println("Text: ${record.text}")
                is NdefRecord.MimeMedia -> println("MIME: ${record.mimeType}")
                else -> {}
            }
        }
    }
}
```

### Write NDEF

```kotlin
val message = ndefMessage {
    uri("https://github.com/gary-quinn/kmp-nfc")
    text("Hello NFC", locale = "en")
}

tag.writeNdef(message)
```

### Raw APDU transceive

For ISO 7816-4 smart cards (Aliro, passports, payment cards):

```kotlin
// SELECT command
val selectAid = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07) +
    byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10)

val response = tag.transceive(selectAid)
// response includes data + SW1 + SW2 (e.g., 0x90 0x00 = success)
```

### Test without hardware

```kotlin
val adapter = FakeNfcAdapter()

adapter.tags().test {
    val tag = fakeNfcTag {
        identifier(byteArrayOf(0x04, 0x12, 0x34, 0x56))
        type(TagType.ISO_DEP)
        ndef(ndefMessage { uri("https://example.com") })
        onTransceive { command -> byteArrayOf(0x90.toByte(), 0x00) }
    }
    adapter.emitTag(tag)
    assertEquals(tag, awaitItem())
}

// Error injection
val failingTag = fakeNfcTag {
    failWith(TagLost())
    respondAfter(100.milliseconds)
}
```

## Platform Differences

NFC has significant platform asymmetry. kmp-nfc exposes this through `NfcCapabilities` rather than hiding it behind a lowest-common-denominator API.

| Feature | Android | iOS |
|---------|---------|-----|
| NDEF Read | Yes | Yes |
| NDEF Write | Yes | Yes (iOS 13+) |
| Raw Transceive | Yes (all tag types) | Yes (ISO 7816, MiFare) |
| Background Read | No (reader mode requires foreground) | Yes (URL tags only, system-managed) |
| Tag Types | NFC-A/B/F/V, ISO-DEP, MIFARE | NFC-A/B/F/V, ISO-DEP |
| Session UX | Transparent | System NFC sheet |

## Ecosystem

- [kmp-ble](https://github.com/gary-quinn/kmp-ble) — Bluetooth Low Energy (scanning, GATT, server, DFU)
- [kmp-uwb](https://github.com/gary-quinn/kmp-uwb) — Ultra-Wideband (precise ranging)
- **kmp-nfc** — NFC (tap-to-access)

Together these form the foundation for an Aliro SDK — the CSA smart lock standard combining NFC + BLE + UWB.

## Requirements

- Kotlin 2.3.20+
- Android minSdk 33
- iOS 15+
- kotlinx-coroutines 1.10+

## License

[Apache 2.0](LICENSE) — Copyright (C) 2025 Gary Quinn
