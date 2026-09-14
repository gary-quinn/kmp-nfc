# Changelog

All notable changes to kmp-nfc are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Note:** All 0.x releases may contain breaking API changes. Pin to a specific minor version for stability.

---

## [Unreleased]

### Added
- feat: Host Card Emulation (HCE) on Android via `HceService` API
- `FakeHceService` test double in kmp-nfc-testing
- `NfcCapabilities.canHostCardEmulation` capability flag
- `ApduResponse.generalError()` (SW 6F00) for malformed inbound APDUs
- Architecture doc: [docs/HCE-ARCHITECTURE.md](docs/HCE-ARCHITECTURE.md)
- `SessionInvalidationReason` and `SessionInvalidated.reason`: iOS reader-session invalidations now
  report *why* the session ended (`USER_CANCELED`, `SESSION_TIMEOUT`, `SYSTEM_BUSY`, `UNKNOWN`), so
  a person dismissing the system NFC sheet is distinguishable from a genuine read failure without
  string-matching `localizedDescription`
- `FakeNfcAdapter.simulateSessionInvalidated()` to drive that path from tests
- `IosNfcMappingsTest` covering the Core NFC `NSError` mapping

### Changed
- HCE: serialize APDU handling with `limitedParallelism(1)`; propagate processor exceptions to `start()`
- HCE: `canPaymentCategory` reflects default Tap & Pay wallet status, not a hardcoded `true`
- HCE: invalid inbound APDUs return `6F00` instead of silent `null`
- Trimmed `HCE-ARCHITECTURE.md` to match implementation scope
- iOS: `NFCReaderErrorUnsupportedFeature`, `NFCReaderErrorRadioDisabled`, and
  `NFCReaderErrorSecurityViolation` now surface as `NotSupported`, `AdapterDisabled`, and
  `Unauthorized` - the same errors `AndroidNfcAdapter` throws eagerly for those conditions -
  instead of collapsing into a generic `SessionInvalidated`

### Fixed
- iOS: session-wide failures reaching a per-operation callback are no longer misattributed to the
  tag. `IosNfcTag` mapped every `NSError` to a fixed per-call type, so a signed build missing the
  `com.apple.developer.nfc.readersession.formats` entitlement reported `NFCErrorDomain` code 2
  from `connectToTag` as `TagLost` ("Tag connection lost"), sending readers after a hardware
  problem that did not exist. `connectToTag`, `queryNDEFStatus`, `readNDEF`, `writeNDEF`, and both
  transceive paths now prefer an adapter-level attribution and fall back to their own
  `TagLost`/`NdefFormatError`/`TransceiveError` only for genuine tag failures

### Removed
- `HceConfig.requireDeviceUnlock` and `HceConfig.description` (were not wired on Android)
- `DeactivationReason.STOPPED` (`stop()` ends `start()` normally without throwing)

---

## [0.0.5] - 2026-07-19

### Changed
- build(dependabot): bump kotlin from 2.3.20 to 2.3.21
- build(dependabot): bump com.android.kotlin.multiplatform.library from 9.1.1 to 9.2.1
- build(dependabot): bump coroutines from 1.10.2 to 1.11.0
- build(dependabot): bump gradle-wrapper from 9.4.1 to 9.5.1
- ci: exclude binaries from typography check
- build(dependabot): bump androidx.core:core-ktx from 1.18.0 to 1.19.0
- ci(dependabot): bump gradle/actions from 6.0.1 to 6.2.0
- ci(dependabot): bump actions/checkout from 6.0.2 to 7.0.0
- ci(dependabot): bump gradle/actions/setup-gradle from 6.0.1 to 6.2.0
- build(dependabot): bump gradle-wrapper from 9.5.1 to 9.6.1
- build(dependabot): bump com.vanniktech.maven.publish from 0.36.0 to 0.37.0
- ci(dependabot): bump actions/setup-java from 5.2.0 to 5.6.0
- build(dependabot): bump kotlin from 2.3.21 to 2.4.10

### Fixed
- fix: bump compileSdk to 37; ci: remove broken auto-merge step


---

## [0.0.4] - 2026-05-18

### Added
- feat: Add support for using Android's foreground dispatch

### Changed
- build(dependabot): bump com.android.kotlin.multiplatform.library from 9.1.0 to 9.1.1
- chore: add agent guidelines and typography pre-commit hook
- ci: add typography-check job
- build(dependabot): bump kotlin from 2.3.20 to 2.3.21
- build(dependabot): bump coroutines from 1.10.2 to 1.11.0
- ci: exclude binaries from typography check


---

## [0.0.3] - 2026-04-16

### Changed
- ci(dependabot): bump peter-evans/create-pull-request from 8.1.0 to 8.1.1
- ci(dependabot): bump actions/upload-pages-artifact from 4.0.0 to 5.0.0
- ci(dependabot): bump actions/github-script from 8.0.0 to 9.0.0
- ci(dependabot): bump actions/upload-artifact from 7.0.0 to 7.0.1

### Fixed
- fix: UTF-16 encoding, tag connection lifecycle, adapter validation


---

## [0.0.2] - 2026-04-05

### Fixed
- fix(ci): strip v prefix from VERSION env passed to Gradle


---

## [0.0.1] - 2026-04-05

### Added
- feat(shared): add kmp-nfc core with reader, NDEF codec, and testing module

### Other
- docs: add README, CHANGELOG, ARCHITECTURE, llms.txt, and release automation


---

[Unreleased]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.5...HEAD
[0.0.5]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.1...v0.0.2
