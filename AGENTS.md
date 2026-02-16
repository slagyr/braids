# AGENTS.md

This project is managed by the **braids** skill. Config: `.braids/config.edn`. Goals and guardrails are below.

## Development Process

**This project follows strict Test-Driven Development.** Read the TDD skill (`~/.openclaw/skills/tdd/SKILL.md`) before writing any code. RED → GREEN → REFACTOR. No production code without a failing test.

## How to Work on This Project

**If you were spawned by the orchestrator** (your task message includes a bead id):
→ Follow `~/.openclaw/skills/braids/references/worker.md`
  (or online: https://raw.githubusercontent.com/slagyr/braids/refs/heads/main/braids/references/worker.md)

**If you're here on your own** (manual session, human asked you to help, etc.):
1. Read `.braids/config.edn` — understand the project settings
2. Read this file (AGENTS.md) — for goals, guardrails, and conventions
3. Find the active iteration: look in `.braids/iterations/*/ITERATION.md` for `Status: active`
4. Run `bd ready` to see available work
5. Pick a bead, then follow the worker workflow: `~/.openclaw/skills/braids/references/worker.md`
   (or online: https://raw.githubusercontent.com/slagyr/braids/refs/heads/main/braids/references/worker.md)

## Quick Reference

```bash
bd ready              # List unblocked tasks
bd show <id>          # View task details
bd update <id> --claim  # Claim a task
bd update <id> -s closed  # Close completed task
bd list               # List all tasks
bd dep list <id>      # List dependencies
```

## Session Completion

Work is NOT complete until `git push` succeeds.

```bash
git add -A && git commit -m "<summary> (<bead-id>)"
git pull --rebase
bd sync
git push
```

## Goal

Build and refine the "braids" OpenClaw skill — an autonomous project management system that enables OpenClaw agents to work on multiple long-running projects in the background with iterative delivery, parallel sub-agent execution, and structured check-ins.

## Guardrails

- Test workflow changes by using this project as the guinea pig
- Commit frequently with meaningful messages
- Ask before making changes that affect other skills or OpenClaw config
- **Test-first development:** Write or update tests BEFORE implementing a feature or fix. Every bead that adds or changes behavior must include a corresponding test. Tests are written in **speclj on Babashka** — no bash test scripts. Run the test suite before closing a bead — all specs must pass.
- **No untested changes:** If you can't write a test for it, document why in the deliverable. Structural tests, simulation tests, and contract checks are all valid test types.
- **Channel agent — beads only:** The channel/main session agent must NOT edit project files (SKILL.md, worker.md, orchestrator.md, config.edn, CONTRACTS.md, etc.) directly. It should only create beads, plan iterations, activate iterations, and review deliverables. All file changes go through beads assigned to workers.
