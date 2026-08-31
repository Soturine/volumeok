# VolumeOK Engineering Constitution

This document translates the user's permanent engineering standard into project-specific rules. It is intentionally shorter than the global constitution and points agents toward executable gates and local docs rather than repeating a giant prompt.

## North star

Build software that can withstand serious technical review. Do not optimize for sophistication, velocity theater, or AI-generated volume. Optimize for correct rules, clear boundaries, safety, privacy, reliability, accessibility, maintainability, testability, reproducibility, and evidence.

## Business rules first

Before implementing a behavior, identify as relevant:

- actor and goal;
- state and transition;
- invariant;
- input/output;
- permission/capability;
- failure and unknown state;
- concurrency/lifecycle;
- acceptance criteria;
- non-functional risk.

Critical rules belong in domain/application logic and tests, not scattered through composables, Android services, repositories, and helpers.

## Architecture

Prefer the simplest architecture that preserves good boundaries.

- Android UI: MVVM + UDF.
- Application/domain: pragmatic Clean / Ports & Adapters.
- Android framework APIs: adapters.
- One app/modular monolith before unnecessary modules/services.
- No microservices/backend until a product requirement proves the need.
- No CQRS/event sourcing/queues/cache/AI merely to appear advanced.

Use ADRs for consequential choices.

## Clean code

Use domain language and explicit types/contracts. Avoid broad catches, silent failures, magic values, generic helpers, unclear boolean flags, `Manager`/`Service` dumping grounds, placeholders, fake implementations, dead code, and comments that restate code.

DRY is semantic, not textual: centralize one rule; do not merge different concepts because code happens to look similar.

## Data and state

Settings/policies need explicit ownership and lifecycle. Persist only what the product needs. For future history/event data, define identity, retention, deletion, provenance and migration before adding Room.

Unknown platform state is first-class; never collapse unsupported/permission-denied/error into READY.

## Concurrency and lifecycle

Assume the app can be backgrounded, killed, restarted, and have system settings changed concurrently by the user, OEM, Bluetooth accessory, another app, or hardware buttons.

Protection logic must be idempotent where possible, avoid check-then-act races when consequences matter, and use a circuit breaker for repeated oscillation.

## Security, privacy and safety

Secure-by-default, privacy-by-default, least privilege.

MVP should avoid sensitive permissions and remote data collection. Never store secrets in Git/logs/docs. Treat Android intents, persisted preferences and platform state as untrusted inputs that require validation.

For this product, safety includes avoiding sudden excessive loudness, misleading protection claims, battery-hostile loops, and automation that overrides deliberate user intent indefinitely.

## Resource budgets

Bound any loop or event stream driven by system/user input. Relevant budgets include:

- change events per rolling time window;
- automatic restore attempts;
- circuit-breaker cooldown;
- diagnostic/test duration;
- local event retention if history is added;
- log volume;
- background wakeups/work.

Numbers must come from measurement/UX needs, not arbitrary “senior-looking” constants.

## Testing

Tests must prove behavior.

Use many small domain tests, meaningful adapter/integration tests, and a small number of end-to-end critical journeys. Platform-specific claims require Android instrumentation/real-device evidence. Do not mock the exact behavior being proved.

For critical rules, consider property tests, negative controls, mutation testing, fault injection and regression tests.

Coverage is a signal, not completion proof.

## Performance and battery

Measure before optimizing. Track cold start, jank, CPU, wakeups, background duration and battery impact of protection. No polling loop as a default architecture.

## Accessibility and internationalization

Accessibility is part of Definition of Done. Support TalkBack semantics, large font scales, touch targets, contrast, keyboard/foldable/tablet behavior where applicable, reduced motion, and translated/localized strings.

English is fallback; PT-BR and Spanish ship as first-class locales. Do not concatenate localized strings manually.

## Git and asynchronous CI

Work in small coherent batches:

`focused test -> logical commit -> real remote push -> async CI`

Commit != push. Confirm the exact SHA is remote before release qualification. Continue independent work while CI runs and perform consolidated checkpoints instead of constant polling.

## Supply chain / DevSecOps

Every dependency needs justification for necessity, license, maintenance, security, compatibility and cost. Use dependency verification/locking as appropriate, least-privilege workflow permissions, dependency review, CodeQL/SAST where supported, secret scanning, SBOM and provenance for releases.

Dependabot informs; it does not auto-authorize merges.

## Release engineering

The published AAB must be the artifact actually validated. Preferred chain:

`exact source SHA -> checks/tests -> final artifact -> hash -> SBOM/provenance -> tag/release`

No unvalidated rebuild at release time.

## AI / LLM / AIOps

MVP readiness and protection are deterministic. LLMs do not decide whether a phone is safe/ready, modify volume, authorize actions, or validate invariants.

LLMOps is `N/A` until an LLM-backed product capability is deliberately introduced. If introduced later, it needs model/prompt versioning, structured outputs, eval datasets, hallucination/grounding checks, privacy review, cost/latency budgets and deterministic fallbacks.

AIOps may later help group opt-in diagnostics or detect regressions; it must not autonomously change user sound policies or production safety constraints.

## Verification versus validation

Verification asks whether we built the specified behavior correctly. Validation asks whether that behavior solves the user's real problem without unacceptable friction or harm. Both are required, proportionally to risk.

## Independent audit

At important milestones/releases, the implementing agent's “done” statement is not sufficient evidence. Review architecture, code, tests, security/privacy, docs, performance/battery and product claims independently.

## Definition of Done

A milestone is complete only when its explicit exit criteria are satisfied with evidence, remote source is synchronized, required gates are green for the exact SHA, docs match current behavior, and unresolved items are labeled honestly rather than hidden.
