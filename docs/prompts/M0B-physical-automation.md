# Codex Prompt — M0B Physical Automation Harness

Repository: `Soturine/volumeok`
Current physical-test device available: Motorola Edge 30 Fusion, Android 14 / API 34, build `U1SJS34.2-92-10-9`.
Issue context: `#1 — M0 — Prove Android sound-readiness and protection feasibility`.

## Mission

Build a **local physical-device automation harness** for VolumeOK so that most M0 hardware validation can be executed reproducibly from a Windows development machine with an Android phone connected over ADB.

The harness must automate what is safely automatable, collect evidence, and clearly separate:

- `AUTOMATED_PUBLIC_API`
- `AUTOMATED_ADB`
- `AUTOMATED_INSTRUMENTED`
- `MANUAL_REQUIRED`
- `UNSUPPORTED`

Do not force manual-only behavior through brittle hacks merely to increase automation percentage.

Before editing, read and obey:

1. `AGENTS.md`
2. `ENGINEERING.md`
3. `README.md`
4. `docs/current-state.md`
5. `docs/business-rules.md`
6. `docs/architecture.md`
7. `docs/testing.md`
8. `docs/threat-model.md`
9. `docs/devops-devsecops.md`
10. `docs/prompts/M0-physical-validation.md`
11. `docs/adr/ADR-003-background-protection-feasibility-gate.md`
12. `docs/adr/ADR-004-defer-continuous-protection-runtime.md`

Do not weaken the current honest M0 status. Continuous protection remains unvalidated unless actual evidence supports a later decision.

---

# Workflow

Work directly on `main` unless repository policy has changed.

Use multiple logical commits and real pushes. Suggested granularity:

```text
build: add physical test harness foundation
feat: collect adb device and audio evidence
feat: automate M0 foreground scenarios
 test: add physical instrumentation assertions
feat: generate per-device M0 evidence report
 docs: document M0B usage and limitations
```

Adapt commit names to actual work; do not create empty ceremonial commits.

For each logical batch:

```text
focused implementation
→ focused test
→ commit
→ git push origin main
→ verify HEAD == origin/main
→ continue independent work while CI runs
```

Do not sit idle waiting on CI. Check remote gates at milestone boundaries. Do not repeatedly run full suites without a reason.

---

# Primary UX for the developer

The ideal developer entry point is one PowerShell command from the repository root, for example:

```powershell
.\scripts\m0-device-validation.ps1
```

If parameters improve safety, support options such as:

```powershell
.\scripts\m0-device-validation.ps1 -Serial <adb-serial>
.\scripts\m0-device-validation.ps1 -Serial <adb-serial> -SkipInstall
.\scripts\m0-device-validation.ps1 -Serial <adb-serial> -Scenario baseline
```

Do not require global Node/Python just for orchestration. Prefer PowerShell + ADB + Gradle because the project is Android/Kotlin and the current development host is Windows.

If helper Kotlin/instrumented code is the better place for a test, use it. Keep responsibilities clean:

```text
PowerShell → orchestration / artifacts / ADB shell
Gradle      → build / test execution
Android test→ in-app/platform assertions
ADB         → device identity / shell state / screenshots / logs
```

---

# Harness output

Create an ignored local artifact hierarchy such as:

```text
artifacts/m0/<timestamp>-<manufacturer>-<model>/
├── manifest.json
├── device.txt
├── source.txt
├── apk-sha256.txt
├── test-summary.md
├── logcat/
├── dumpsys/
│   ├── audio-before.txt
│   ├── audio-after.txt
│   └── notification.txt
├── screenshots/
│   ├── 01-baseline.png
│   ├── 02-after-volume-change.png
│   ├── 03-after-controlled-test.png
│   └── 04-after-restart.png
└── instrumentation/
    └── connected-test-output.txt
```

Exact structure may evolve, but artifacts must be deterministic enough to compare runs and must remain **gitignored by default**. Only sanitized, intentionally reviewed evidence belongs under `docs/evidence/`.

Do not persist unnecessary personal data.

---

# Device discovery and preflight

Automate:

```text
adb devices -l
getprop manufacturer
getprop model
getprop Android release
getprop API level
getprop display/build id
```

Also record:

```text
git rev-parse HEAD
git rev-parse origin/main
git status --porcelain
```

Fail early and clearly when:

- no device is connected;
- device is `unauthorized` or `offline`;
- multiple devices exist and no serial is selected;
- local source is dirty when a clean evidence run is required;
- `HEAD != origin/main` for a qualification/evidence run;
- app build/install fails.

Developer ergonomics matter: errors should explain the exact next action.

---

# Build/install stage

