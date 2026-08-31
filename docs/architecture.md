# Architecture

## Architectural style

VolumeOK starts as an Android **modular monolith** with pragmatic Clean Architecture / Ports & Adapters boundaries around Android platform APIs.

The intent is not architectural ceremony. The intent is to make the sound-readiness and protection rules testable without a device, while isolating unstable OEM/platform behavior behind adapters.

## Layers

```text
Compose UI
  ↓ events / ↑ immutable UI state
ViewModels
  ↓
Application use cases
  ↓
Domain model + rules
  ↓ ports
Android/platform adapters
  ↓
AudioManager / NotificationManager / background APIs / DataStore
```

### Presentation
Responsibilities:
- render state;
- collect user intent;
- accessibility semantics;
- navigation;
- localization;
- adaptive layouts.

Must not:
- contain readiness rules;
- call `AudioManager` directly;
- decide whether protection should restore;
- infer success from button clicks.

### Application
Use cases orchestrate domain + ports, for example:
- `ReadSoundSnapshot`
- `EvaluateReadiness`
- `CorrectIssue`
- `RunSafeSoundTest`
- `EnableProtection`
- `PauseProtection`
- `ResumeProtection`
- `HandleObservedVolumeChange`

### Domain
Pure Kotlin where practical.

Owns:
- models;
- invariants;
- readiness evaluation;
- protection policy;
- state transitions;
- circuit breaker;
- failure taxonomy.

Must not depend on Compose, Android Context, AudioManager, NotificationManager, Service, Hilt, Room, or DataStore.

### Platform/adapters
Owns Android-specific behavior and capability detection.

Candidate ports:

```kotlin
interface SoundStateReader
interface SoundStateController
interface DndStateReader
interface AudioRouteReader
interface ProtectionRuntime
interface PolicyStore
interface Clock
interface DiagnosticEventSink
```

Adapters must translate Android/OEM results into explicit domain evidence rather than leaking raw framework state everywhere.

## Proposed source structure

Start with cohesive packages in a small number of Gradle modules. Split Gradle modules only when build boundaries, ownership, test isolation, or dependency direction justify the cost.

```text
app/
  VolumeOkApplication
  MainActivity
  navigation/
  di/

core/
  model/
  domain/
  application/
  common-ui/

feature/
  home/
  diagnosis/
  safe-test/
  protection/
  settings/

platform/
  audio/
  dnd/
  routing/
  background/
  persistence/

benchmark/

test-support/
```

Possible later Gradle module split:

```text
:app
:core:model
:core:domain
:core:ui
:feature:home
:feature:diagnosis
:feature:protection
:platform:android-audio
:platform:persistence
:benchmark
```

Do not create this split on day one merely to look sophisticated.

## State management

Presentation uses **MVVM + unidirectional data flow**.

```text
UserEvent → ViewModel → UseCase → Domain/Port → Result → StateFlow → UI
```

UI state should be immutable and screen-specific.

Classify state:
- platform/server-equivalent state: sound snapshot/capabilities;
- persisted preference state: protection policy;
- transient UI state: dialog/sheet/progress;
- navigation state;
- runtime protection state.

Do not put all of them into one global mutable singleton.

## Persistence

MVP default: **DataStore** for user policy/preferences and persisted override/runtime metadata that genuinely needs process-death recovery.

Room is deferred until structured history/timeline requirements justify:
- queries;
- retention;
- migrations;
- indexes;
- data lifecycle complexity.

## Background execution

Background protection is not assumed to be solved.

M0 must test viable Android 16-compliant mechanisms and OEM behavior. If a foreground service is required, it must be:
- user-visible;
- tied to a core user-enabled feature;
- declared with the correct service type/policy rationale;
- stoppable;
- battery-conscious;
- tested under process death and OEM restrictions.

No `while(true)` polling loop.

Prefer event/callback/platform-driven observation where Android provides reliable signals; use periodic reconciliation only if justified by evidence.

## Capability model

Different Android/OEM versions may not support identical observation/correction behavior. Model capability explicitly.

Example:

```text
Capability
- READ_RINGER_MODE: SUPPORTED | UNSUPPORTED | PERMISSION_REQUIRED | UNKNOWN
- READ_RING_VOLUME: ...
- WRITE_RING_VOLUME: ...
- READ_DND: ...
- OBSERVE_CHANGES: ...
- CONTINUOUS_PROTECTION: ...
```

This prevents false claims and allows UI to degrade safely.

## Dependency rules

Allowed direction:

```text
app → feature → application/domain
platform → domain contracts
feature → core UI/domain
```

Forbidden:
- domain → Android framework;
- domain → feature UI;
- platform adapter → Compose screen;
- one feature reaching into another feature's internals;
- `utils` as a cross-project dumping ground.

## Composition root

`Application`/DI setup wires concrete Android adapters to domain ports. Entry points remain small.

## Error handling

Platform errors are translated into domain failures. Broad catches must either:
- add context and map to an explicit failure; or
- rethrow.

No silent exception swallowing.

## ADR triggers

Create/update an ADR for changes involving:
- minSdk/targetSdk strategy;
- background protection mechanism;
- persistence choice;
- new sensitive permission;
- backend/cloud introduction;
- contact/call-log features;
- AI/LLM introduction;
- proprietary/open-source license change;
- major third-party runtime dependency;
- Gradle module split with lasting coupling consequences.
