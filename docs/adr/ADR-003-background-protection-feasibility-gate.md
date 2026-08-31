# ADR-003 — Background protection requires M0 feasibility proof

Status: Accepted
Date: 2026-08-31

## Context

Continuous protection is central to the long-term product promise but Android 12+ background restrictions, Android 15/16 behavior, foreground-service policy, OEM battery management, and audio-setting races make this the highest technical-risk area.

Assuming a permanent `Service` or polling loop works would create false product claims and likely battery/policy problems.

## Decision

Do not freeze the runtime implementation before M0.

M0 must compare viable approaches, including event/callback-driven observation and foreground-service options only when justified, and must validate them on real devices/OEMs.

The chosen mechanism must satisfy:
- explicit user opt-in;
- user-visible runtime truth;
- stop/disable semantics;
- acceptable battery/wakeup cost;
- process-death recovery;
- policy-compatible foreground behavior if used;
- no infinite polling loop;
- testable failure/degraded state.

## Consequences

- UI/product copy cannot claim continuous protection until evidence exists.
- M0 is a release-blocking architecture milestone.
- Product scope may be narrowed if OEM/platform evidence shows some guarantees are not defensible.
- A follow-up ADR must record the actual chosen mechanism after M0.
