# DevOps / DevSecOps

## Delivery model

Use small, reviewable batches.

```text
focused implementation
→ focused tests
→ logical commit
→ push remote
→ CI/Security async
→ continue independent work
→ checkpoint
```

Commit local != remote sync. Milestones require remote push and verification that the intended SHA exists on `origin/main` or the target branch.

## Baseline quality gates

When Android source exists, every relevant push/PR should run proportional checks such as:
- Gradle build/compile;
- unit tests;
- Spotless/ktlint formatting checks;
- detekt;
- Android Lint;
- dependency verification;
- license/dependency review where available.

Milestone/PR expansion:
- instrumentation/component tests as blast radius requires;
- CodeQL for Java/Kotlin/GitHub Actions;
- dependency review;
- security checks;
- documentation consistency checks.

Do not run expensive full suites on every trivial edit if focused evidence is sufficient; do run them for RC/milestones where the blast radius warrants it.

## Formatting / static analysis

Primary source of style: `.editorconfig`.

Planned Android tooling:
- **Spotless + ktlint** — deterministic formatting/checking for Kotlin/Kotlin DSL;
- **detekt** — code-quality/static-analysis rules;
- **Android Lint** — Android correctness, accessibility, performance, security, and i18n signals.

**Prettier is not a Kotlin formatter and should not force Node/npm into the Android app.** If a future TypeScript/JavaScript web subproject exists, Prettier belongs to that subproject.

For Markdown/YAML/JSON docs/workflows, prefer lightweight dedicated checks rather than introducing a runtime dependency into the Android app.

## Dependency governance

Every new runtime/build dependency must justify:
- purpose;
- maintenance/activity;
- license;
- known security posture;
- transitive dependencies;
- APK/build/runtime cost;
- Android/API compatibility;
- whether platform APIs could solve the need with less risk.

Use:
- Gradle Version Catalog when scaffolded;
- dependency locking where appropriate;
- Gradle dependency verification (checksums/signatures);
- Dependabot as intelligence, not automatic authority.

M1 uses exact versions in a Version Catalog and centralized repositories with project repositories rejected. Locking
or verification metadata must be added before release qualification, when the resolved release graph can be reviewed;
their absence in the current pre-release baseline is explicit and must not be described as an active control.

Do not auto-merge dependency PRs blindly.

## GitHub Actions security

- `permissions` least privilege;
- pin third-party actions to full commit SHAs where practical;
- avoid untrusted PR content entering privileged shell contexts;
- no secrets printed in logs;
- cancel superseded expensive workflow runs when safe;
- artifact retention bounded.

## Secrets

MVP should need no product secrets.

Future secrets must never exist in:
- repository files;
- client resources;
- screenshots;
- test fixtures;
- docs;
- logs.

Use environment/repository deployment secrets with least privilege only when required.

## CI workflow intent

Proposed workflows once source is scaffolded:

```text
ci.yml
  formatting
  static-analysis
  unit-tests
  assemble

security.yml
  CodeQL
  dependency/security checks

android-instrumentation.yml
  selected milestone/device/emulator tests

release.yml
  exact-SHA qualification
  AAB
  checksum
  SBOM/provenance
```

Do not create redundant workflows that rebuild/test the same artifact repeatedly without a clear trust-boundary reason.

## Release engineering

Target flow:

```text
source SHA
→ qualification tests
→ release AAB
→ checksum
→ SBOM
→ provenance/metadata
→ exact green SHA verification
→ tag
→ Play internal/closed/staged production promotion
```

The artifact published must be the artifact qualified. Do not rebuild a “similar” AAB after qualification just because a tag was created.

## Versioning

Use semantic versioning pragmatically for product releases:
- patch: compatible fixes;
- minor: compatible product capabilities;
- major: intentionally breaking product/data/contract behavior.

Android `versionCode` remains monotonically increasing and is separate from human-readable version semantics.

## Play release strategy

- internal testing first;
- closed testing with representative devices/users;
- pre-launch reports and Android Vitals reviewed;
- staged production rollout;
- rollback/halt criteria documented before broad rollout.

## Security severity handling

High/Critical real vulnerabilities in active paths are addressed early.

Patch/minor dependency updates are evaluated independently. Major dependency churn is not merged solely because a bot proposed it.

## Build reproducibility

Aim for:
- pinned toolchain versions;
- Gradle wrapper committed;
- documented JDK/Android SDK requirements;
- deterministic formatting;
- no local-machine-only build steps;
- canonical bootstrap/check commands.

## Definition of Done for implementation tasks

A task is Done only when applicable:
- acceptance behavior works;
- negative paths handled;
- tests meaningful and green;
- static/security checks pass;
- docs/contracts updated;
- no relevant dead code/placeholders;
- commit is logical;
- remote push exists;
- required remote gates are green;
- claims match evidence.
