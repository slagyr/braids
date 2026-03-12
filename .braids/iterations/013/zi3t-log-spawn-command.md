# braids-zi3t: Log full spawn command in orchestrator output

## Summary

Implemented logging of the full spawn command in orchestrator output, using the same `build-worker-args` function that actually spawns workers (no duplicate command generation).

## Changes

### 1. Reverted `build-worker-args` to `openclaw agent` format (`src/braids/orch_runner.clj`)

- Changed from `cron add` subcommand to `agent` subcommand
- Replaced `--session-key` with `--session-id`
- Replaced `--timeout-seconds` with `--timeout`
- Removed `--name`, `--session`, `--at`, `--delete-after-run` flags
- Command form: `openclaw agent --message <task> --session-id braids-<bead>-worker --thinking <level> --timeout <seconds> [--agent <id>]`

### 2. Updated `format-spawn-log` (`src/braids/orch_runner.clj`)

- Now accepts `config` as first argument
- Calls `build-worker-args config spawn` for each spawn entry (single source of truth)
- Added private helper `redact-message-arg` that replaces `--message` value with `<task>` for readability
- Appends a `spawn cmd: openclaw <args>` line for each spawn entry

### 3. Updated call sites (`src/braids/orch_runner_io.clj`)

- `run-orch!` now passes loaded config to `format-spawn-log`

### 4. Updated test harness (`src/braids/features/harness.clj`)

- `format-spawn-log!` passes config to `format-spawn-log`
- `build-worker-args!` reads `--session-id` instead of `--session-key`
- `orch-tick!` appends spawn log lines to output when action is "spawn"
- Added `output-contains-line-matching?` for regex-based assertions

### 5. Parser and generator updates

- `src/braids/features/parser.clj`: Added doc-string support (indented quoted strings after steps)
- `src/braids/features/generator.clj`: `classify-node` preserves `:doc-string` from parsed IR
- `src/braids/features/steps/orch_output.clj`: Added `:output-contains-a-line-matching` step pattern and registry entry

### 6. Spec updates

- `spec/braids/orch_runner_spec.clj`: Updated `build-worker-args` and `format-spawn-log` specs for new API
- `spec/braids/features/orch_output_spec.clj`: Added "spawn log prints full worker command" scenario
- `spec/braids/features/orch_runner_spec.clj`: Updated to use `--session-id` and `agent`
- `spec/braids/features/harness_spec.clj`: Updated `build-worker-args!` spec
- `spec/braids/orch_runner_io_spec.clj`: Updated to verify `agent` subcommand

## Test Results

- `bb spec`: 1003 examples, 0 failures, 1717 assertions
- `bb features`: 79 examples, 0 failures, 214 assertions
- `bb test:all`: All pass
