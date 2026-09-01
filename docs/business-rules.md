# Business Rules

## Domain vocabulary

- **SoundSnapshot** — evidence captured from Android about sound-related state at a point in time.
- **Readiness** — VolumeOK's deterministic evaluation of whether the phone appears ready to ring based on available evidence.
- **Issue** — an explainable condition that may prevent or reduce audible ringing.
- **ProtectionPolicy** — user-defined bounds and behavior for accidental-volume protection.
- **ProtectionState** — actual runtime state of the protection mechanism.
- **ProtectionOverride** — intentional temporary suspension of protection.
- **CircuitBreaker** — safeguard that stops automatic restoration when repeated oscillation suggests a stuck button, competing app, or unstable platform behavior.
- **Capability** — whether the OS/OEM/API lets VolumeOK observe or change a particular state reliably.

## Core models

```text
SoundSnapshot
- capturedAt
- ringerMode
- ringVolume
- ringVolumeMax
- notificationVolume?
- alarmVolume?
- dndState
- vibrationState?
- audioRouteEvidence
- capabilities[]
```

```text
ReadinessResult
- status: READY | ATTENTION | ACTION_REQUIRED | UNKNOWN
- issues[]
- recommendations[]
- evaluatedAt
- evidenceVersion
```

```text
ProtectionPolicy
- enabled
- minRingVolume
- maxRingVolume
- restoreMode: NOTIFY_ONLY | AUTO_RESTORE
- temporaryOverride?
```

```text
ProtectionRuntimeState
- STOPPED
- STARTING
- ACTIVE
- PAUSED_BY_USER
- SUSPENDED_BY_CIRCUIT_BREAKER
- DEGRADED
- ERROR
```

## Readiness rules

1. `READY` requires sufficient evidence for every capability considered mandatory for the current product version.
2. Missing permission, unsupported API, OEM ambiguity, or failed read must not become `READY`; use `UNKNOWN` or degraded issue(s).
3. Silent ringer mode is at least `ACTION_REQUIRED` unless the user has explicitly entered a temporary intentional-silence flow that the UI communicates as such.
4. Until a user-configured threshold is introduced, ringtone `0` is `ACTION_REQUIRED`, the lowest non-zero platform
   step (`1`) is `ATTENTION`, and `2+` has no volume-level issue. This ordinal rule avoids pretending OEM percentages
   are acoustically equivalent; ADR-005 records the decision.
5. DND cannot be inferred only from ring volume.
6. Audio-route claims must reflect evidence quality. “Bluetooth connected” and “ring audio is definitely routed to Bluetooth” are different claims.
7. Readiness is re-evaluated after every user-triggered correction; success is based on a fresh snapshot, not only on setter completion.
8. `READY` describes current public sound-setting evidence. It does not prove human audibility, call delivery, carrier
   behavior, or active routing.

## Protection invariants

1. Protection is opt-in.
2. `minRingVolume <= maxRingVolume <= platformMax`.
3. Auto-restore may not raise volume beyond `maxRingVolume`.
4. Protection must never claim `ACTIVE` unless the underlying runtime mechanism is actually active.
5. User pause has priority over automatic restoration until expiry/cancellation.
6. Protection failure is visible to the user and logged locally in redacted diagnostic form.
7. A repeated oscillation pattern opens the circuit breaker rather than causing an endless restore loop.
8. Circuit-breaker thresholds are explicit constants/configuration with tests, not magic numbers scattered through code.
9. Disabling protection returns control to normal Android behavior immediately.

## Safe test invariants

1. Sound test starts only after explicit user action.
2. Test never forces system maximum volume.
3. Progression, if used, must be bounded and cancelable.
4. User can stop the test immediately.
5. Successful playback does not prove telephony/carrier behavior; wording must be limited to what was tested.
6. Test completion is based on user confirmation and/or verified local playback state, not a fake timeout-only success.
7. The M3 local-tone test does not mutate the system ringtone volume; its generated-tone gain progresses only through
   platform-derived steps below maximum, so there is no temporary system volume to restore.

## Temporary override rules

- Overrides have a reason and start time.
- Time-bounded overrides have an expiry.
- Expiry restores protection only if the user has not disabled protection globally.
- Restart/process death must not silently lose a persisted active override or misreport it.
- Manual resume clears the override.

## Circuit breaker

### Goal
Protect the user and device from unstable behavior.

### Trigger examples
- repeated volume down → auto-restore → volume down cycles in a short window;
- repeated setter/readback mismatch;
- competing system/app continuously overrides state.

### Behavior

```text
ACTIVE
  ↓ repeated instability
SUSPENDED_BY_CIRCUIT_BREAKER
  ↓ user acknowledgement/recovery
STARTING
  ↓ verified
ACTIVE
```

No auto-reset loop. Recovery must require a stable period and/or explicit user action according to the final M0 evidence.

## Failure taxonomy

Use domain errors, not generic “Something went wrong”.

- `CAPABILITY_UNAVAILABLE`
- `PERMISSION_REQUIRED`
- `READ_FAILED`
- `WRITE_NOT_ALLOWED`
- `WRITE_FAILED`
- `WRITE_NOT_EFFECTIVE`
- `DND_STATE_UNKNOWN`
- `AUDIO_ROUTE_UNKNOWN`
- `BACKGROUND_MECHANISM_STOPPED`
- `PROTECTION_DEGRADED`
- `OSCILLATION_DETECTED`
- `UNSUPPORTED_OEM_BEHAVIOR`
- `TEST_PLAYBACK_FAILED`
- `PERSISTENCE_FAILED`

Each failure maps to:
- user-facing localized explanation;
- safe recovery action when available;
- internal redacted diagnostic code;
- test coverage proportional to risk.

## Business rules that must remain deterministic

- readiness evaluation;
- permission/capability interpretation;
- protection bounds;
- automatic restore decision;
- circuit breaker;
- persistence of policy/override;
- whether UI may claim `READY` or `ACTIVE`.

No LLM may decide these rules.
