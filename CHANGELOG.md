# Changelog

All notable changes to kmp-nfc are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Note:** All 0.x releases may contain breaking API changes. Pin to a specific minor version for stability.

---

## [Unreleased]

_Changes on `main` that have not yet been tagged for release._

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

[Unreleased]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.3...HEAD
[0.0.3]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/gary-quinn/kmp-nfc/compare/v0.0.1...v0.0.2
