# Current State

Last updated: 2026-09-01

## Status

**M0 is partial with one-device physical evidence.** The repository contains a reproducible Android app,
deterministic domain rules, public-API Android adapters, a minimal diagnostic UI, automated quality gates, and a local
PowerShell/ADB evidence harness. A Motorola Edge 30 Fusion running Android 14/API 34 was exercised on exact source SHA
`f613c7e39b8087ab0f9662c6b3ead643cb941aca`. This is not cross-OEM or background-runtime evidence.

## Implemented and locally verified

- Android app using Kotlin, Compose, Material 3, compile SDK 37, target SDK 36, and min SDK 26.
- English fallback plus Brazilian Portuguese and Spanish resources.
- Explicit readings for ringtone volume, ringer mode, DND state, and output-device presence.
- Deterministic readiness evaluation in which unavailable mandatory evidence cannot become `READY`.
- Controlled one-step ringtone write, fresh readback, and attempt to restore and re-read the original value.
- Domain coverage for protection bounds, pause expiry, inactive-runtime truth, and oscillation circuit breaking.
- Foreground-only `ContentObserver` experiment plus manual refresh; there is no polling loop.
- Physical M0B harness with device/source preflight, build/install, APK hashing, bounded scenarios, state restoration,
  screenshots, connected tests, structured outcomes, and ignored raw artifacts.
- Spotless/ktlint, detekt, Android Lint, unit-test, assemble, CI, CodeQL, and Dependabot configuration.

## M1 foundation audit

M1 is **complete**. The single-module package boundaries remain clean, Android APIs stay behind adapters, localized
resources have matching EN/PT-BR/ES keys, and the current manual composition root is proportionate. Hilt, DataStore,
Coroutines/StateFlow, Material 3 Adaptive layouts, and module splitting remain deferred until a feature creates a real
need. Dependency versions are pinned in the version catalog with centralized repositories; verification metadata is a
pre-release supply-chain gate rather than a claim of the current baseline.

Gradle 9.5 reports one Gradle 10 deprecation from detekt 1.23.8 (`ReportingExtension.file(String)`). It does not
originate in project-controlled build configuration and is retained visibly pending a compatible detekt upgrade.

## Physically verified on the Motorola test device

- Public Android APIs returned valid ringtone, ringer-mode, DND, and output-device-presence evidence.
- The controlled test changed ringtone volume from `1/7` to `2/7`, proved the write with fresh readback, restored
  `1/7`, and freshly proved restoration. It never requested platform maximum.
- Priority DND produced `ACTION_REQUIRED` after explicit UI refresh and the original DND state was restored.
- Force-stop/reopen retained truthful runtime `STOPPED`; no stale `ACTIVE` state was manufactured.
- `connectedDebugAndroidTest` passed against the physical device and logged sanitized contract evidence.

## Experimental or not validated

- These results cover one Motorola model/build only; Pixel, Samsung, Xiaomi, and OPPO/Realme remain untested.
- `Settings.System` observation remains a foreground diagnostic experiment, not a proven event contract. The safe ADB
  DND transition required explicit Refresh, and the OEM shell path could not alter ringtone volume; hardware-control
  notification behavior remains manual.
- Output APIs prove device presence only; the UI does not claim that ring audio is actively routed to that device.
- DND is read independently from ringtone volume. DND correction is not implemented.
- The normal `MODIFY_AUDIO_SETTINGS` public write was effective with fresh readback on this one device; effectiveness
  remains device-specific elsewhere.
- `1/7` with normal ringer mode and DND off rendered `READY`; the threshold remains an explicit product-review item.

## Deferred

- Continuous/background protection and any foreground service.
- Protection preference persistence and automatic restart.
- Quick Settings tile.
- Battery/wakeup measurements.
- Safe audible playback test.
- Play readiness, signed release artifacts, tag, and GitHub release.

The UI always reports protection runtime as `STOPPED`. A stored preference cannot manufacture `ACTIVE`; no protection
preference is persisted in M0.
