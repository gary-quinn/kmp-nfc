# Changelog

All notable changes to kmp-nfc are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Note:** All 0.x releases may contain breaking API changes. Pin to a specific minor version for stability.

---

## [Unreleased]

_Changes on `main` that have not yet been tagged for release._

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
