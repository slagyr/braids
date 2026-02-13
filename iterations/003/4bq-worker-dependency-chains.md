# Worker should explicitly handle bead dependency chains (projects-skill-4bq)

## Summary
Added an explicit dependency verification step to the worker workflow so workers check that all dependencies are resolved before starting work on a bead.

## Details

### Problem
The orchestrator uses `bd ready` to find unblocked beads, but by the time a worker is spawned and claims the bead, dependencies could have changed (reopened, added). The worker had no mechanism to detect or handle this.

### Changes

**`references/worker.md`** — Added new Step 3 "Verify Dependencies" between claiming and working:
- Worker runs `bd dep list <bead-id>` after claiming
- If any dependency is not closed, marks bead as blocked and notifies Channel
- Documents that only direct dependencies need checking (transitive chains are handled by `bd ready` semantics)
- Renumbered subsequent steps (3→4 became 4→5, etc.)

**`SKILL.md`** — Added `bd dep list <id>` to the Beads Quick Reference section.

### Design Decisions
- **Direct deps only:** Workers check direct dependencies. Transitive chain resolution is already handled by `bd ready` — if a transitive dep is unresolved, the direct dep won't be closed.
- **Race condition focus:** This primarily guards against the window between orchestrator spawning and worker claiming where dependency state could change.
- **Block, don't retry:** If deps are unresolved, the worker blocks and stops rather than polling/waiting. The orchestrator will pick it up again when deps clear.
