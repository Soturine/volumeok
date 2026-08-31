# ADR-002 — Local-first MVP with no backend

Status: Accepted
Date: 2026-08-31

## Context

The core VolumeOK value is device-local: observe Android sound state, explain readiness, safely correct supported settings, and optionally protect ringtone volume. Accounts/cloud are not required to prove this value.

## Decision

MVP is local-first:
- no user account;
- no backend/API;
- no cloud sync;
- no behavioral ads;
- no product requirement for Internet permission unless a later feature justifies it;
- DataStore for local policy/preferences/runtime metadata.

## Consequences

Positive:
- smaller attack surface;
- simpler privacy story;
- lower operating cost;
- offline operation;
- fewer permissions and failure modes.

Negative:
- no remote family monitoring/configuration;
- no cloud backup/sync;
- limited centralized product analytics.

Any backend/family remote feature requires a new ADR, threat model, authentication/authorization design, lifecycle/retention rules, and cost/operations plan.
