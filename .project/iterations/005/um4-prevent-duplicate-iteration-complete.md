# Prevent Duplicate Iteration-Complete Notifications

## Summary

Added a `.completing` lock file guard to prevent race conditions where multiple workers simultaneously detect iteration completion and send duplicate `iteration-complete` notifications or generate duplicate RETRO.md files.

## What Changed

### `~/.openclaw/skills/projects/references/worker.md` — Step 7

Added a **completion guard** before RETRO generation and notification:
- Worker checks if `iterations/<N>/.completing` already exists
- If it exists → another worker is already completing → skip entirely
- If it doesn't exist → create it (contents: bead id) → proceed with RETRO, ITERATION.md update, and notification
- Only the first worker to create `.completing` performs iteration completion

### `CONTRACTS.md` — Section 3.7

Updated the Iteration Completion Check invariant to document the `.completing` lock requirement. Specifies that only one worker completes an iteration even when multiple beads close simultaneously.

### `spec/simulation_spec.clj` — Scenario 16

Added 8 specs validating:
- worker.md documents the `.completing` guard
- worker.md instructs workers to skip if lock already exists
- CONTRACTS.md documents the atomicity invariant
- File-based lock simulation (create, detect, skip)

## Design Decision

Chose a simple lock file over `bd merge-slot` because:
1. The guard is iteration-scoped (one lock per iteration directory), not project-scoped
2. Lock files are visible in git and self-documenting
3. No additional `bd` dependency for a simple coordination primitive
4. The file is committed with the iteration completion commit, making it permanent
