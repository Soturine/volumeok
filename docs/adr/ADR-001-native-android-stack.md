# ADR-001 — Native Android stack

Status: Accepted
Date: 2026-08-31

## Context

VolumeOK is fundamentally tied to Android audio state, DND, Quick Settings, lifecycle, background execution, accessibility, and OEM behavior. The product does not currently need cross-platform UI reuse.

## Decision

Use:
- Kotlin;
- Jetpack Compose + Material 3;
- Material 3 Adaptive;
- MVVM + UDF in presentation;
- Coroutines/StateFlow;
- Hilt;
- DataStore initially;
- Gradle Kotlin DSL;
- targetSdk 36 from first scaffold;
- proposed minSdk 26 pending reach validation.

## Alternatives considered

### Flutter / React Native
Rejected for MVP because the hardest parts are Android-platform integration rather than reusable cross-platform UI.

### XML Views
Viable but not preferred for a new app; Compose better matches current Android guidance and the adaptive/accessibility plan.

## Consequences

Positive:
- first-class Android APIs/lifecycle;
- fewer bridges around platform behavior;
- modern Compose test/adaptive ecosystem.

Negative:
- no iOS implementation reuse;
- Android expertise required;
- OEM-specific behavior still needs real-device validation.
