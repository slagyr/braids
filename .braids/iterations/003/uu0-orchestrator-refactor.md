# Cron worker is orchestrator only — spawns workers, does no bead work (braids-uu0)

## Summary
Refactored the projects cron system into a two-tier architecture: orchestrator + spawned workers.

## Details

### Problem
The cron worker was doing everything — finding work AND executing beads. This meant:
- Long-running bead work blocked the cron cycle
- No true parallelism possible
- Timeout risk on complex beads

### Solution
Split into two reference docs:

1. **`references/orchestrator.md`** (new) — The cron job reads this. It only: reads registry, checks concurrency, finds ready beads, spawns worker sessions, and exits. ~300s timeout.

2. **`references/worker.md`** (rewritten) — Spawned workers read this. Each worker receives a specific bead assignment (project path, iteration, bead id, channel). It claims the bead, does the work, writes a deliverable, closes the bead, commits, and sends notifications.

### Changes
- Created `references/orchestrator.md`
- Rewrote `references/worker.md` for single-bead worker pattern
- Updated cron job prompt to point to orchestrator.md
- Reduced cron timeout from 1800s to 300s (orchestrator is fast)
- Updated SKILL.md cron integration and architecture sections
- Worker sessions use label `project:<slug>:<bead-id>` for concurrency tracking
