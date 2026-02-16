# Iteration 009

Status: active

## Stories
- projects-skill-98r: braids list: show projects from registry
- projects-skill-604: braids iteration: show active iteration and bead statuses
- projects-skill-v1n: braids status: dashboard view
- projects-skill-57e: braids orch spawn-msg: emit spawn message for sessions_spawn
- projects-skill-8bf: Move registry.md, orchestrator-state, and STATUS.md to ~/.openclaw/projects/
- projects-skill-1qc: braids migrate: migrate existing installs

## Guardrails
- TDD: RED → GREEN → REFACTOR for every feature
- git pull before making changes
- All specs must pass with `bb test` before closing any bead
- 8bf should land before 1qc (migration needs to know the new paths)
- CLI commands should support both human-friendly output and --json flag
- Use the EDN schemas from qle — no markdown parsing in new code

## Notes
- Theme: CLI commands + infrastructure reorganization
- 6 beads, all unblocked — workers can run in parallel
- a4o (rename to braids) and P2 beads (init, new) deferred to iteration 010
