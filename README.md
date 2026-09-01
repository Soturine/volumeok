# VolumeOK

> **Your phone. Ready to ring.**  
> PT-BR: **Seu celular. Pronto para tocar.**  
> ES: **Tu teléfono. Listo para sonar.**

VolumeOK is an Android-first **sound-readiness assistant** designed to answer one simple question: **is this phone actually ready to ring when it matters?**

Instead of being another generic volume panel, booster, or automation suite, VolumeOK focuses on **diagnosis, safe correction, and protection against accidental silence**. The primary audience includes older adults, families helping relatives, people with unreliable volume buttons, and everyday Android users who want confidence that calls and alerts will not be missed because of an accidental configuration change.

## Product principles

- **Simple before powerful.** The main screen must be understandable in seconds.
- **Readiness, not sliders.** The app explains whether the phone is ready and why.
- **Safe automation.** Automatic changes require explicit opt-in, have bounds, can be paused, and never fight the user indefinitely.
- **Fail visible.** Unknown or unverifiable state must never be presented as “Everything OK”.
- **Privacy by default.** MVP is local-first, no account, no backend, no behavioral advertising, and ideally no Internet permission.
- **Accessible by design.** Large touch targets, screen-reader semantics, font scaling, reduced motion, high contrast, and no infantilized “senior mode”.
- **International from day one.** English fallback plus PT-BR and Spanish translations from the first product milestone.
- **Engineering over claims.** Features are not considered implemented until behavior is proven on supported Android/OEM combinations.

## Core MVP

1. **Readiness status** — `READY`, `ATTENTION`, `ACTION_REQUIRED`, or `UNKNOWN`.
2. **Sound diagnosis** — ringtone level, ringer mode, Do Not Disturb state/capability, vibration-relevant state, and relevant audio-route context.
3. **Safe guided sound test** — progressive, user-confirmed testing; never force maximum volume.
4. **One-tap corrections** where Android permits them.
5. **Protection policy** — user-defined minimum ringtone level, explicit enable/disable, temporary pause, restore behavior, and safe upper bound.
6. **Oscillation/circuit-breaker behavior** — stop automatic restoration when repeated changes suggest a held/broken hardware button or a competing system/app.
7. **Quick Settings integration** when technically validated.
8. **PT-BR / English / Spanish**, light/dark theme, TalkBack and large-font support.

## Explicit non-goals for MVP

- Volume boosters above device/system limits.
- AccessibilityService hacks.
- Contact/call-log access.
- Remote family monitoring.
- Cloud accounts or sync.
- AI/LLM decision-making for readiness or protection.
- Re-creating Tasker/MacroDroid or a full profile automation suite.

## Proposed Android stack

- **Kotlin**
- **Jetpack Compose + Material 3**
- **Material 3 Adaptive**
- **MVVM + unidirectional data flow** for presentation
- **Pragmatic Clean / Ports & Adapters** around Android platform APIs
- **Coroutines + StateFlow**
- **Hilt** for dependency injection
- **DataStore** for policies/preferences; Room only if persistent history earns its complexity
- **Gradle Kotlin DSL**
- `targetSdk = 36` from the first Android scaffold
- proposed `minSdk = 26`, to be validated against target-user reach before freezing

As of **2026-08-31**, Google Play requires new mobile apps and updates to target **Android 16 / API 36**. See the official Play Console target API requirements.

## Architecture at a glance

```text
UI / Compose
    ↓ events              ↑ state
ViewModel / UDF
    ↓
Application use cases
    ↓
Domain
    ├── Readiness evaluation
    ├── Protection policy
    ├── Safe-test rules
    └── Oscillation/circuit breaker
    ↓ ports
Platform adapters
    ├── Android AudioManager
    ├── Notification/DND APIs
    ├── Audio route/capability APIs
    ├── Background execution
    └── Persistence
```

The **domain must not depend directly on `AudioManager`, `NotificationManager`, Services, Compose, or persistence frameworks**. Android APIs are adapters behind explicit contracts.

## Key invariants

- VolumeOK never raises a protected volume above a user-defined maximum.
- Automatic correction is never enabled implicitly.
- Every automatic change can be paused/disabled and must be explainable to the user.
- A denied/missing permission or unsupported OEM behavior produces `UNKNOWN`/degraded capability, not a false green state.
- DND state is not inferred only from a volume number.
- “Bluetooth connected” must not be claimed as “audio is definitely routed to Bluetooth” unless the platform evidence supports that statement.
- Safe tests never force 100% volume.
- Repeated volume oscillation trips a circuit breaker rather than creating an infinite restore loop.
- UI state must never say protection is active if the underlying mechanism is not actually active.

