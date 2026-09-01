# Current State

Last updated: 2026-09-01

## Status

**M0C remains partial with one-device physical evidence.** M1 is complete, M2 Diagnose is implemented, and M3 Safe
Test is experimental/partial pending human audible and accessibility checks. A Motorola Edge 30 Fusion running Android
14/API 34 was exercised on exact synchronized source SHA `b70b3fc53a04ee05b7b5ec8453e7caa0ebb4d227`. This is not
cross-OEM, human-audibility, accessibility, or background-runtime evidence.

## Implemented and locally verified

- Android app using Kotlin, Compose, Material 3, compile SDK 37, target SDK 36, and min SDK 26.
- English fallback plus Brazilian Portuguese and Spanish resources.
- Explicit readings for ringtone volume, ringer mode, DND state, and output-device presence.
- Deterministic readiness evaluation in which unavailable mandatory evidence cannot become `READY`; ordinal step `1`
  is explicitly `ATTENTION` under ADR-005.
- Product Diagnose home with plain-language issue copy, safe supported correction CTA, EN/PT-BR/ES resources,
  accessibility semantics, and engineering controls behind optional diagnostic details.
- Narrow `1 -> 2` low-volume correction with fresh readback and explicit effective/ineffective failure taxonomy.
- Explicitly initiated generated local-tone flow with bounded platform-derived gain steps below maximum, immediate
  stop, lifecycle interruption handling, and user-confirmed outcomes. It does not mutate system ringtone volume.
- Controlled one-step ringtone write, fresh readback, and attempt to restore and re-read the original value.
- Domain coverage for protection bounds, pause expiry, inactive-runtime truth, and oscillation circuit breaking.
- Foreground-only `ContentObserver` experiment plus manual refresh; there is no polling loop.
- Physical M0B harness with device/source preflight, build/install, APK hashing, bounded scenarios, state restoration,
  screenshots, connected tests, structured outcomes, and ignored raw artifacts.
- Spotless/ktlint, detekt, Android Lint, unit-test, assemble, CI, CodeQL, and Dependabot configuration.

## M2 Diagnose status

M2 implementation and the available-device CUJ-1/CUJ-2 evidence are **complete**. The primary screen is no longer an
engineering panel; unknown, muted, lowest-nonzero, silent, vibrate-only, DND, route-evidence, capability inconsistency,
and correction-readback outcomes remain explicit. The Motorola instrumentation proved the product correction request
and fresh readback, with defensive restoration by the test. Cross-OEM qualification remains an M0C/M6 concern rather
than a claim that M2 works identically everywhere.

## M3 Safe Test status

M3 is **partial / experimental**. Deterministic progression, cancellation, stale-callback rejection, lifecycle stop,
UI outcomes, and the Android local-tone boundary are automated. The physical API test proved that starting/stopping
the generated player does not change the system ringtone step. No agent or automation confirmed that a person heard
the tone, judged it comfortable, or completed TalkBack/200% font/contrast checks, so CUJ-3 and the M3 exit gate remain
open.

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
- The M2 `1 -> 2` correction produced an effective fresh readback; the instrumentation restored the starting step in
  `finally` to leave the test device unchanged.
- The M3 Android player contract started/stopped locally without changing the ringtone setting. This is API/state
  evidence only, not an audibility claim.

## Experimental or not validated

- These results cover one Motorola model/build only; Pixel, Samsung, Xiaomi, and OPPO/Realme remain untested.
- `Settings.System` observation remains a foreground diagnostic experiment, not a proven event contract. The safe ADB
  DND transition required explicit Refresh, and the OEM shell path could not alter ringtone volume; hardware-control
  notification behavior remains manual.
- Output APIs prove device presence only; the UI does not claim that ring audio is actively routed to that device.
- DND is read independently from ringtone volume. DND correction is not implemented.
- The normal `MODIFY_AUDIO_SETTINGS` public write was effective with fresh readback on this one device; effectiveness
  remains device-specific elsewhere.
- Physical normal/vibrate/silent controls, OEM-UI DND notification latency, Bluetooth/wired presence, actual tone
  audibility/comfort, TalkBack, 200% font scale, contrast, and broken/stuck-button behavior remain manual and untested.
- Samsung/One UI and Pixel/AOSP-like physical qualification remain not tested; ADR-004 therefore stays deferred.

## Deferred

- Continuous/background protection and any foreground service.
- Protection preference persistence and automatic restart.
- Quick Settings tile.
- Battery/wakeup measurements.
- Play readiness, signed release artifacts, tag, and GitHub release.

Normal product UI says accidental-silence protection is unavailable; optional diagnostics never manufacture an active
runtime. No protection preference or production runtime exists.
