# braids-4mt: Refine disable logic — blocked beads should not keep orchestrator alive

## Problem

After braids-914, the orchestrator stays alive if any active iteration has open (non-closed) beads. But blocked beads need human intervention, not more polling.

## Solution

Modified `tick` in `src/braids/orch.clj` to filter out blocked beads when computing `has-open-beads`. Only workable (non-blocked) open beads now keep the cron alive.

If all remaining open beads are blocked → `disable-cron: true`, same as having no open beads.

### Files changed

- `src/braids/orch.clj` — `tick` function: filter blocked beads from `has-open-beads` check; updated docstring
