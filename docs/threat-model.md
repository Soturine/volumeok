# Threat Model

## Scope

VolumeOK is a local-first Android app that observes and, after explicit opt-in, may modify sound-related settings. The primary security/safety risk is not financial theft; it is **misconfiguration, misleading readiness claims, excessive permissions, privacy leakage, or automation that fights the user/device**.

## Assets

- user trust that status reflects reality;
- protection policy and temporary overrides;
- device sound settings;
- diagnostic state/events;
- app signing/release integrity;
- future family/contact data if those features are ever added.

## Trust boundaries

- Compose/UI ↔ application/domain;
- domain ↔ Android adapters;
- app ↔ Android system/OEM services;
- app ↔ persistence;
- build/release ↔ third-party dependencies/GitHub Actions/Play Console.

## Threats and controls

### False green readiness
**Risk:** app says “Everything OK” while required state is unknown or unverifiable.

Controls:
- explicit capability model;
- UNKNOWN/degraded state;
- post-correction re-read;
- OEM validation;
- tests that fail if missing evidence defaults to READY.

### Unsafe automatic volume changes
**Risk:** protection raises sound unexpectedly or repeatedly.

Controls:
- explicit opt-in;
- user-defined min/max;
- never exceed configured max/platform max;
- pause/disable;
- circuit breaker;
- no forced maximum-volume test;
- visible protection state.

### Infinite tug-of-war
**Risk:** stuck hardware button or competing app causes endless restore loop.

Controls:
- oscillation detection;
- bounded event window;
- circuit breaker;
- user-facing recovery;
- no auto-reset loop without stability/acknowledgement.

### Permission overreach
**Risk:** app asks for contacts, call logs, accessibility, microphone, location, etc. without necessity.

Controls:
- MVP permission budget;
- no contact/call-log/location/camera/microphone requirement in core MVP;
- no AccessibilityService hacks;
- permission added only via ADR + threat/privacy review.

### Background execution abuse
**Risk:** battery drain, policy violation, misleading “active” state.

Controls:
- M0 feasibility gate;
- prefer event-driven observation;
- foreground service only if justified and policy-compatible;
- visible user control;
- reconcile runtime truth after process/service death;
- battery/performance measurement.

### Sensitive logging
**Risk:** diagnostics expose user data.

Controls:
- structured redacted event codes;
- no contact names/phone numbers in MVP logs;
- no secrets/tokens in code/docs/logs;
- local logs bounded by retention/size if implemented.

### Supply-chain compromise
Controls:
- minimal dependencies;
- Gradle dependency verification;
- lock/version catalog strategy;
- GitHub dependency review/Dependabot/CodeQL as applicable;
- Actions pinned by full SHA when practical;
- least-privilege workflow permissions;
- SBOM/provenance for release artifacts.

### Malformed persisted state
**Risk:** corrupted/out-of-range settings produce unsafe behavior.

Controls:
- validate on read;
- clamp/reject against platform max and policy invariants;
- versioned persistence schema;
- safe defaults that do not silently enable automation.

## Privacy baseline

MVP goals:
- no account;
- no cloud backend;
- no behavioral advertising;
- no analytics SDK required for core operation;
- ideally no `INTERNET` permission in the core app unless a concrete feature requires it;
- Android Vitals/Play diagnostics used proportionally for release quality;
- policy/preferences stored locally.

## Safety principles

- never claim “guaranteed to never miss a call”;
- never force 100% volume;
- never use fear-based copy;
- never hide degraded capability;
- automatic actions are bounded, reversible, and explainable;
- protection disabled means no continued state manipulation.

## Future high-risk features

The following require a new threat model/ADR before implementation:
- contacts/VIP callers;
- call screening/telecom roles;
- remote family monitoring;
- cloud accounts/sync;
- remote configuration;
- accessibility service;
- LLM/cloud AI;
- notification-listener access;
- persistent telemetry linked to a user/device identity.
