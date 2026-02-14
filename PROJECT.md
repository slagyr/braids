# Projects Skill

- **Status:** active
- **Priority:** high
- **Autonomy:** full
- **Checkin:** daily
- **Channel:** 1471680593497554998
- **MaxWorkers:** 1

## Notifications

| Event | Notify |
|-------|--------|
| iteration-start | on |
| bead-start | on |
| bead-complete | on |
| iteration-complete | on (mention <@274692642116337664>) |
| no-ready-beads | on |
| question | on (mention <@274692642116337664>) |
| blocker | on (mention <@274692642116337664>) |

## Goal

Build and refine the "projects" OpenClaw skill — an autonomous project management system that enables OpenClaw agents to work on multiple long-running projects in the background with iterative delivery, parallel sub-agent execution, and structured check-ins.

## Guardrails

- Changes to the skill definition go in `~/projects/projects-skill/SKILL.md` (symlinked from `~/.openclaw/skills/projects/`)
- Test workflow changes by using this project as the guinea pig
- Commit frequently with meaningful messages
- Ask before making changes that affect other skills or OpenClaw config
- **Test-first development:** Write or update tests BEFORE implementing a feature or fix. Every bead that adds or changes behavior must include a corresponding test. Tests are written in **speclj on Babashka** — no bash test scripts. Run the test suite before closing a bead — all specs must pass.
- **No untested changes:** If you can't write a test for it, document why in the deliverable. Structural tests, simulation tests, and contract checks are all valid test types.
- **Channel agent — beads only:** The channel/main session agent must NOT edit project files (SKILL.md, worker.md, orchestrator.md, PROJECT.md, CONTRACTS.md, etc.) directly. It should only create beads, plan iterations, activate iterations, and review deliverables. All file changes go through beads assigned to workers.
