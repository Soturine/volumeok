# AGENTS.md — VolumeOK

This file is the repository map and operating contract for coding agents and human contributors. Read it before changing code.

## Mission

Build VolumeOK as a trustworthy Android sound-readiness assistant. The product must help a user understand whether the phone is ready to ring, safely correct supported problems, and optionally protect against accidental silence without fighting the user or overstating platform capability.

## Canonical docs

Read these before implementation:

1. `README.md` — current product/status overview.
2. `ENGINEERING.md` — non-negotiable engineering rules.
3. `docs/product.md` — product scope and user journeys.
4. `docs/business-rules.md` — domain states/invariants/failure taxonomy.
5. `docs/architecture.md` — boundaries and source layout.
6. `docs/threat-model.md` — safety/security/privacy constraints.
7. `docs/testing.md` — verification strategy and device matrix.
8. `docs/roadmap.md` — milestones and exit criteria.
9. `docs/adr/*` — accepted architectural decisions.

If docs conflict, prefer the most specific accepted ADR for technical decisions and open/update an ADR rather than silently diverging.

## Current project phase

**Pre-code / M0 feasibility.**

Do not claim protection, DND correction, Quick Settings integration, background reliability, OEM compatibility, or Play readiness until evidence exists.

## Architecture rules

- Kotlin + Jetpack Compose + Material 3/Adaptive.
- Presentation: MVVM + UDF.
- Domain/application code must not depend directly on Android framework classes.
- Android APIs live behind explicit ports/adapters.
- Prefer package/module boundaries by feature/domain; avoid premature Gradle-module explosion.
- No generic `Utils`, `Manager`, or God-Service dumping grounds.
- Entry points are composition/bootstrap, not business logic.
- DataStore first for settings/policy; Room only when persistent history earns it.
- No backend, account system, cloud sync, LLM, or analytics SDK in MVP without a new ADR and threat/privacy review.

## Core invariants

Never violate these:

- Unknown/unverified capability is not green/READY.
- Automatic protection is opt-in.
- Automatic changes obey user minimum/maximum bounds.
- Safe tests never force 100% volume.
- The app must not fight deliberate user actions indefinitely.
- Repeated oscillation trips a circuit breaker and surfaces a user-visible reason.
- UI must not say protection is active unless the underlying mechanism is active.
- Do not infer causality that platform evidence cannot prove.
- Missing permission is a capability limitation, not success.
- AccessibilityService is not an implementation shortcut.

## Workflow

For each task:

1. Read the relevant docs/ADRs.
2. Restate internally the objective, acceptance criteria, risks, and out-of-scope.
3. Explore existing code before editing.
4. Implement the smallest coherent change.
5. Run focused tests first.
6. Run formatting/static checks for touched scope.
7. Review the diff for AI slop, dead code, placeholders, stale docs, and claims without proof.
8. Make a logical commit.
9. Push the commit to the remote repository when credentials allow.
10. Continue independent work while remote CI runs; checkpoint remote failures between milestones.

Do not wait on remote CI if another independent task can proceed safely.

## Definition of Done

A task is not done merely because it compiles.

Required as applicable:

- business rule is implemented at the correct boundary;
- negative/unknown paths are explicit;
- focused tests prove behavior;
- platform integration is tested at the real boundary where needed;
- accessibility is not regressed;
- security/privacy implications are reviewed;
- docs/ADR are updated when behavior or architecture changes;
- no relevant TODO/placeholder/fake implementation remains;
- commit is pushed remotely;
- required CI/security gates are green for the exact SHA.

Use honest status labels: `fixed`, `partial`, `experimental`, `deferred`, `not validated`.

## Testing rules

- Risk-driven test selection.
- Do not mock the behavior under test.
- Prefer deterministic fakes for platform-independent domain tests and real Android instrumentation for platform contracts.
- No arbitrary sleeps; wait for state.
- Flaky tests are defects.
- Critical logic should include negative controls/property tests where useful.
- A smoke test must prove a real consequence, not merely app/process startup.

## Security / privacy / safety

- Least privilege permissions.
- Never commit secrets.
- Avoid collecting contacts, call logs, microphone, location, or identifiers in MVP.
- Logs must not contain personal phone data.
- No remote telemetry without explicit product decision and privacy review.
- Safety matters: never unexpectedly force loud output, create infinite volume loops, or hide failed protection.

## Dependencies

Every new dependency needs a reason: necessity, license, maintenance, security, APK/build cost, compatibility. Prefer AndroidX/official platform libraries where practical. Do not add Node/Prettier to the Android project merely for formatting; Kotlin uses `.editorconfig`, ktlint/Spotless, detekt, and Android Lint.

## Releases

Release artifact must be the artifact validated from the exact source SHA:

`source SHA -> tests -> AAB -> hash -> SBOM/provenance -> tag -> staged release`

Do not rebuild a different artifact at tag time and call it equivalent.

## Agent discipline

Generated code is untrusted draft until reviewed and tested. Do not optimize for line count, file count, test count, commit count, or architecture ceremony. Optimize for correctness, clarity, evidence, safety, and maintainability.
