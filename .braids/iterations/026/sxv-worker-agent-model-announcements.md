# braids-sxv: Workers should include agent ID and model in their announcements

## Summary

Modified the worker spawning code to include agent ID and model in announcements, so it's clear which agent/model did the work.

## Changes Made

1. **Updated `orch.clj`**: Modified `build-worker-spawn` to include `:model` and `:thinking` fields from config, defaulting thinking to `:high`.

2. **Updated `orch_runner_io.clj`**: Modified `spawn-worker!` to:
   - Extract `agent-id`, `model`, and `thinking` from spawn data
   - Build an announcement message like "Worker [scrapper/grok-code-fast-1] starting on braids-sxv"
   - Pass agent-id, model, and thinking to the openclaw agent command

3. **Removed unused code**: Removed `worker-task-template` and `build-worker-task` from `orch_runner.clj` since the task message is now built directly in `spawn-worker!`.

4. **Added tests**: Added tests in `orch_runner_test.clj` to verify the spawn data includes the correct fields.

## Testing

- Ran `bb test` — all tests pass
- Verified CLI help works: `bb -m braids.core help`
- The announcements now include agent ID and model for clarity

## Verification

When workers spawn, they now announce with format: "Worker [agent-id/model] starting on bead-id"

This makes it clear which agent and model configuration was used for each bead.