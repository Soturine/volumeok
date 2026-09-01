# Codex Prompt — M0C Physical Qualification + M1→M3 Parallel Progression

Repository: `Soturine/volumeok`

Current known state:

- M0A automated feasibility: complete.
- M0B physical automation harness: complete.
- M0C physical qualification: partial; Motorola Edge 30 Fusion evidence exists, Samsung and Pixel/AOSP-like remain pending.
- ADR-004: continuous-protection runtime selection deferred pending representative physical evidence.
- M1 foundation is substantially implemented already during M0/M0B.

## Mission

Advance VolumeOK without wasting time waiting for additional OEM devices.

Run two coordinated tracks:

```text
TRACK A — M0C PHYSICAL QUALIFICATION
Motorola manual remainder
→ Samsung / One UI
→ Pixel / AOSP-like
→ evidence
→ runtime decision gate

TRACK B — PRODUCT PROGRESSION
M1 foundation audit/gaps
→ M2 Diagnose
→ M3 Safe Test
```

Track B may advance while Track A is incomplete **only where work does not depend on the unresolved continuous-protection runtime**.

M4 Protection remains gated by ADR-004 and representative physical evidence.

Before editing, read and obey:

1. `AGENTS.md`
2. `ENGINEERING.md`
3. `README.md`
4. `docs/current-state.md`
5. `docs/product.md`
6. `docs/business-rules.md`
7. `docs/architecture.md`
8. `docs/testing.md`
9. `docs/testing-m0b.md`
10. `docs/threat-model.md`
11. `docs/devops-devsecops.md`
12. `docs/roadmap.md`
13. `docs/evidence/M0-device-matrix.md`
14. `docs/adr/ADR-003-background-protection-feasibility-gate.md`
15. `docs/adr/ADR-004-defer-continuous-protection-runtime.md`

Do not reinterpret `partial` as `complete` merely because product work advances.

---

# Global workflow

Work directly on `main` unless repository policy changes.

Use small logical commits and push them throughout the work:

```text
focused implementation
→ focused test
→ logical commit
→ git push origin main
→ continue independent task while CI runs
→ checkpoint at milestone boundary
```

Do not sit idle waiting on CI.

Do not repeatedly run full suites. Use focused tests during implementation and one appropriate broader qualification near each milestone exit or release candidate.

Maintain:

```text
HEAD == origin/main
```

before claiming remote completion.

Do not use commit count, LOC, or test count as quality metrics.

---

# Track A — M0C Physical Qualification

Continue using:

```powershell
.\scripts\m0-device-validation.ps1 -Serial <adb-serial>
```

## A1 — Finish Motorola manual remainder

Using the existing Motorola Edge 30 Fusion, complete only the checks that still require human/device interaction:

- physical Normal → Vibrate → Silent transitions while VolumeOK is visible;
- observe foreground update latency and whether manual Refresh is required;
- DND changes through the OEM UI;
- Bluetooth presence behavior;
- wired output if hardware is available;
- audibility confirmation for the safe/controlled path when applicable;
- TalkBack smoke check;
- 200% font scale / large text check;
- contrast/readability check;
- physical force-stop/reopen truth confirmation;
- record that broken/stuck physical button remains manual-only unless actual hardware exists.

Do not run long battery/background tests because there is still no selected continuous-protection runtime to measure.

## A2 — Samsung / One UI

Repeat the same harness/protocol on one reachable Samsung device.

Priority evidence:

- ringtone read semantics;
- ringer-mode behavior;
- DND behavior;
- controlled write/readback/restore;
- foreground observer behavior;
- force-stop/reopen truth;
- OEM restrictions/dialogs;
- differences from Motorola.

## A3 — Pixel / AOSP-like

Use a physical Pixel if available. If not, an official remote/device-lab Pixel may provide automation evidence, but classify remote/device-lab evidence separately from local physical-human evidence.

Do not call emulator-only evidence equivalent to physical OEM validation.

## A4 — Runtime-decision gate

Do not supersede ADR-004 until evidence is sufficient to answer:

- can relevant changes be observed reliably enough on representative OEMs?
- what happens outside the activity lifecycle?
- what user-visible execution model would be required?
- what does process death / force-stop / reboot mean for runtime truth?
- can protection be stopped immediately and predictably?
- what OEM restrictions materially affect behavior?
- can the app truthfully distinguish `ACTIVE`, `DEGRADED`, and `STOPPED`?

Possible outcomes:

1. select a continuous-protection runtime;
2. select a narrower notify-only runtime;
3. retain protection as foreground-only;
4. remove/narrow the continuous-protection promise.

Evidence decides; roadmap intention does not.

---

# Track B — Product Progression

## M1 — Foundation Audit / Gap Closure

M1 should be treated as an **audit and gap-closure milestone**, not a rebuild.

Already likely present from M0/M0B:

