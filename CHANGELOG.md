# Changelog

All notable changes to VolumeOK are documented in this file.

The project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning will begin with the M0
feasibility baseline; entries under `Unreleased` are not production claims.

## [Unreleased]

### Added

- Native Android foundation with Kotlin, Jetpack Compose, Material 3, target SDK 36, and English, Brazilian
  Portuguese, and Spanish resource structures.
- Reproducible Gradle wrapper and executable Spotless, detekt, Android Lint, unit-test, and debug-build gates.
- Least-privilege Android CI, CodeQL, and Dependabot configuration.
- Explicit sound-evidence and readiness domain models that prevent unknown capability state from becoming `READY`.
- Bounded protection policy decisions, pause expiry, truthful runtime restart handling, and deterministic oscillation
  circuit-breaker behavior.
- Controlled ringtone-volume test contract that requires fresh readback and restores the original value.
- M0 diagnostic UI and Android adapter for ringtone volume, ringer mode, DND state, output-device presence, manual
  refresh, and foreground-only change observation.
- Local PowerShell M0B harness with ADB device/source preflight, safe build/install orchestration, structured physical
  scenarios, state restoration checks, screenshots, sanitized diagnostics, and ignored per-run reports.
- Physical instrumentation for public sound-state reads and controlled write/readback/restoration, with a safety guard
  that refuses automated writes from zero volume because zero may represent deliberate silence.

### Fixed

- Made the M0B harness compatible with UTF-8 UI text under Windows PowerShell 5.1 and restored the debug app after
  Android instrumentation removes it, so subsequent physical UI scenarios can run against the same APK.
- Distinguished ADB state/readback plus explicit UI refresh from automatic foreground observation, reporting the
  former as partial evidence that still requires physical-control validation.

### Status

- M0 remains **partial / awaiting device validation**. No continuous-protection, OEM-compatibility, background-runtime,
  Quick Settings, or production-readiness claim has been established.
