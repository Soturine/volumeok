# Codex Prompt — M0 VolumeOK Feasibility + First Executable Foundation

Repository: `Soturine/volumeok`

Issue: `#1 — M0 — Prove Android sound-readiness and protection feasibility`

## Mission

Execute M0 for VolumeOK and turn the documentation-only repository into the **smallest professional Android foundation capable of proving the critical platform assumptions** behind sound readiness and safe volume protection.

Do not optimize for LOC, number of tests, or architectural ceremony. Optimize for evidence, correctness, Android-platform truth, maintainability, accessibility, and fast feedback.

Read and obey before editing:

1. `AGENTS.md`
2. `ENGINEERING.md`
3. `README.md`
4. `docs/product.md`
5. `docs/business-rules.md`
6. `docs/architecture.md`
7. `docs/testing.md`
8. `docs/threat-model.md`
9. `docs/devops-devsecops.md`
10. `docs/roadmap.md`
11. `docs/adr/ADR-001-native-android-stack.md`
12. `docs/adr/ADR-002-local-first-no-backend-mvp.md`
13. `docs/adr/ADR-003-background-protection-feasibility-gate.md`
14. GitHub Issue #1

Treat repository docs as current project contracts. If implementation evidence disproves a current claim, **update the documentation instead of forcing the code to match a false assumption**.

---

# Non-negotiable workflow

Work directly on `main` unless the repository itself has changed to require PRs.

Use **multiple small logical commits**, not one giant commit.

Expected rhythm:

```text
explore
→ plan
→ focused implementation
→ focused test
→ logical commit
→ git push origin main
→ verify HEAD == origin/main
→ continue independent next task while remote CI runs
→ remote checkpoint between milestones
```

## CI rule

Do **not** sit idle waiting for every remote suite.

After a logical milestone is pushed:

- let CI/Security run remotely;
- continue another independent task when safe;
- check remote state at milestone boundaries;
- fix real red gates early;
- do not poll continuously;
- cancel/ignore superseded runs when appropriate;
- do not repeatedly run the same full suite locally and remotely without a reason.

Use focused local tests during development. Run the expensive/full local qualification **once near release candidate**, plus only when blast radius justifies it.

A green CI from an older SHA is not evidence for the current SHA.

---

# Git discipline

Every meaningful batch must produce a descriptive commit and remote push.

Examples of the intended granularity; adapt to actual work:

```text
build: scaffold native Android project
ci: add Android quality gates
feat: model sound readiness domain
feat: implement Android audio state adapter
feat: add M0 diagnostic spike UI
feat: verify controlled ringtone write and readback
feat: add protection runtime experiment
 test: add readiness and protection invariant coverage
 docs: record M0 device evidence and architecture decision
chore: qualify M0 release candidate
```

Do not create empty/ceremonial commits just to increase the count.

After each push where practical verify:

```text
git rev-parse HEAD
git rev-parse origin/main
```

They must match before claiming the milestone is remotely synchronized.

---

# Technical baseline to create

Create the Android project only after checking current repository state and current official Android tooling compatibility.

Target intent:

- app name: `VolumeOK`
- namespace/applicationId: `com.soturine.volumeok`
- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- `targetSdk = 36`
- proposed `minSdk = 26` unless current evidence gives a better reason
- Coroutines / StateFlow when asynchronous/state behavior requires them
- Hilt only if/when dependency composition benefits from it; do not create DI ceremony before needed
- DataStore only for actual persistent policies/preferences
- no Room in M0 unless real persistent relational/event requirements justify it
- no backend
- no account
- no AI/LLM
- no AccessibilityService workaround
- no contacts/call-log/location/camera/microphone permission in M0
- avoid INTERNET permission unless a concrete M0 requirement demands it

Use the architecture contracts already specified in the repository. Keep Android framework APIs outside the pure domain.

---

# Formatting and static quality

The repository must gain executable formatting/lint gates appropriate to Kotlin rather than Node-based ceremony.

Use:

- `.editorconfig` as style authority;
- Spotless + ktlint for Kotlin/Gradle formatting;
- detekt for static/code-quality analysis where compatible and useful;
- Android Lint for Android correctness/security/performance/accessibility/i18n.

Do **not** add Node/npm merely to run Prettier on Kotlin.

If a future JS/TS subproject is introduced, Prettier belongs in that subproject only.

Prefer pinned/controlled tool versions and a version catalog where useful. Any new dependency must have a reason, license/security/maintenance review, and measurable build/runtime cost awareness.

---

# M0 product questions to prove

Do not proceed as if these are already solved.