- Android project scaffold;
- Kotlin/Compose/Material 3;
- target API 36;
- Coroutines/Flow where needed;
- domain contracts;
- unit/instrumented tests;
- Spotless/ktlint;
- detekt;
- Android Lint;
- CI/CodeQL/Dependabot;
- EN/PT-BR/ES resources;
- threat model/ADRs;
- MVVM/UDF home diagnostic spike.

Audit actual repository state before adding anything.

### M1 tasks

- identify roadmap items already complete and mark them accurately;
- verify package boundaries remain clean;
- decide whether Hilt is actually needed now; do not add DI ceremony solely because roadmap once listed it;
- add DataStore only if M2/M3 introduce real persistent settings/preferences;
- verify dependency governance/version catalog/verification as appropriate;
- resolve or document Gradle 10 deprecation warnings if they originate from project-controlled configuration and can be fixed safely without destabilizing current toolchain;
- verify resources contain no important hard-coded product strings;
- ensure current-state/docs reflect implementation truth;
- keep one-module architecture unless evidence justifies a module split.

### M1 exit

M1 is complete when:

- foundation gaps are either fixed or explicitly deferred;
- deterministic quality gates are green on the exact SHA;
- no unnecessary architecture rewrite was performed;
- docs accurately state which original M1 bullets were already satisfied during M0.

No special release is required solely for M1 unless repository policy later introduces one.

---

# M2 — Diagnose Productization

Goal: turn the M0 diagnostic spike into the first real user-facing VolumeOK experience.

The product question remains:

> Is this phone ready to ring?

Do not compete as another volume slider panel.

## M2 domain/product work

Refine the readiness engine and issue taxonomy around explainable evidence.

Required categories should distinguish at least:

- ringtone unavailable/unknown;
- ringtone too low;
- silent;
- vibrate-only;
- DND active;
- DND unknown;
- output-device presence/context;
- unsupported capability;
- write/correction ineffective;
- runtime protection unavailable/stopped.

### Important open product decision: low-volume readiness

Current physical evidence showed:

```text
1 / 7
Normal
DND off
→ READY
```

Do not silently accept or change this.

Create a tested, documented product decision for what `READY` means.

Evaluate alternatives such as:

```text
A. technically ring-capable
   any non-zero ringtone may be READY

B. reasonably audible readiness
   very low non-zero volume becomes ATTENTION

C. user-configured threshold
   READY depends on a personalized minimum
```

For MVP, favor deterministic and understandable behavior. Do not invent a percentage from aesthetics alone. Use product reasoning, physical evidence, accessibility goals, and future protection-policy compatibility.

Record the decision in an ADR or business-rule update if it affects core semantics.

## M2 UI

Replace the M0 engineering-looking diagnostic screen with a simple product surface while retaining an optional diagnostic detail view.

Primary screen target:

```text
VolumeOK

[ readiness card ]
✓ Tudo OK
Seu celular está pronto para tocar.

or

! Seu celular pode não tocar
Volume de toque muito baixo.

[ CORRIGIR ] when safe/supported

Checks
Toque              ...
Modo               ...
Não Perturbe       ...
Áudio/dispositivos ...
```

Do not expose implementation terminology such as `Reading.Available`, `STOPPED`, adapter names, or raw enum values to normal users.

Diagnostic/developer detail may remain available separately during development.

## M2 correction rules

One-tap corrections are allowed only where current Android APIs and evidence make them predictable.

For every correction:

```text
read
→ request change
→ fresh readback
→ determine effective/ineffective
→ update UI from actual state
```

Never assume setter success.

DND correction remains out unless current Android permission/API semantics and product consent flow are explicitly designed and validated.

## M2 accessibility/i18n

From implementation time, not as cleanup:

- PT-BR, EN and ES strings;
- TalkBack semantics;
- minimum touch targets;
- 200% font compatibility;
- no information conveyed by color alone;
- dynamic/adaptive layout;
- light/dark themes where current design system supports them;
- no infantilized senior-specific UI.

## M2 tests

Add focused tests for:

- readiness issue composition;
- low-volume semantic decision;
- correction readback success/failure;
- UNKNOWN cannot become green;
- DND + non-zero volume cannot become READY incorrectly;
- UI renders actionable issue copy from domain state;
- supported correction CTA appears only when appropriate;
- no unsupported correction CTA.

Use real-device checks for CUJ-1/CUJ-2 where required.

## M2 exit

M2 exits when:

- readiness semantics are explicitly decided;
- primary Diagnose UX is product-quality, not a debug panel;
- CUJ-1 status reading is proven;
- CUJ-2 at least one safe correction path is proven with post-correction readback;
- representative physical evidence exists on available device(s);
- exact SHA remote gates are green.

M0C may still be open for additional OEM qualification.

---

# M3 — Safe Test

M3 may start after the M2 readiness model is stable enough to consume its results. It does not depend on selecting the continuous-protection runtime.

