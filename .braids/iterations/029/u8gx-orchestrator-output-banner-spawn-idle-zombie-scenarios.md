# Implement orch_output banner, spawn/idle/zombie log scenarios

## Summary

Added support for testing orchestrator output in dry-run and confirmed modes, including banner, spawn log, idle log, and zombie log.

## Changes Made

### Source Code Changes

- `src/braids/orch_runner.clj`: Added dynamic var `*test-zombies*` for test harness to inject zombie session data. Modified `zombie-sessions` function to use test data when available.

### Test Harness Changes

- `test/braids/features/orch_output_harness.clj`: Added `zombie-sessions` atom, `set-zombie-sessions-from-table` function, `orch-tick-dry-run!` and `orch-tick-confirmed!` functions with binding for test zombies.

### Feature Generator Changes

- `src/braids/features/orch_output.clj`: Added step patterns for "zombie sessions:", "the orchestrator ticks in dry-run mode", and "the orchestrator ticks in confirmed mode".

### Feature File Changes

- `features/orch_output.feature`: Removed `@wip` tags from the 5 new scenarios (banner in dry-run mode, banner in confirmed mode, spawn log, idle log, zombie log).

## Verification

### Unit Tests
```bash
$ bb spec
Ran 13 specs, 0 failures.
```

### Feature Tests
```bash
$ bb features orch_output
13 scenarios: 13 passed, 0 failed.
```

### CLI Verification
Confirmed `bd ready` shows the bead as closed and no new unblocked beads for the iteration.

### Edge Cases Tested
- Dry-run mode produces "DRY-RUN started/completed" banner
- Confirmed mode produces "CONFIRMED started/completed" banner  
- Zombie sessions table properly formats log output
- Spawn/idle logs appear when projects have active/inactive workers
- Output contains expected line matches and ordering