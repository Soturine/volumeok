# Product Definition

## Problem

Android users frequently miss calls or alerts because the device is silent, ringtone volume is too low, Do Not Disturb is active, or the current sound state is misunderstood. Existing apps mostly expose sliders, automation, or technical diagnostics. VolumeOK focuses on a simpler user outcome: **is the phone ready to ring, and what should the user do if it is not?**

## Primary users

1. Older adults who want confidence that the phone will ring.
2. Family members helping configure a relative's Android device.
3. Users who accidentally silence or lower ringtone volume.
4. Users with unreliable/stuck physical volume buttons.
5. General Android users who want a simple readiness check without power-user automation.

## Jobs to be done

- Check whether the phone is currently ready to ring.
- Understand why a call may be missed.
- Safely correct a problem with minimal steps.
- Protect against accidental silence after explicit opt-in.
- Pause protection intentionally without losing configuration.
- Test audible output without sudden maximum-volume playback.

## Critical user journeys

### CUJ-1 — Check readiness

Open app → observe clear readiness state → inspect issues if any.

Acceptance:
- state is never falsely green when a required capability is unavailable;
- actionable issue descriptions use plain language;
- no technical jargon is required to understand the status.

### CUJ-2 — Diagnose and correct

App detects low ringtone volume → user sees consequence → user chooses correction → platform state is re-read → UI confirms the actual new state.

The UI must not assume a setter succeeded solely because the API returned without throwing.

### CUJ-3 — Safe sound test

User initiates test → test explains what will happen → sound starts at a safe level/progression → user confirms whether it was heard → app either completes or guides diagnosis.

### CUJ-4 — Enable protection

User selects minimum allowed ringtone level → explicitly enables protection → VolumeOK reports whether the protection mechanism is genuinely active.

### CUJ-5 — Intentional silence

User intentionally changes to silence/low volume → VolumeOK offers or honors temporary pause semantics → no endless tug-of-war occurs.

### CUJ-6 — Repeated oscillation

Volume repeatedly changes between protected and unprotected values → protection circuit breaker opens → automatic restore pauses → user receives a clear explanation and recovery action.

## MVP scope

- Readiness snapshot and status.
- Ringer mode / ringtone-volume analysis.
- DND state/capability analysis.
- Relevant audio-route context where the Android API can support a defensible claim.
- Safe sound test.
- One-tap correction where platform APIs permit it.
- Explicit ringtone protection policy.
- Temporary protection pause.
- Circuit breaker for repeated oscillation.
- Quick Settings tile if validated during feasibility.
- Offline/local-first behavior.
- English, PT-BR and Spanish.
- Accessible/adaptive Compose UI.

## Post-MVP candidates

- Profiles and schedules.
- Local event/history timeline.
- Optional PIN for family-assisted configuration.
- VIP/priority-call features after a dedicated permission/privacy/policy review.
- Remote family status only after a separate backend/auth/privacy architecture.

## Non-goals

- Volume booster beyond system/device limits.
- Claiming to guarantee every call will ring regardless of Android/OEM/carrier/app behavior.
- AccessibilityService automation hacks.
- Full automation-engine replacement for Tasker/MacroDroid.
- Contact/call-log access in MVP.
- Cloud accounts in MVP.
- LLM-based readiness decisions.

## Product language

Brand: **VolumeOK**

EN: **Your phone. Ready to ring.**
PT-BR: **Seu celular. Pronto para tocar.**
ES: **Tu teléfono. Listo para sonar.**

The brand stays the same in every market. Product copy is localized rather than forcing the brand name to translate.

## Success signals

Do not optimize primarily for downloads or feature count. Useful initial product signals include:

- diagnosis completion rate;
- percentage of detected issues resolved successfully;
- false-positive/false-green reports;
- protection restore success by Android/OEM version;
- protection circuit-breaker frequency;
- crash/ANR rate;
- accessibility/usability findings from real users;
- uninstall reasons and qualitative reviews.
