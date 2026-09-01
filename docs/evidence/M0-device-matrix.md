# M0 Device Evidence Matrix

Evidence date: 2026-09-01

One Motorola device has exact-SHA physical evidence. All other rows remain a validation queue, not compatibility
claims. A result from one device must not be generalized to its OEM family or Android as a whole.

| Manufacturer / model | Android / API | Build / skin | Ringtone read | Controlled write + readback + restore | DND read / capability | Foreground observation | Background protection / battery | Process death | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Google Pixel / reachable AOSP-like device | Record on test | Record on test | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| Samsung / reachable Galaxy device | Record on test | Record One UI version | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| Motorola Edge 30 Fusion | Android 14 / API 34 | `U1SJS34.2-92-10-9` | PASS: public API returned valid `1/7` | PASS: `1 -> 2 -> 1`, each state freshly read | PASS read; priority DND rendered non-ready after explicit refresh and was restored | PARTIAL: safe ADB ring write unavailable; ADB DND required explicit refresh | Deferred; no runtime candidate | PASS: force-stop/reopen remained `STOPPED` | partial |
| Xiaomi / reachable device | Record on test | Record HyperOS version | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |
| OPPO or Realme / reachable device | Record on test | Record build | Not tested | Not tested | Not tested | Not tested | Not tested | Not tested | not tested |

## Motorola evidence record

- Date: 2026-09-01.
- Source SHA: `f613c7e39b8087ab0f9662c6b3ead643cb941aca`, synchronized with `origin/main` during the run.
- Debug APK SHA-256: `068CECCCCA8656EC1096684688F9BF8F9E18A24BAC7C409D5C616DC56F260136`.
- Automated PASS: device/source preflight, baseline public-API snapshot, controlled write/readback/restoration, truthful
  `STOPPED` runtime after force-stop/reopen, and the connected instrumented suite.
- Automated PARTIAL: ADB could set and restore priority DND, and the app showed `ACTION_REQUIRED` after explicit
  refresh. This ADB path did not prove automatic foreground notification. The OEM build accepted the shell ringtone
  command but did not change `STREAM_RING`, so physical volume-button observation remains manual.
- Product-review warning: `1/7`, normal ringer mode, and DND off rendered `READY`. M0B records this result without
  silently changing the business threshold.
- Scope boundary: no sound audibility, physical-button behavior, accessibility, Bluetooth/wired route, background
  runtime, endurance, battery, or cross-OEM claim was established.

Raw run artifacts remain local and gitignored because they include device identifiers and unsanitized screenshots.

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