## 1. Read sound readiness inputs

Prove what can actually be read on supported Android versions/devices for:

- ringtone stream current/max value;
- ringer mode;
- DND state/capability under current Android semantics;
- relevant audio-route/device information;
- any capability the current product docs claim.

Unknown/unavailable state must remain explicit.

## 2. Controlled write + readback

Where Android permits a ringtone-volume write:

```text
read current
→ request controlled change
→ fresh readback
→ compare actual state
```

A setter returning without exception is **not proof that the state changed**.

Do not test by forcing 100% volume.

## 3. Observation mechanism

Investigate the least costly reliable mechanism for detecting relevant sound-state changes.

Forbidden default design:

```text
while (true) poll()
```

Record trade-offs and device/API behavior.

## 4. Continuous protection feasibility

Compare only mechanisms that current Android APIs/policies can actually support.

The app must not claim `ACTIVE` merely because a preference says protection is enabled.

Runtime truth must distinguish at least:

```text
STOPPED
STARTING
ACTIVE
PAUSED_BY_USER
SUSPENDED_BY_CIRCUIT_BREAKER
DEGRADED
ERROR
```

If foreground execution is required, validate user visibility, lifecycle, policy suitability, stop semantics, battery impact, and OEM behavior before selecting it.

## 5. Oscillation/circuit breaker

Model a scenario such as:

```text
70 → 0 → 70 → 0 → 70 → 0
```

and prove protection suspends rather than fighting indefinitely.

Do not claim the hardware button is broken; report that repeated conflicting changes were detected and explain possible causes.

## 6. Process death / restart

Kill/restart the process and prove UI/runtime truth is not falsely restored from stale preference state.

## 7. Quick Settings

Determine whether a Quick Settings tile materially improves MVP and can truthfully represent runtime protection state. Implement only if it earns its complexity during M0/M1.

---

# Minimum UI for the spike

Do not spend M0 on final visual polish.

Create only enough Compose UI to exercise and observe the domain/platform behavior:

```text
VolumeOK

[ current readiness ]
READY / ATTENTION / ACTION REQUIRED / UNKNOWN

Ringtone: n / max
Ringer mode: ...
DND: ... / unavailable
Audio route: ... / unknown
Protection runtime: ...

[ Refresh ]
[ Safe test / controlled test action ]
[ Enable/disable experimental protection, only if supported ]
```

Use accessible semantics and strings through Android resources from the beginning. English fallback + PT-BR + Spanish structure must be ready even if M0 copy is minimal.

The fox mascot and polished product UI belong to later product milestones unless a lightweight placeholder asset already exists.

---

# Domain and safety invariants

Implement/test the existing repository invariants, especially:

- `UNKNOWN` can never be rendered/converted to `READY` by defaulting missing values;
- automatic correction requires explicit user opt-in;
- protected target always respects policy bounds and Android max;
- safe test never requires maximum system volume;
- pause suppresses automatic restoration for the pause lifetime;
- runtime inactive/dead cannot appear as active protection;
- repeated opposing volume changes trip a circuit breaker;
- automatic actions are explainable and reversible where meaningful;
- DND is not inferred only from ringtone percentage;
- connected Bluetooth/device presence is not overclaimed as definite active routing without sufficient evidence.

Prefer pure Kotlin tests for domain rules.

---

# QA strategy during M0

Use risk-driven proof.

Minimum automated evidence should cover:

- readiness state composition;
- missing/unknown capability handling;
- protection bounds;
- pause/expiry with controllable clock;
- oscillation detection/circuit-breaker;
- failed/ineffective platform writes;
- no false green state.

Use negative controls/mutation-style checks for critical invariants when practical: deliberately break an invariant locally and make sure the relevant test would fail; revert before commit.

Instrumented/device tests should prove only behavior that genuinely requires Android.

Do not mock the exact effect a test claims to prove.

Avoid arbitrary sleeps. Synchronize on observable state with bounded timeouts.

---

# Real-device evidence

M0 is not complete from emulator-only evidence.

Create a dated matrix document such as:

`docs/evidence/M0-device-matrix.md`

Record where available:

- date;
- manufacturer;
- model;
- Android/API;
- build/skin;
- read ringtone result;
- write + readback result;
- DND result;
- observation mechanism result;
- background/protection result;
- process death result;
- notable OEM behavior;
- status: proven / partial / unsupported / not tested.

Priority when physical devices are available:

1. Pixel/AOSP-like
2. Samsung/One UI
3. Motorola
4. Xiaomi/HyperOS
5. OPPO/Realme

