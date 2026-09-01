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
- Product-quality Diagnose home with plain-language issue copy, optional diagnostic details, accessible full-width
  actions, and a supported one-step low-volume correction verified by fresh readback.
- Guided local safe-sound test with explicit start/retry/stop controls, bounded platform-derived steps below maximum,
  lifecycle cancellation, no microphone or Internet dependency, and user-confirmed outcomes.
- Focused domain, correction, Compose UI, and physical Android tests for M2/M3 decisions and safety boundaries.

### Changed

- Completed the M1 foundation gap audit without adding unused Hilt, DataStore, coroutine, Adaptive, or module-split
  machinery; documented their real adoption triggers and the deferred dependency-verification release gate.
- Defined the M2 low-volume rule: zero requires action, the lowest non-zero platform step requires attention, and
  higher steps may be ready only when all other mandatory evidence is healthy.
- Updated the physical harness for the product Diagnose surface while keeping engineering controls behind expandable
  details and treating accidental-silence protection as unavailable, never active.

### Fixed

- Made the M0B harness compatible with UTF-8 UI text under Windows PowerShell 5.1 and restored the debug app after
  Android instrumentation removes it, so subsequent physical UI scenarios can run against the same APK.
- Distinguished ADB state/readback plus explicit UI refresh from automatic foreground observation, reporting the
  former as partial evidence that still requires physical-control validation.

### Status

- M0 remains **partial with one-device Motorola evidence**. Public reads, controlled write/readback/restoration,
  force-stop/reopen truth, and physical instrumentation passed; foreground observation is partial. No broad
  OEM-compatibility, continuous-protection, background-runtime, Quick Settings, or production-readiness claim has been
  established.
- M1 is **complete**; M2 Diagnose is **complete for its implemented scope and available-device evidence**; M3 Safe
  Test is **partial/experimental** until human audibility, comfort, TalkBack, 200% font, and contrast checks pass.
