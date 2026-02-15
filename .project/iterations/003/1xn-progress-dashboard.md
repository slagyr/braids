# Add Progress Dashboard: Auto-generate STATUS.md (projects-skill-1xn)

## Summary

Added auto-generated `STATUS.md` progress dashboard across all active projects. The orchestrator generates this file at the end of every run.

## Details

### Changes Made

1. **Created `references/status-dashboard.md`** — New reference doc defining the STATUS.md format, generation steps, and when it runs. Covers:
   - Per-project sections with iteration info, bead counts by status, and active bead listings
   - Timestamp for freshness tracking
   - Graceful handling of projects with no active iteration

2. **Updated `references/orchestrator.md`** — Added Step 6 (Generate Status Dashboard) before the final "Done" step. The orchestrator now generates STATUS.md after spawning workers.

3. **Updated `SKILL.md`** — Added `STATUS.md` to the directory layout documentation.

4. **Generated initial `$PROJECTS_HOME/STATUS.md`** — Proved the format works across all three active projects (projects-skill, zane-setup, wealth).

### Design Decisions

- **Orchestrator-driven, not separate cron** — STATUS.md regenerates every orchestrator run (~5 min), keeping it current without extra scheduling complexity.
- **Single cross-project file** — One `$PROJECTS_HOME/STATUS.md` rather than per-project files, since the value is the cross-project overview.
- **Overwrite model** — File is fully regenerated each time (not appended). It's a snapshot, not a log.
