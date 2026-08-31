# Testing / QA Strategy

## Principle

Tests must prove behavior, not increase a counter. Coverage is a signal, not completion evidence.

The primary risks are platform/OEM inconsistency, false-green readiness, incorrect automatic restoration, process death, and misleading UI claims.

## Test pyramid by risk

### Unit
Focus on pure Kotlin domain behavior:
- readiness evaluation;
- protection bounds;
- override expiry;
- circuit breaker;
- state transitions;
- error mapping;
- localization-independent status selection.

### Component/Application
Use real use cases with fake platform ports:
- read snapshot → evaluate → correct → re-read;
- enable protection → runtime start → verified active state;
- repeated oscillation → breaker open;
- permission/capability missing → degraded/unknown.

### Instrumented Android
Prove framework integration:
- AudioManager reads/writes where supported;
- DND capability semantics;
- DataStore persistence;
- Quick Settings tile behavior if included;
- service/background lifecycle if used;
- process death/restart.

### Compose UI
Critical journeys:
- READY state rendering;
- issue explanation and primary action;
- correction feedback based on actual re-read result;
- safe test start/stop;
- protection pause/resume;
- circuit-breaker explanation;
- accessibility semantics.

### End-to-end / device
Few high-value real-device scenarios, especially OEM-specific behavior.

## Critical User Journey gates

1. Open → readiness shown from real evidence.
2. Force low ringtone → issue appears → correction → state re-read → READY/remaining issue.
3. Enable protection → verify runtime genuinely active.
4. Drop below protected minimum → expected notify/restore behavior.
5. Intentional pause → no auto-restore during pause → correct resume.
6. Simulate rapid repeated changes → circuit breaker opens.
7. Kill process/reboot where relevant → persisted policy and reported runtime state remain truthful.

## OEM/device matrix

M0/M6 should include representative devices/OS combinations, prioritizing actual reachable hardware and Play pre-launch reports:

- Google Pixel / AOSP-like behavior;
- Samsung / One UI;
- Motorola;
- Xiaomi / HyperOS;
- OPPO/Realme where feasible.

Track:
- Android API level;
- OEM skin/version;
- device model;
- capability outcomes;
- background restrictions;
- known deviations;
- date validated.

Do not generalize one device result to all Android devices.

## Property-based tests

Useful invariants:
- `0 <= min <= max <= platformMax`;
- auto-restore target never exceeds policy max;
- disabled/paused protection never emits automatic restore action;
- UNKNOWN capability cannot produce false READY through missing data defaults.

## Mutation / negative controls

For critical rules, deliberately mutate/break conditions and verify tests turn red, especially:
- readiness missing-capability handling;
- protection bounds;
- circuit-breaker threshold/state transition;
- UI active-protection claim.

Mutation testing may be scoped to core domain rather than the entire Android app.

## Flakiness policy

Flaky test = defect.

Do not fix with arbitrary sleeps or retry-until-green. Prefer:
- state-based synchronization;
- fake/deterministic clocks;
- controlled dispatchers;
- isolated DataStore/test dirs;
- explicit timeouts;
- idling/synchronization APIs for UI/instrumentation.

## Accessibility QA

Test at minimum:
- TalkBack semantics and order;
- 200% font scale;
- high-contrast scenarios;
- reduced motion;
- keyboard/d-pad where applicable;
- orientation/multi-window;
- 48dp+ targets;
- no color-only status communication.

## Internationalization QA

- English, PT-BR, Spanish smoke passes;
- pseudolocale expansion;
- no hardcoded user-visible strings;
- no sentence concatenation;
- percentage/time/number formatting localized.

## Performance QA

Measure before setting hard budgets:
- cold/warm startup;
- jank in home/diagnosis/protection flows;
- CPU/battery cost of protection monitoring;
- wakeups;
- memory;
- time from platform change to reflected UI/protection reaction.

Use Macrobenchmark/Baseline Profiles only after actual critical paths exist.

## Security/safety QA

Test:
- denied permissions;
- revoked permission while running;
- unsupported API/OEM paths;
- malicious/invalid persisted preference values;
- crash/restart during correction/protection transition;
- repeated hardware-change storm;
- no sensitive values in logs;
- app stops changing system state immediately after protection disabled.

## Release qualification

A release is not qualified by “tests passed” alone. Required evidence includes:
- exact SHA;
- required CI/security checks green;
- relevant real-device matrix status;
- no unresolved High/Critical security issue;
- critical journeys manually/automatically validated;
- release notes match actual behavior;
- generated AAB is the artifact that was validated.