Where useful, automate:

```powershell
.\gradlew.bat assembleDebug
Get-FileHash app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Record exact APK hash in the run manifest.

Do not assume byte-for-byte reproducibility without evidence; if a hash differs from a historical build, record it rather than hiding the mismatch.

---

# Evidence collection

Collect sanitized system evidence before and after scenarios using stable public shell tooling where practical.

Candidates include:

```text
adb shell dumpsys audio
adb shell dumpsys notification
adb shell dumpsys activity activities
adb shell dumpsys package com.soturine.volumeok
```

Use only what has concrete diagnostic value; do not dump enormous unrelated system state by default.

Logcat:

- clear only when appropriate for a controlled run;
- filter around VolumeOK process/tag/package where possible;
- capture crashes, ANRs, platform exceptions and app diagnostics;
- avoid retaining unrelated personal notification/app data;
- redact before promoting evidence to `docs/evidence/`.

---

# Screenshot automation

Use ADB screenshot capture for reproducible visual evidence:

```text
adb exec-out screencap -p
```

PowerShell must write the raw binary safely; do not pipe PNG bytes through text transformations that corrupt them.

Capture screenshots at meaningful checkpoints, not every second.

At minimum:

- baseline diagnostic state;
- after controlled one-step test;
- after an automated or manual state transition when feasible;
- after force-stop/reopen.

If UI hierarchy inspection is useful, prefer supported accessibility/test tooling such as UI Automator/instrumentation rather than relying on OCR.

---

# Visual/UI verification

Automate semantic assertions where possible instead of depending only on screenshot pixel comparisons.

Preferred layers:

1. Compose semantics/tests for app UI state;
2. UI Automator/instrumentation for cross-app/system interaction when justified;
3. screenshot artifacts for human/agent review.

Do not introduce flaky screenshot-golden testing in M0 unless it has a narrow, stable purpose.

Important physical assertions include:

- `0/max + vibrate/silent` must not render `READY`;
- DND active must not render `READY` merely because ringtone volume is nonzero;
- controlled write result shown in UI matches domain result;
- runtime remains `STOPPED` where continuous protection is absent;
- after restart/force-stop stale state must not manufacture `ACTIVE`.

Also flag for product review when a technically audible but very low volume (e.g. `1/7`) becomes `READY`. Do not silently change the business threshold during harness work unless a separate tested domain decision is made.

---

# Automated scenarios

Implement a scenario model rather than one giant script.

Suggested scenarios:

```text
preflight
baseline
controlled-write
foreground-observation
force-stop-reopen
instrumented-suite
capture-only
```

Each scenario should return a structured outcome:

```text
PASS
FAIL
PARTIAL
SKIPPED
MANUAL_REQUIRED
UNSUPPORTED
```

with reason and evidence path.

## Baseline

Automate what can be read safely and compare app/system evidence where a deterministic assertion is available.

## Controlled write

Drive the app's controlled one-step test through instrumentation/UI automation when stable.

Verify or collect:

```text
original
requested
readback
restore request
restored readback
result
```

Never force platform maximum.

## Foreground observation

Determine which transitions can be safely induced by test code/ADB on the attached device without privileged hacks.

Where a transition cannot be reliably changed via public/safe automation, mark it `MANUAL_REQUIRED` and make the script pause with precise human instructions, then resume and capture evidence.

A hybrid flow is acceptable and preferable to brittle automation:

```text
Harness: "Set phone to Vibrate, then press Enter"
Human changes physical/system state
Harness captures UI/logs/dumpsys automatically
```

This still dramatically reduces manual work while preserving trustworthy evidence.

## Force-stop/reopen

Automate:

```text
adb shell am force-stop com.soturine.volumeok
relaunch app
wait on bounded observable condition
capture state
```

Do not use arbitrary long sleeps. Use bounded polling only for test synchronization when there is no better observable signal; this is not the forbidden production polling architecture.

Assert protection runtime is not falsely `ACTIVE`.

## Instrumented suite

Run once per tested SHA/device unless a failure or relevant code change justifies another run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Capture result and relevant report paths.

---

# Android test expansion

Add or refine instrumented tests only where they prove real platform behavior unavailable to pure JVM tests.

Good candidates:

- adapter read contracts on device;
- controlled write/readback/restore behavior;
- fresh snapshot after lifecycle restart;
- UI semantics backed by real adapter readings where stable.

Do not mock the effect the physical test claims to prove.

Keep domain/business tests in pure Kotlin.

Avoid test behavior that leaves the user's phone in a modified state. Use `try/finally`-style restoration patterns and fresh readback.

---

# State-changing safety rules

Physical harness must be conservative.

- Never set volume to maximum as part of an automated test.
- Record original mutable state before changing it.
- Restore state at the end, including on failure where practical.
- If restoration cannot be verified, stop subsequent state-changing scenarios and prominently report it.
- Do not toggle settings that require dangerous/privileged permissions through hacks.
- Do not use AccessibilityService as test infrastructure for behavior that can be tested via instrumentation/UI Automator.
- Never reboot/wipe/reset the phone automatically.

---

# Human-required checks

Explicitly preserve manual gates for things automation cannot truthfully prove, including:

- whether a sound was actually audible to a human;
- perceived loudness/usability;
- physical hardware-button behavior;
- genuinely faulty/stuck hardware button scenarios;
- long-duration battery behavior unless a deliberate measured endurance test is run;
- OEM dialogs/settings that cannot be safely automated;
- subjective UX/accessibility checks.

Harness should output a checklist of remaining manual items after automated scenarios finish.

---

# Report generation

Generate a compact `test-summary.md` per run containing:

```text
Device
Android/API/build
Source SHA
APK SHA-256
Run timestamp
Scenarios + outcomes
Automated findings
Failures/warnings
Manual-required items
Evidence file paths
Recommended next action
```

Optionally produce a machine-readable `manifest.json` with the same core data.

Do not automatically commit raw artifacts.

Provide a separate command or documented step to promote reviewed/sanitized evidence into `docs/evidence/M0-device-matrix.md`.

---

# Motorola Edge 30 Fusion first run

Use the currently connected Motorola as the first real target when available.

Known device facts from the physical session:

```text
manufacturer: motorola
model: motorola edge 30 fusion
Android: 14
API: 34
build: U1SJS34.2-92-10-9
```

Previously observed manually:

- `0/7 + Vibrar` rendered `Ação necessária`;
- DND off was read as `Desativado`;
- internal output presence was reported conservatively without claiming active routing;
- protection runtime remained `Parada`;
- controlled one-step test reported successful fresh readback and original-volume restoration;
- `1/7 + Normal + DND off` rendered `Pronto`, which should be flagged for a later product/business-rule threshold review rather than silently altered in M0B.

Treat these as historical observations to be reproduced/verified, not as automatic PASS results for a new SHA.

---

# CI and runtime

Do not add physical-device jobs to ordinary GitHub-hosted CI unless an actual self-hosted/device-lab infrastructure exists. GitHub CI cannot prove the attached local Motorola.

Normal remote CI should continue proving deterministic build/unit/static aspects.

The physical harness is a **local evidence tool** whose output references an exact pushed SHA.

If code changes during M0B:

- focused local tests first;
- push logical commits;
- let Android CI/CodeQL run asynchronously;
- check final gates on exact SHA before promoting physical evidence as qualification evidence.

---

# Documentation

Update documentation proportionately:

- add a usage guide such as `docs/testing-m0b.md` if needed;
- update `docs/testing.md` with physical automation architecture;
- update `.gitignore` for raw evidence artifacts;
- update `docs/current-state.md` only when actual capability status changes;
- do not mark M0 complete solely because the harness exists.

The harness is a means to produce evidence, not evidence by itself.

---

# Definition of Done — M0B harness

The harness implementation is done when:

- one documented PowerShell entry point discovers/selects an ADB device;
- preflight validates device/auth/source/build state;
- build/install can be orchestrated safely;
- device metadata, APK hash, selected dumpsys/logcat and screenshots are captured;
- scenarios produce structured PASS/FAIL/PARTIAL/SKIPPED/MANUAL_REQUIRED/UNSUPPORTED outcomes;
- force-stop/reopen is automated and checks stale runtime truth;
- connected instrumented tests can be launched and captured;
- state-changing tests restore original state or fail visibly;
- manual-only checks are explicitly surfaced;
- raw artifacts are gitignored;
- a human-readable per-device report is produced;
- harness is demonstrated on the currently available Motorola when the execution environment has access to it;
- multiple logical commits are pushed to `main`;
- final source SHA is synchronized with `origin/main`;
- remote deterministic gates are checked for the final SHA;
- M0 remains `partial` unless the broader physical evidence/ADR gate is actually satisfied.

Do not create `v0.0.1-m0` just because M0B automation exists.

---

# Final report

Return:

1. final SHA;
2. commits created;
3. harness entry command;
4. scenarios implemented;
5. physical device(s) actually exercised;
6. automated PASS/FAIL/PARTIAL outcomes;
7. manual checks still required;
8. local test/static results run because of changed code;
9. remote CI/CodeQL status for final SHA;
10. exact next recommended physical validation step.