Goal: give the user a safe answer to:

> Can I actually hear my phone?

## M3 behavior

Create a guided audible test that is:

- explicitly user initiated;
- progressive/bounded;
- immediately stoppable;
- never forces maximum volume;
- restores temporary state changes when applicable;
- clear about what it is testing;
- accessible.

Potential progressive sequence must be derived from platform volume steps/capabilities, not hard-coded percentages that exceed safe/user-defined bounds.

Example UX concept:

```text
Vamos testar seu toque.

O VolumeOK tocará um som curto em um nível seguro.

[ COMEÇAR ]

Did you hear it?
[ SIM ] [ NÃO ]
```

If the user does not hear it, progress only within bounded policy and with explicit explanation.

## M3 implementation principles

- generated/local test tone if possible; avoid microphone permission;
- no INTERNET dependency;
- no media asset/licensing risk when a generated tone is sufficient;
- stop on user request immediately;
- restore relevant volume state after test;
- fresh readback after restoration;
- handle interruptions/lifecycle changes safely;
- never leave the phone unexpectedly louder because the test crashed or backgrounded.

## M3 tests

Automate deterministic parts:

- progression bounds;
- never request max unless the user/platform policy explicitly allows and product requirements later justify it;
- immediate cancel state machine;
- restoration logic;
- process/lifecycle interruption handling where practical;
- accessibility semantics;
- UI outcome paths;
- no microphone permission.

Human/device checks remain required for:

- actual audibility;
- perceived loudness;
- comfort;
- speaker behavior;
- OEM-specific audio interaction.

## M3 exit

M3 exits when:

- guided flow works end to end;
- no forced unsafe maximum behavior exists;
- restoration is verified;
- manual audible validation exists on at least one physical device;
- accessibility smoke checks pass;
- exact SHA remote gates are green.

---

# What is still blocked — M4 Protection

Do **not** begin implementation of continuous/background protection merely because M2/M3 progress is complete.

Allowed before ADR-004 is superseded:

- pure domain policy refinement;
- UI mock/state design for future protection;
- deterministic tests of bounds/circuit breaker;
- documentation/research.

Blocked until runtime decision:

- production foreground service;
- persistent auto-restore runtime;
- background restart strategy;
- Quick Settings tile claiming active protection;
- battery qualification of a non-selected runtime;
- production copy promising continuous protection.

M4 starts only after Track A produces enough evidence to accept a new runtime ADR or consciously narrows the product promise.

---

# Parallel execution guidance

Good parallel work while waiting for Samsung/Pixel:

```text
Motorola manual evidence        ↔ M1 audit
Samsung acquisition/testing     ↔ M2 readiness semantics/UI
Pixel/AOSP evidence             ↔ M3 safe-test implementation
ADR runtime decision            → unlock M4
```

Avoid parallel work that edits the same files concurrently. One active implementer per overlapping area remains the default.

---

# Suggested logical commit sequence

Adapt to actual repository state:

```text
docs: classify M0C and parallel milestone gates
chore: close remaining M1 foundation gaps
feat: refine readiness semantics and issue model
feat: productize diagnosis home experience
feat: add safe verified correction path
 test: cover diagnosis decisions and correction readback
feat: implement guided safe sound test
 test: cover safe-test bounds cancellation and restoration
 docs: record physical evidence and milestone status
```

Do not manufacture commits when one coherent change is smaller/larger than these examples.

---

# Documentation/status rules

Update:

- `docs/current-state.md`;
- `docs/roadmap.md`;
- `CHANGELOG.md`;
- relevant ADR/business rules;
- `docs/evidence/M0-device-matrix.md` only with real evidence.

Use explicit states:

```text
complete
partial
experimental
deferred
blocked
not tested
unsupported
```

Do not close Issue #1 until the M0 feasibility decision itself meets its exit gate.

Consider separate issues for M1/M2/M3 execution if that improves task tracking, but do not fragment work into bureaucracy.

---

# Release strategy

Do not create `v0.0.1-m0` until M0 is actually qualified according to its gate.

M2/M3 progress may land on `main` before that tag. If so, the eventual M0 tag must not pretend to isolate only historical M0 code unless the exact tagged SHA and release notes explain what is included.

Before any tag/release:

- push candidate first;
- run appropriate local qualification once;
- remote gates green on exact SHA;
- HEAD == origin/main;
- docs/claims match implementation;
- artifact qualified from the same SHA.

Do not create `v1.0.0` before the real v1 scope is qualified.

---

# Final report for this progression task

Return:

1. final SHA;
2. commits created;
3. M0C status and new physical evidence;
4. M1 audit result and remaining gaps;
5. M2 features completed/partial;
6. M3 features completed/partial;
7. tests/static gates run;
8. remote CI/CodeQL status for final SHA;
9. unresolved product decisions;
10. items still blocked by ADR-004;
11. next exact recommended task.
