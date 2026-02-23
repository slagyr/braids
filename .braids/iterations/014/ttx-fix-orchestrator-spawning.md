# Fix Orchestrator Spawning Logic (braids-ttx)

## Problem
The orchestrator's spawn message and JSON output had two issues:
1. **Missing worker instruction** — `spawn-msg` output only contained `Project/Bead/Iteration/Channel` fields but not the instruction telling the worker to read `worker.md`. Workers needed external context to know their onboarding process.
2. **Mismatched JSON field names** — `format-spawn-msg-json` output `message` and `worker_timeout` fields, but `sessions_spawn` expects `task` and `runTimeoutSeconds`. The `cleanup` and `thinking` parameters were also missing from the JSON output.

## Changes

### src/braids/orch.clj
- Added `worker-instruction` constant with the standard worker onboarding message
- `spawn-msg` now prefixes output with the worker.md instruction
- `format-spawn-msg-json` now outputs `task` (not `message`), `runTimeoutSeconds` (not `worker_timeout`), plus `cleanup: "delete"` and `thinking: "low"`

### braids/references/orchestrator.md
- Step 4 now instructs the orchestrator to use `braids spawn-msg --json` and pass the output directly to `sessions_spawn`
- Documents the complete JSON structure

### CONTRACTS.md
- Updated §2.4 to reflect the new spawn message format with worker instruction prefix and JSON fields

### Tests
- Updated `spawn_msg_spec.clj` to verify worker instruction prefix and new JSON field names
- Updated `core_spec.clj` spawn-msg integration tests
- Updated `simulation_spec.clj` spawn message format test