## Milestones

| Milestone | Goal |
| --- | --- |
| M0 — Feasibility | Prove required Android capabilities, OEM behavior, DND semantics, and viable background protection on real devices. |
| M1 — Foundation | Android scaffold, domain contracts, adapters, CI/quality gates, docs, threat model. |
| M2 — Diagnose | Readiness snapshot, issue taxonomy, recommendations and corrections. |
| M3 — Safe Test | Guided accessible audio test. |
| M4 — Protection | Minimum policy, pause/resume, restoration and circuit breaker. |
| M5 — UX & i18n | Adaptive UI, fox mascot system, PT/EN/ES, TalkBack, font scaling, reduced motion. |
| M6 — Hardening | OEM/device matrix, process death, battery/background, regression and security testing. |
| M7 — Play Beta | API 36, policy declarations, internal/closed testing, Android Vitals baseline. |
| M8 — v1.0 | Exact-SHA qualification, reproducible AAB, SBOM/provenance, staged rollout. |
| M9+ | Profiles, optional family features, VIP contacts, history — only after separate privacy/threat reviews. |

## Documentation map

- [`AGENTS.md`](AGENTS.md) — repository map and rules for Codex/Claude/Copilot/human contributors.
- [`ENGINEERING.md`](ENGINEERING.md) — project-specific engineering constitution.
- [`docs/product.md`](docs/product.md) — product definition, users, flows, scope and acceptance principles.
- [`docs/business-rules.md`](docs/business-rules.md) — domain model, states, invariants and failure taxonomy.
- [`docs/architecture.md`](docs/architecture.md) — module boundaries, ports/adapters and proposed source structure.
- [`docs/benchmark.md`](docs/benchmark.md) — competitor and market benchmark snapshot.
- [`docs/ux-design.md`](docs/ux-design.md) — UX, accessibility, responsive/adaptive rules, fox mascot and microinteractions.
- [`docs/testing.md`](docs/testing.md) — risk-driven QA strategy and OEM/device matrix.
- [`docs/testing-m0b.md`](docs/testing-m0b.md) — local physical-device harness usage and evidence boundaries.
- [`docs/threat-model.md`](docs/threat-model.md) — security/privacy/safety model.
- [`docs/devops-devsecops.md`](docs/devops-devsecops.md) — CI/CD, supply chain, release and security gates.
- [`docs/observability-aiops-llmops.md`](docs/observability-aiops-llmops.md) — proportional observability and explicit AI/AIOps scope.
- [`docs/roadmap.md`](docs/roadmap.md) — detailed milestone plan and exit criteria.
- [`docs/research-sources.md`](docs/research-sources.md) — dated external research and official Android/Play references.
- [`docs/adr/`](docs/adr/) — architectural decision records.

## Current status

**Status: M0 partial / one-device physical evidence.**

The repository contains an executable diagnostic foundation, domain invariants, Android public-API adapters, a minimal
Compose UI, local/remote quality gates, and a reproducible PowerShell/ADB harness. A Motorola Edge 30 Fusion running
Android 14/API 34 passed the baseline, controlled write/readback/restoration, force-stop/reopen, and connected-test
scenarios on an exact source SHA. Foreground observation remains partial, and continuous protection, cross-OEM
behavior, background reliability, battery impact, Quick Settings, and Play readiness remain unvalidated. See
[`docs/current-state.md`](docs/current-state.md) and the
[`M0 device matrix`](docs/evidence/M0-device-matrix.md).

## Build and verify

Use JDK 17 and an Android SDK with platform 37 installed. The app targets API 36 as required by the M0 contract.

```text
./gradlew spotlessCheck testDebugUnitTest detekt lintDebug assembleDebug
```

On Windows, use `gradlew.bat`. The debug APK is generated under `app/build/outputs/apk/debug/`. Instrumented tests are
compiled by CI-ready Gradle tasks but require an attached emulator or physical device to execute.

## License / rights

This repository is currently **not open source**. No permission to copy, modify, redistribute, sublicense, or use the code/assets is granted unless explicitly stated in a future license. See [`LICENSE`](LICENSE).

---

**VolumeOK** — confidence that your phone is ready when it matters.
