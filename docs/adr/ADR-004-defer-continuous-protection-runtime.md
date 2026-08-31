# ADR-004 — Defer continuous-protection runtime selection

Status: Deferred pending physical-device evidence  
Date: 2026-08-31

## Context

M0 automation established deterministic protection rules and a foreground diagnostic observer, but no Android device
was connected. Therefore there is no valid evidence for background lifecycle reliability, foreground-service policy
suitability, OEM behavior, stop semantics, wakeups, or battery cost.

## Decision

Do not select or ship a continuous-protection runtime yet.

- The current `ContentObserver` is registered only while `MainActivity` is started. It improves diagnostic refresh but
  is not continuous protection.
- No foreground service, periodic worker, alarm, accessibility service, or polling loop is implemented.
- The UI reports runtime `STOPPED`; a preference cannot imply `ACTIVE`.
- Quick Settings is deferred because there is no truthful active runtime for a tile to represent.

ADR-003 remains the governing feasibility gate. This ADR must be superseded only after representative physical-device
evidence measures event reliability, process death, user-visible lifecycle, immediate stop behavior, battery/wakeups,
and OEM restrictions.

## Consequences

- M0 remains partial rather than falsely complete.
- Diagnose/readback work can proceed independently.
- Continuous protection, automatic restart, and Quick Settings remain unavailable in product copy and UI.
- A later accepted ADR must select a mechanism or explicitly narrow/remove the protection promise based on evidence.

