# Getting Started — VolumeOK + Codex

This guide covers the two supported ways to start working on VolumeOK with Codex.

Repository: `https://github.com/Soturine/volumeok`

Primary execution prompt: [`docs/prompts/M0-codex.md`](prompts/M0-codex.md)

---

## Option A — Codex web/cloud (no local clone required)

This is the fastest path when the repository is not yet cloned to the PC.

1. Open Codex from ChatGPT / Codex web.
2. Sign in with the ChatGPT account that has access to the GitHub connection.
3. Connect/select GitHub if prompted.
4. Select repository `Soturine/volumeok`.
5. Ask Codex to read and execute:

```text
Read AGENTS.md first, then execute docs/prompts/M0-codex.md against GitHub Issue #1.
Work directly on main as the repository instructions allow. Use multiple logical commits and push each meaningful milestone. Follow the async-CI policy: do not sit idle waiting for remote suites; continue independent work and checkpoint remote gates between milestones. Do not create the M0 tag/release unless the exact final SHA satisfies the qualification requirements in the prompt.
```

6. Review Codex evidence, changed files, tests, and remote Git state before accepting completion claims.

`AGENTS.md` is repository authority for agent workflow; the task prompt should not override stronger safety/security constraints.

---

## Option B — Local Windows clone + Codex CLI / IDE

### Prerequisites

Install/verify:

- Git for Windows
- Android Studio (when Android implementation starts)
- a current supported JDK selected by the Android Gradle Plugin used by the project
- Codex CLI or the Codex IDE extension

Do not install random binaries from third-party download sites. Use official Git/OpenAI/Android sources.

### 1. Choose a development folder

Example PowerShell:

```powershell
New-Item -ItemType Directory -Force "$HOME\Documents\GitHub" | Out-Null
Set-Location "$HOME\Documents\GitHub"
```

### 2. Clone VolumeOK

```powershell
git clone https://github.com/Soturine/volumeok.git
Set-Location .\volumeok
```

Verify:

```powershell
git remote -v
git branch --show-current
git status
git log --oneline -10
```

Expected remote:

```text
origin  https://github.com/Soturine/volumeok.git
```

Expected branch:

```text
main
```

### 3. Install Codex CLI

OpenAI currently documents the npm installation command:

```powershell
npm install -g @openai/codex
```

If Node/npm is not otherwise wanted in the Android repository, this installation is a **developer-machine tool**, not a project dependency. Do not add npm/package.json to VolumeOK just because Codex CLI was installed this way.

Then:

```powershell
codex
```

Sign in with ChatGPT when requested.

On Windows, if Codex itself has startup/connectivity problems, use:

```powershell
codex doctor
```

### 4. Start Codex from the repository root

Always start it from:

```text
...\GitHub\volumeok
```

so repository `AGENTS.md` and project files are in scope.

Initial instruction:

```text
Read AGENTS.md first, then execute docs/prompts/M0-codex.md against GitHub Issue #1.
```

### 5. IDE route

If using the Codex IDE extension, open the repository root in the IDE first.

For VS Code-compatible launch from PowerShell when `code` is installed:

```powershell
code .
```

For Android implementation, Android Studio should ultimately be able to import/open the repository root after the Gradle scaffold exists.

---

## Git behavior expected from Codex

VolumeOK currently permits direct-main development.

A proper milestone looks like:

```text
focused change
→ focused test
→ logical commit
→ git push origin main
→ confirm HEAD == origin/main
→ continue independent work while remote CI runs
→ checkpoint CI/security before milestone closure
```

Commands used to verify remote synchronization:

```powershell
git fetch origin
git rev-parse HEAD
git rev-parse origin/main
```

The two SHAs must match before Codex claims the current milestone is pushed.

Do not create artificial commits only to increase commit count.

---

## CI performance rule

During normal development:

- run focused tests locally;
- push logical milestones early;
- allow CI/Security to run remotely;
- continue independent work rather than waiting idle;
- checkpoint remote status between milestones;
- fix genuine red gates early;
- avoid constant polling;
- avoid repeating the same expensive full suite with no new evidence.

Near release candidate:

1. push candidate first;
2. remote CI/security and one appropriate local RC qualification may run in parallel;
3. fix failures in separate logical commits;
4. require exact final SHA on `origin/main`;
5. require applicable remote gates green for that SHA before tagging.

---

## M0 tag/release policy

The M0 task prompt defines the intended prerelease tag:

```text
v0.0.1-m0
```

and release title:

```text
VolumeOK v0.0.1-m0 — Feasibility Baseline
```

They must **not** be created just because code exists.

Create them only after the exact final SHA meets the M0 qualification rules. If real-device evidence is missing, report the status honestly and follow the task prompt's partial/experimental rules rather than calling M0 fully qualified.

A release is evidence/history, not a substitute for validation.

---

## Do not do these

- Do not create `v1.0.0` during M0.
- Do not use an old green CI run to qualify a newer SHA.
- Do not leave commits only local.
- Do not add Node/Prettier to the Android project just because Codex CLI uses npm for installation.
- Do not run full suites after every tiny edit.
- Do not claim physical-device behavior that was only tested in an emulator.
- Do not bypass Android/Play constraints with AccessibilityService hacks.
- Do not accept an agent's “done” report without evidence.
