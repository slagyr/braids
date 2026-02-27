# braids-vgc: Optimize orchestrator — reduce zombie detection overhead

## Summary

Moved zombie detection from the orchestrator agent (N individual `bd show` calls) into the `braids orch-run` CLI (batch `bd list --json` per project). The orchestrator now makes **1 CLI call** instead of N+1.

## Changes

### `src/braids/orch.clj`
- Added `detect-zombies` pure function: given session info, project configs, and bead statuses, returns zombie entries with reasons (`session-ended`, `bead-closed`, `timeout`)
- Updated `format-orch-run-json` to include `zombies` array in output when present

### `src/braids/orch_io.clj`
- Added `load-bead-statuses`: batch loads all bead statuses per project via `bd list --json`
- Added `parse-session-labels`: parses JSON session info
- Added `gather-and-tick-with-zombies`: enhanced pipeline with batch zombie detection

### `src/braids/core.clj`
- `orch-run` now accepts `--session-labels '<json>'` flag

### `braids/references/orchestrator.md`
- Simplified from 6 steps to 5
- Removed manual zombie detection loop (N `bd show` calls)
- Orchestrator now passes session info to CLI in one call

### Tests
- 9 new specs for detect-zombies and format-orch-run-json with zombies

## Performance Impact

**Before:** N+2 tool calls (sessions_list + N × bd show + braids orch-run)
**After:** 2 tool calls (sessions_list + braids orch-run --session-labels)

## Verification

```
$ braids orch-run --session-labels '[{"label":"project:braids:braids-vgc","status":"running","ageSeconds":100}]'
{"action":"idle","reason":"all-at-capacity","disable_cron":false}

$ braids orch-run --session-labels '[{"label":"project:braids:braids-vgc","status":"completed","ageSeconds":100}]'
{"action":"spawn","spawns":[...],"zombies":[{"slug":"braids","bead":"braids-vgc","label":"project:braids:braids-vgc","reason":"session-ended"}]}
```

All new specs pass. 10 pre-existing integration test failures (unrelated).
