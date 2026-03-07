# AGENTS.md

This project is managed by the **braids** skill. Config: `.braids/config.edn`. Goals and guardrails are below.

## Toolbox

This project uses [toolbox](https://raw.githubusercontent.com/slagyr/toolbox/main/SKILL.md)
to manage agent components. If `.toolbox/` doesn't exist, fetch the toolbox
SKILL.md from the URL above and follow its instructions. Once bootstrapped:

- **Skills:** Load from `.toolbox/skills/{name}/SKILL.md` when their descriptions match the task at hand.
- **Commands:** When the user invokes a command by name (e.g., "/work"), read and follow `.toolbox/commands/{name}.md`.

### Skills

- [tdd](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/tdd/SKILL.md)
- [crap4clj](https://raw.githubusercontent.com/unclebob/crap4clj/master/SKILL.md)
- [using-clj-mutate](https://raw.githubusercontent.com/unclebob/clj-mutate/master/skills/using-clj-mutate/SKILL.md)
- [speclj-structure-check](https://raw.githubusercontent.com/unclebob/speclj-structure-check/master/.claude/skills/speclj-structure-check/SKILL.md)

### Commands

- [plan](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan.md)
- [todo](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/todo.md)
- [work](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/work.md)

## Development Process

**This project follows strict Test-Driven Development.** Load the `tdd` skill before writing any code. RED → GREEN → REFACTOR. No production code without a failing test.

## How to Work on This Project

**First:** Read `braids/references/worker.md` — it contains the full worker workflow (8 steps: load context, claim bead, verify deps, check tags, do work, write deliverable, close bead, check iteration). Follow it precisely.

**If you were spawned by the orchestrator** (your task message includes a bead id):
→ Read `braids/references/worker.md` and follow it from Step 1

**If you're here on your own** (manual session, human asked you to help, etc.):
1. Read `braids/references/worker.md` — understand the worker workflow
2. Read `.braids/config.edn` — understand the project settings
3. Read this file (AGENTS.md) — for goals, guardrails, and conventions
4. Find the active iteration: look in `.braids/iterations/*/iteration.edn` for `:status :active`
5. Run `bd ready` to see available work
6. Pick a bead, then follow `braids/references/worker.md` from Step 2

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

## Definition of Done

A bead is **not done** until all of the following are satisfied:

1. **Unit tests pass:** Run the full test suite (`bb test`) — all specs green
2. **CLI verification:** Test the actual `bd` CLI with real commands against a test project. Do not assume code changes work just because tests pass — run the CLI and confirm the output is correct
3. **Integration check:** If the change affects workflow (worker, orchestrator, iteration transitions), verify end-to-end by simulating the workflow with real CLI commands
4. **Document what you tested:** In the deliverable, include the actual commands you ran and their output (text, not screenshots). Example:
   ```
   ## Verification
   $ bd ready
   (output)
   $ bd show braids-xyz
   (output)
   ```

## Acceptance Criteria Standards

Beads should have **specific, verifiable** acceptance criteria. Avoid vague criteria like "it works" or "looks good." Good criteria:

- State the exact expected behavior: "Running `bd list` shows status in color when output is a TTY"
- Include edge cases: "Running `bd ready` with no open beads prints 'No ready beads' and exits 0"
- Specify what NOT to break: "Existing `bd list` output format unchanged for non-TTY"

## Safe Testing Practices

- **Use test projects** for integration testing — never run destructive experiments against real project data
- Create temporary test projects with `bd init` in `/tmp` or a scratch directory when needed
- When testing against this project, use read-only commands (`bd list`, `bd show`, `bd ready`) — do not create/delete/modify real beads for testing purposes
- If a test requires modifying state, set up and tear down in an isolated environment
