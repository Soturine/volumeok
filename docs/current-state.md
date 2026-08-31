# Current State

Last updated: 2026-08-31

## Status

**M0 is partial and awaiting physical-device validation.** The repository now contains a reproducible Android app,
deterministic domain rules, public-API Android adapters, a minimal diagnostic UI, and automated quality gates. The
execution environment had no connected Android device, so no OEM, runtime, battery, process-death, or write/readback
result is presented as proven on hardware.

## Implemented and locally verified

- Android app using Kotlin, Compose, Material 3, compile SDK 37, target SDK 36, and min SDK 26.
- English fallback plus Brazilian Portuguese and Spanish resources.
- Explicit readings for ringtone volume, ringer mode, DND state, and output-device presence.
- Deterministic readiness evaluation in which unavailable mandatory evidence cannot become `READY`.
- Controlled one-step ringtone write, fresh readback, and attempt to restore and re-read the original value.
- Domain coverage for protection bounds, pause expiry, inactive-runtime truth, and oscillation circuit breaking.
- Foreground-only `ContentObserver` experiment plus manual refresh; there is no polling loop.
- Spotless/ktlint, detekt, Android Lint, unit-test, assemble, CI, CodeQL, and Dependabot configuration.

## Experimental or not validated

- Android adapter reads and the controlled write test compile but have not run on a device in this environment.
- `Settings.System` observation is a foreground diagnostic experiment, not a proven cross-OEM event contract.
- Output APIs prove device presence only; the UI does not claim that ring audio is actively routed to that device.
- DND is read independently from ringtone volume. DND correction is not implemented.
- The normal `MODIFY_AUDIO_SETTINGS` permission enables the public write API, but effectiveness remains device-specific
  until fresh readback is recorded.

## Deferred

- Continuous/background protection and any foreground service.
- Protection preference persistence and automatic restart.
- Quick Settings tile.
- Battery/wakeup measurements.
- Safe audible playback test.
- Play readiness, signed release artifacts, tag, and GitHub release.

The UI always reports protection runtime as `STOPPED`. A stored preference cannot manufacture `ACTIVE`; no protection
preference is persisted in M0.