Do not fabricate results for devices not available in the execution environment. Mark them `not tested` and leave explicit manual verification instructions.

---

# CI / DevSecOps to establish once Gradle exists

Add GitHub Actions only after the project actually has executable Gradle tasks.

Keep normal push CI efficient. Typical normal gate, adapted to actual plugin/task names:

```text
compile/build relevant debug artifact
focused/unit tests
Spotless/ktlint check
Detekt
Android Lint
```

Heavier instrumented/emulator/security/release qualification runs belong in appropriate milestone/release workflows, not every tiny commit unless they are cheap enough to justify it.

Also configure proportionately:

- Dependabot;
- CodeQL for Java/Kotlin when repository content makes it useful;
- least-privilege GitHub Actions permissions;
- dependency verification/locking strategy;
- no secrets in repository;
- actions pinned to immutable SHAs where practical;
- dependency review where repository/plan support permits it.

Do not claim security tooling is active merely because documentation mentions it; verify actual GitHub workflows/settings/results where accessible.

---

# Documentation updates required from evidence

Update current-truth documentation alongside implementation.

At minimum:

- `README.md`
- `docs/current-state.md` (create if not present)
- `docs/architecture.md`
- `docs/business-rules.md` if rules change
- `docs/testing.md`
- `docs/roadmap.md`
- `docs/evidence/M0-device-matrix.md`
- ADR selecting or rejecting a continuous-protection runtime mechanism after evidence exists

Use honest states:

```text
proven
partial
experimental
unsupported
deferred
not validated
```

Do not describe later roadmap features as implemented.

---

# Release qualification

When M0 implementation is complete and scope is frozen:

1. Push the candidate first.
2. Let remote CI/security run.
3. In parallel, run **one** appropriate full local RC suite from the same candidate SHA; do not duplicate expensive suites repeatedly.
4. Resolve real failures.
5. Push fixes as separate logical commits.
6. Confirm:

```text
HEAD == origin/main
```

7. Confirm required remote gates are green for that exact SHA.
8. Confirm docs and claims match runtime evidence.
9. Confirm working tree is clean.

Do not tag an unpushed SHA or rely on green checks from an older commit.

---

# Tag and GitHub Release

If and only if M0 acceptance criteria are met and the exact final SHA is qualified, create:

```text
v0.0.1-m0
```

Annotated tag message:

```text
VolumeOK M0 — Android feasibility baseline
```

Push the exact tag.

Then create a GitHub prerelease/release named:

```text
VolumeOK v0.0.1-m0 — Feasibility Baseline
```

Release notes must include:

- exact source SHA;
- what was actually proven;
- which devices/APIs were tested;
- limitations / `not validated` items;
- selected or still-open background-protection decision;
- tests/gates run;
- artifact(s), if any, and hashes if an APK/AAB is intentionally attached;
- no claims of production readiness.

If available tooling cannot create the release safely, do **not** fake it. Leave the repo/tag fully pushed and provide the exact GitHub CLI/UI command/steps required as the only remaining manual action.

Do not create `v1.0.0` during M0.

---

# Definition of Done for this task

M0 is done only when:

- Android scaffold/build is real and reproducible;
- critical domain model exists outside Android UI/framework code;
- M0 reads have runtime evidence;
- controlled write uses fresh readback;
- unknown/unsupported states fail visibly;
- protection mechanism is either proven or honestly marked unsupported/experimental;
- oscillation circuit breaker is proven in deterministic tests;
- process-death/stale-state problem is addressed/tested proportionately;
- formatting/static-quality gates are executable;
- CI exists and proves real Gradle behavior;
- docs match implementation;
- multiple logical commits were pushed to `main` throughout work;
- exact final SHA is remotely synchronized;
- required checks are green for exact final SHA;
- `v0.0.1-m0` + GitHub release exist only if qualification succeeded;
- Issue #1 is updated/closed with evidence only when genuinely complete.

If physical-device evidence cannot be completed in the agent environment, finish every automatable part, create clear manual device steps, and leave M0 status **partial / awaiting device validation**. In that case do not falsely release a fully qualified M0; a clearly labeled experimental prerelease may be used only if repository policy and evidence justify it.

---

# Final report format

Return a compact report with:

1. final SHA;
2. commits created;
3. source files/modules added;
4. M0 findings;
5. tests and static gates run;
6. remote CI/security status for final SHA;
7. device evidence actually obtained;
8. unresolved/not-validated items;
9. tag/release URL if created;
10. exact next recommended task.

Do not equate number of tests, files, commits, or LOC with quality.
