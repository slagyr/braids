# Debug orchestrator agent behavior (braids-hk9)

## Summary

Diagnosed and fixed the orchestrator agent's failure to spawn workers. Root cause: **context overflow** from accumulated cron session transcripts.

## Root Cause

The orchestrator cron job (`sessionTarget: "isolated"`) reuses the same session across runs. Each run (every 5 min) adds to the transcript: reading orchestrator.md, running `sessions_list`, running `braids orch-tick`, running `braids spawn-msg` per spawn, and calling `sessions_spawn`. After many runs, the 2MB+ transcript exceeded the model's context window, causing every run to fail with "Context overflow: prompt too large for the model."

The cron run history showed **every recent run** failing with this error (~2.5s each, no actual work done).

## Changes

### 1. New `braids orch-run` command (src/braids/orch.clj, core.clj)

Added `format-orch-run-json` which combines `orch-tick` + `spawn-msg` into a single JSON output. Each spawn entry is pre-formatted with all `sessions_spawn` parameters (`task`, `label`, `runTimeoutSeconds`, `cleanup`, `thinking`). This eliminates the need for the agent to run `spawn-msg` separately per spawn.

**Before:** Agent runs `orch-tick` → parses JSON → runs `spawn-msg` per spawn → calls `sessions_spawn` per spawn (many tool calls per run).

**After:** Agent runs `orch-run` → calls `sessions_spawn` per spawn (minimal tool calls per run).

### 2. Simplified orchestrator.md

Reduced orchestrator instructions to use the new `orch-run` command. Removed the `spawn-msg` step entirely. Added a "Troubleshooting" section documenting the context overflow issue and how to fix it (delete and recreate the cron job).

### 3. Reset stuck cron session

Deleted the old cron job (with 2MB accumulated transcript) and created a fresh one with a new session ID.

## Tests

Added 3 specs for `format-orch-run-json`:
- Formats idle result with reason and disable_cron
- Formats spawn result with sessions_spawn-ready entries
- Formats multiple spawns from different projects

All existing tests continue to pass (12 pre-existing failures unrelated to this change).
