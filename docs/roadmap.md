# Roadmap

## M0 — Feasibility

Status on 2026-09-01: **partial / one-device physical evidence**. The executable diagnostic foundation,
deterministic rules, controlled write/readback workflow, physical harness, and CI exist. A Motorola Edge 30 Fusion
proved public reads, controlled write/readback/restoration, and truthful force-stop/reopen behavior. Foreground
notification behavior is still partial, other OEMs are untested, and background/battery evidence plus the
runtime-mechanism decision remain open.

Goal: prove the product is technically honest on Android 16-era devices before promising continuous protection.

Tasks:
- spike AudioManager read/write behavior;
- verify ringer-mode semantics;
- verify DND observation/correction boundaries;
- test viable change-observation mechanisms;
- test foreground/background viability and policy implications;
- test at least Pixel/AOSP-like + Samsung + Motorola if hardware is available;
- document unsupported/ambiguous OEM behavior;
- decide whether Quick Settings tile belongs in MVP;
- produce evidence table with device/API/date/result.

Exit criteria:
- required capabilities classified SUPPORTED / DEGRADED / UNSUPPORTED;
- no false claim that background protection is solved;
- ADR for background mechanism accepted;
- product scope adjusted to evidence.

## M1 — Foundation

Status on 2026-09-01: **complete**. The foundation created during M0/M0B passed a gap audit. Hilt, DataStore,
Coroutines/StateFlow, Material 3 Adaptive layouts, and module splitting were not added without a present requirement;
their triggers are documented instead. The remaining Gradle 10 deprecation is emitted internally by detekt 1.23.8
and is tracked for a compatible plugin upgrade rather than suppressed.

- Android project scaffold;
- Kotlin/Compose/Material 3;
- target API 36;
- dependency injection, async state, and persistence only when required by a real feature;
- domain contracts and first pure tests;
- Spotless+ktlint, detekt, Android Lint;
- CI/security baseline;
- EN/PT-BR/ES resource structure;
- threat model and ADRs current.

Exit: clean build, meaningful unit tests, static gates green on remote SHA.

## M2 — Diagnose

Status on 2026-09-01: **complete for the implemented M2 scope and available-device evidence**. ADR-005 fixes the
lowest-nonzero rule, the product Diagnose surface replaces the M0 debug panel, and the supported Motorola correction
path is verified by fresh readback. This does not close M0C or establish cross-OEM compatibility.

- SoundSnapshot adapter;
- capability model;
- deterministic readiness engine;
- issue taxonomy;
- plain-language recommendations;
- one-tap corrections where supported;
- mandatory post-correction re-read.

Exit: CUJ-1 and CUJ-2 proven on representative devices.

## M3 — Safe Test

Status on 2026-09-01: **partial / experimental**. The guided generated-tone flow, bounded steps below maximum,
immediate/lifecycle stop, localized accessible UI, and deterministic/Android boundary tests exist. Human audible,
comfort, TalkBack, 200% font, and contrast validation on a physical device remain required for exit.

- guided test flow;
- bounded/progressive playback;
- immediate stop;
- accessibility semantics;
- outcome/recovery guidance.

Exit: CUJ-3 proven without forced maximum volume.

## M4 — Protection

- policy persistence;
- explicit enable/disable;
- min/max bounds;
- notify-only vs auto-restore if retained by product evidence;
- pause/override;
- runtime-truth reporting;
- oscillation circuit breaker;
- process-death/restart behavior.

Exit: CUJ-4 through CUJ-6 proven; battery/background behavior acceptable.

## M5 — UX / Internationalization

- final fox mascot system;
- adaptive compact/medium/expanded layouts;
- PT-BR/EN/ES copy review;
- TalkBack;
- 200% font scale;
- reduced motion;
- dark/light themes;
- onboarding/help only where needed.

Exit: accessibility and localization acceptance pass.

## M6 — Hardening

- expanded OEM/device matrix;
- malicious/corrupt persisted-state tests;
- permission revoke paths;
- regression suite;
- Macrobenchmark baseline;
- battery/wakeup profiling;
- independent architecture/security/test audit.

## M7 — Play Beta

- Data Safety/policy declarations;
- internal + closed testing;
- pre-launch report review;
- Android Vitals baseline;
- store listing copy constrained to proven capabilities;
- staged-rollout/rollback plan.

## M8 — v1.0

- scope freeze;
- exact final SHA remote;
- release qualification;
- AAB + checksum + SBOM/provenance;
- tag exact green SHA;
- staged production rollout;
- post-release monitoring and retrospective.

## M9+ — Only after evidence

Candidates:
- profiles/schedules;
- local event timeline;
- PIN/family-assisted configuration;
- VIP/priority contacts;
- remote family status;
- optional support/explanation AI.

Each high-risk candidate requires its own ADR, privacy/threat review and acceptance criteria.
