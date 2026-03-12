# braids-5trt: Update orch to use openclaw sessions spawn

## Summary
Updated the braids orchestrator to spawn workers using `openclaw sessions spawn` instead of `openclaw agent --message`, creating properly isolated sessions that don't inherit Discord deliveryContext.

## Changes

### `src/braids/orch_runner.clj`
- **`build-worker-args`**: Changed from `["agent" "--message" task "--session-id" session-id ...]` to `["sessions" "spawn" "--task" task "--label" label ...]`
  - Uses `--task` instead of `--message`
  - Uses `--label project:<slug>:<bead-id>` instead of `--session-id braids-<bead>-worker`
  - Derives slug from `:project` key (set by orch tick) or falls back to extracting from bead ID
- **`redact-message-arg`** → **`redact-task-arg`**: Updated to redact `--task` instead of `--message`
- **`format-spawn-log`**: Updated to use `redact-task-arg`

### `src/braids/features/harness.clj`
- **`build-worker-args!`**: Extracts `--label` instead of `--session-id` from args
- Added **`label-result`** accessor function

### Specs Updated
- `spec/braids/orch_runner_spec.clj` — All `build-worker-args` and `format-spawn-log` assertions updated
- `spec/braids/orch_runner_io_spec.clj` — spawn-worker! and run-orch! assertions updated to expect `sessions spawn`
- `spec/braids/features/orch_runner_spec.clj` — Updated to assert `--task`, `--label`, `sessions`, `spawn`
- `spec/braids/features/orch_output_spec.clj` — Spawn log assertions updated
- `spec/braids/features/harness_spec.clj` — Updated `build-worker-args!` assertions

### Feature Files Updated
- `features/orch_runner.feature` — Updated spawn command format in scenarios
- `features/orch_output.feature` — Updated spawn log assertions, fixed `\(` escape issue

## New Command Format
```
openclaw sessions spawn --agent <worker-agent> --task "<task>" --thinking high --timeout <timeout> --label project:<slug>:<bead-id>
```

## Test Results
- **1003 unit specs**: All passing ✅
- **Feature specs**: 3 pre-existing failures (bead formatting assertions unrelated to this change), 2 pending
