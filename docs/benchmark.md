# Market / Competitor Benchmark

Snapshot date: **2026-08-31**.

This document is a product benchmark, not a claim that every competitor feature works perfectly on every Android/OEM version.

## Market pattern

The Play Store already has many apps that expose volume sliders, lock/restoration behavior, custom volume panels, schedules, and power-user audio profiles. The opportunity for VolumeOK is not “another volume controller”; it is a **sound-readiness assistant** that explains whether the phone is ready to ring and safely helps recover from accidental misconfiguration.

## Representative competitors

| Product | Public traction signal | Primary value | Opportunity for VolumeOK |
| --- | ---: | --- | --- |
| Volume Lock | 1M+ installs; ~10K reviews in current Play listing | min/max lock, restore behavior, profiles/PIN depending version | Reviews and product responses show restoration can oscillate when hardware/system keeps changing volume. VolumeOK should detect instability and open a circuit breaker instead of fighting forever. |
| Native Volume | 1M+ installs | fast/native-style access to volume controls | Access to sliders is not the same as readiness diagnosis. |
| Volume Control Panel | 100K+ installs | custom volume panel/UI | UI customization is outside VolumeOK's main job. |
| Audio Profiles | 100K+ installs | profiles, automation, scheduling | Powerful but power-user oriented; avoid rebuilding a full automation engine. |
| Sound Profile | 1M+ installs | profiles, schedules, DND integrations, richer automation | Confirms demand for sound-state automation but also demonstrates complexity that VolumeOK intentionally avoids in MVP. |
| Phone Doctor-style apps | large category | generic hardware/device diagnostics | Usually technical and broad. VolumeOK should be narrow, plain-language and outcome-oriented. |
| RingBreaker / priority-call niche apps | small/emerging | make selected callers more noticeable | Candidate post-MVP module, but requires separate telecom/permission/privacy validation. |

## Competitive lessons

### 1. Do not sell an impossible “absolute lock”
Android and OEM behavior can override or race with third-party apps. The product should say **protection** and report the actual runtime state, not promise impossible control.

### 2. Do not fight hardware forever
Repeated down → restore → down → restore can produce a bad experience and battery churn. VolumeOK's domain includes an explicit oscillation/circuit-breaker state.

### 3. Avoid audio ads
An app whose purpose is sound reliability should not surprise users with loud video/audio advertising. MVP direction: no behavioral advertising; consider one-time Pro purchase only after value is proven.

### 4. Diagnosis should be human-readable
Competitors often expose raw sliders/settings. VolumeOK should turn evidence into statements such as:

- “Your phone may not ring: ringtone volume is 5%.”
- “Do Not Disturb is active.”
- “VolumeOK cannot verify this setting on this device.”

### 5. Older adults should not receive an infantilized UI
Use excellent accessibility, not a caricatured “senior mode”.

## Positioning

```text
Generic volume app:
  change settings

VolumeOK:
  observe → explain → safely correct → verify → optionally protect
```

## Differentiators to defend

- deterministic readiness engine;
- evidence-aware `UNKNOWN` rather than false green status;
- post-correction re-read verification;
- safe progressive sound test;
- explicit temporary override;
- oscillation circuit breaker;
- local-first/no-account MVP;
- PT-BR/EN/ES from launch;
- accessibility and adaptive layouts from design time;
- OEM/device compatibility evidence rather than universal claims.

## Benchmark cautions

Install counts and listings change. Refresh this document before major product/marketing decisions. External evidence is catalogued in `research-sources.md`.
