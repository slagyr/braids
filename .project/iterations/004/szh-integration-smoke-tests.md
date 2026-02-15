# Integration Smoke Tests

## Summary

Added `tests/test_integration_smoke.sh` — end-to-end validation that checks live project state across all registered projects. Unlike simulation tests (mock data) or structural tests (file existence), these verify that the actual system is internally consistent.

## What It Validates

### Per-Project Checks
1. **Git state** — branch status, unpushed commit count
2. **Completed iterations** — RETRO.md exists, every bead has a deliverable, bd status (closed or archived), git commits reference bead IDs
3. **Active iterations** — closed beads have deliverables and git commits, all listed beads exist in bd, detects "all closed but still active" inconsistency
4. **Single active iteration** — at most one per project (§1.3)
5. **Orphaned deliverables** — deliverable files that don't match any bead in their iteration

### Cross-Project Checks
6. **STATUS.md freshness** — exists and was recently updated
7. **Orchestrator state** — valid JSON, required fields present, idleReason is a known value

## Design Decisions

- **Tolerant of old data:** Beads not found in `bd` for completed iterations are treated as "archived/pruned" (pass), since older iterations had their beads removed from the tracker.
- **Both naming conventions:** Handles full bead-id filenames (`projects-skill-fvd.md`, iter 001 style) and suffix-style (`fvd-description.md`, iter 002+ style).
- **Real findings over green:** The test intentionally surfaces pre-existing inconsistencies (missing RETROs for old iterations, orphaned deliverables from migration periods, etc.) rather than suppressing them.

## Current Results

205 passed, 37 failed. All failures are legitimate pre-existing data issues:
- Missing RETRO.md for iterations 001, 002, wealth-001 (predated convention)
- Deliverables misplaced between iterations 001↔002 during naming convention transition
- Wealth project didn't follow git commit-per-bead convention
- A few beads from iter 003 listed in iter 004 show as closed but without iter-004 deliverables

## Files Changed
- `tests/test_integration_smoke.sh` (new)
