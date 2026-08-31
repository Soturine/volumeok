# Codex / Human Protocol — M0 Physical Android Validation

Repository: `Soturine/volumeok`
Baseline automated SHA: `e79b558a93ac11ece2bb4d098f5407a0cce5cfcb`
Issue: `#1 — M0 — Prove Android sound-readiness and protection feasibility`

## Goal

Close the physical-evidence gap left by the automated M0 run. Validate VolumeOK on real Android hardware without changing product claims beyond what is actually measured.

Do not repeat expensive automated suites unless code changes or blast radius justify it. The automated baseline already passed unit tests, Spotless, detekt, Android Lint, assembleDebug, instrumented-test compilation, Android CI and CodeQL on the baseline SHA.

## Before testing

1. Ensure the local checkout is synchronized:

```powershell
git pull --ff-only
git status
git rev-parse HEAD
git rev-parse origin/main
```

2. Record the device identity through ADB:

```powershell
adb devices -l
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.display.id
```

3. Build the exact checkout if needed:

```powershell
.\gradlew.bat assembleDebug
```

4. Record the APK hash:

```powershell
Get-FileHash ".\app\build\outputs\apk\debug\app-debug.apk" -Algorithm SHA256
```

The previously reported baseline debug APK hash is:

`6634514708D66BB78683C700FC4BFEB794AD43C57E0692BBFCEEFEB91BC7B6F8`

A mismatch is not automatically a defect because build reproducibility may depend on artifact metadata/tooling. Record it; do not silently treat different bytes as the same validated artifact.

5. Install:

```powershell
adb install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

## Test sequence per device

Record all results in `docs/evidence/M0-device-matrix.md` using only actually observed evidence.

### A. Baseline read

With normal sound enabled and DND off:

- compare VolumeOK ringtone current/max with Android Settings;
- compare ringer mode;
- compare DND state;
- record output-device wording;
- verify unavailable/ambiguous evidence does not become `READY`.

### B. Ringer-state transitions

Test separately:

1. normal sound;
2. ringtone volume at zero/lowest practical value;
3. vibrate mode;
4. silent mode;
5. DND enabled;
6. DND disabled again.

For each case record expected Android state, VolumeOK state, latency, mismatches and whether manual refresh is required.

### C. Controlled write → readback → restore

Use the app's controlled one-step volume test.

Record:

- original volume;
- requested target;
- fresh readback after write;
- restoration request;
- fresh readback after restoration;
- final status.

Stop testing that capability if restoration cannot be verified. Never force platform maximum.

### D. Foreground observation experiment

While the app is visible, manually change:

- ringtone volume;
- ringer mode;
- DND.

Record:

- whether an observer event arrives;
- approximate latency;
- duplicate callbacks;
- missed changes;
- whether manual Refresh returns the correct state.

Do not infer that this proves background protection.

### E. Audio-device evidence

When practical, test built-in output, wired device and Bluetooth device presence.

The app may state that a device is present only when supported by evidence. Do not convert presence into a claim that ringtone audio is definitely routed to that device.

### F. Process death / stale-state truth

Run:

```powershell
adb shell am force-stop com.soturine.volumeok
```

Reopen the app.

Verify:

- protection runtime is not falsely shown as `ACTIVE`;
- fresh platform readings are obtained;
- stale UI state is not presented as current truth.

### G. Instrumented tests

With a physical device connected, run the relevant instrumented suite once for the tested SHA:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Do not repeatedly rerun it when unchanged and green. If it fails, diagnose the real cause before retrying.

## Evidence capture

For each device record:

- date/time;
- manufacturer/model;
- Android version/API;
- OEM skin/build;
- exact source SHA;
- APK SHA-256;
- baseline read result;
- write/readback/restore result;
- DND result;
- foreground-observer result;
- process-death result;
- instrumented-test result;
- notable OEM restrictions;
- sanitized screenshots/logs if useful;
- status: `proven`, `partial`, `unsupported`, or `not tested`.

Do not commit personal notification contents, phone numbers, account names, Bluetooth device names that identify people, or other unnecessary personal information.

## OEM order

Preferred validation sequence:

1. first physical device available;
2. Samsung / One UI;
3. Motorola;
4. Pixel/AOSP-like if available;
5. Xiaomi/HyperOS;
6. OPPO/Realme.

The project does not need all OEMs to continue diagnosis UX work, but the continuous-protection runtime must not be frozen from a single-device result.

## What not to validate yet

Do not manufacture a background-protection test because no runtime has been selected.

Currently deferred:

- continuous/background protection;
- foreground-service strategy;
- battery/wakeup measurements for such runtime;
- Quick Settings active-protection tile;
- automatic restart;
- DND correction.

Those begin only after current hardware evidence justifies selecting a runtime candidate.

## After each device

If no code change is needed:

- update only the evidence/current-state docs;
- make one logical documentation commit;
- push to `main`;
- do not rerun the entire local suite solely for documentation changes unless repository gates require it;
- let remote CI run asynchronously.

If a defect is found:

1. reproduce;
2. classify expected vs actual behavior;
3. create a regression test where practical;
4. implement the smallest correct fix;
5. run focused tests;
6. logical commit;
7. push `main`;
8. continue independent evidence work while CI runs when safe;
9. checkpoint CI on the new SHA.

## Gate to supersede ADR-004

Only propose a continuous-protection runtime after evidence answers at least:

- are relevant setting changes observable reliably on representative devices?
- what happens when the activity backgrounds?
- what happens on process death/force-stop/reboot?
- what user-visible execution model would be required?
- can it stop immediately when the user disables protection?
- what OEM battery restrictions materially affect it?
- can the UI truthfully know `ACTIVE` vs `DEGRADED` vs `STOPPED`?

Then create a new ADR that either:

1. selects a runtime mechanism with evidence and consequences; or
2. narrows/removes the continuous-protection promise if Android/OEM constraints make it unreliable or policy-hostile.

## M0 completion gate

Do not tag/release merely because one phone works.

M0 can be considered complete when the team has enough representative physical evidence to make the core feasibility decision honestly, the device matrix is updated, any discovered critical defects are resolved, the runtime decision is recorded or explicitly scoped out, and the final exact SHA has the required remote gates green.

Only then may `v0.0.1-m0` / `VolumeOK v0.0.1-m0 — Feasibility Baseline` be created.
