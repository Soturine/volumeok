# M0B Physical Validation Harness

The M0B harness runs repeatable, conservative feasibility checks against one Android phone connected to a Windows
development machine. It reduces manual work but does not certify broad OEM compatibility or continuous protection.

## Prerequisites

- JDK 17 and the Android SDK required by the project;
- Android SDK Platform Tools (`adb`) available through the SDK environment or `PATH`;
- one unlocked, ADB-authorized physical device;
- a clean `main` worktree whose `HEAD` matches `origin/main`.

The source check is fail-closed because evidence must identify the exact code under test. `-AllowDirtySource` exists
for harness development only; runs using it are explicitly warned and are not qualification evidence.

## Commands

Run the complete flow:

```powershell
.\scripts\m0-device-validation.ps1 -Serial <adb-serial>
```

Run one or more selected scenarios:

```powershell
.\scripts\m0-device-validation.ps1 -Serial <adb-serial> -Scenario baseline
.\scripts\m0-device-validation.ps1 -Serial <adb-serial> -Scenario controlled-write,force-stop-reopen
```

Supported scenarios are `preflight`, `baseline`, `controlled-write`, `foreground-observation`,
`force-stop-reopen`, `instrumented-suite`, and `capture-only`. `all` runs the full sequence. `-SkipInstall` is useful
only when the required APK is already installed and no selected scenario needs restoration after instrumentation.

## Safety and state restoration

- The harness records the original mutable state before attempting a change.
- Automated ringtone tests never request platform maximum.
- A zero ringtone is treated as possible deliberate silence and is not automatically raised by the controlled test.
- DND and ringtone state are restored and freshly verified where the platform permits automation.
- A restoration failure stops later mutable scenarios and produces a visible failure.
- The harness never reboots, wipes, resets, uses AccessibilityService, or bypasses privileged settings.

Some OEM shell implementations accept a command without changing the requested sound state. The harness verifies fresh
readback and reports `PARTIAL` or `MANUAL_REQUIRED` when safe automation cannot establish the transition.

## Evidence output

Every run creates an ignored directory under:

```text
artifacts/m0/<UTC timestamp>-<manufacturer>-<model>/
```

The directory contains a compact `test-summary.md`, machine-readable `manifest.json`, exact source and APK hashes,
sanitized device/build facts, selected UI hierarchies, screenshots, instrumentation output, and filtered logcat.
These raw artifacts can contain device identifiers or status-bar details. Keep them local; promote only reviewed,
sanitized findings to `docs/evidence/M0-device-matrix.md`.

Outcome meanings:

- `PASS`: the named automated assertion passed on the named device/build/SHA;
- `FAIL`: a required assertion or state restoration failed;
- `PARTIAL`: useful evidence exists, but the complete contract was not automated or proven;
- `MANUAL_REQUIRED`: safe automation cannot make the required claim;
- `SKIPPED`: an earlier safety failure prevented the scenario;
- `UNSUPPORTED`: the device exposes no safe supported path for that scenario.

## Manual checks that remain

Automation cannot truthfully establish perceived audibility, physical or stuck-button behavior, automatic foreground
notifications driven by OEM controls, TalkBack/large-font/contrast usability, wired or Bluetooth routing, or long-run
battery behavior. Continuous/background protection has no selected runtime and must not be tested or claimed until the
feasibility ADR is superseded.
