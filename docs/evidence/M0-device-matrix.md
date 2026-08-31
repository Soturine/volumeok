# M0 Device Evidence Matrix

Evidence date: 2026-08-31

No Android device was connected to ADB in the execution environment. The rows below are intentionally `not tested`;
they are a validation queue, not compatibility claims.

| Manufacturer / model | Android / API | Build / skin | Ringtone read | Controlled write + readback + restore | DND read / capability | Foreground observation | Background protection / battery | Process death | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Google Pixel / reachable AOSP-like device | Record on test | Record on test | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| Samsung / reachable Galaxy device | Record on test | Record One UI version | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| Motorola / reachable device | Record on test | Record build | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| Xiaomi / reachable device | Record on test | Record HyperOS version | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| OPPO or Realme / reachable device | Record on test | Record build | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |

## Manual evidence protocol

For every device, record date, exact model, Android/API, build number, OEM skin, and battery-management settings.

1. Install the debug APK built from the exact SHA under test and open VolumeOK.
2. Record the displayed ringtone value/max, ringer mode, DND state, and output-device wording. Compare each with system
   settings; unavailable or ambiguous evidence must remain non-green.
3. Tap **Controlled one-step test**. Record original value, requested value, fresh readback, restored value, and final
   readback. Stop and record `unsupported` if restoration cannot be verified.
4. Change volume, ringer mode, and DND while the app is visible. Record which changes trigger the foreground observer,
   latency, duplicates, and missed events. Manual Refresh is the negative control.
5. Background and force-stop the app. Confirm the UI never reports protection `ACTIVE` after restart.
6. Do not test background protection or battery impact until a candidate runtime is approved in an ADR. When approved,
   measure wakeups and battery using the same duration/workload and record stop semantics and OEM restrictions.
7. Attach sanitized logs or screenshots that contain no personal phone data, and commit only evidence actually observed.

The controlled test never requests platform maximum. It makes one safe step where possible and attempts immediate
restoration without playing audio.

