# llgi — Clean up spawn log output

## Summary

Reduced spawn log output from 4 lines to 2 lines per spawn event.

**Before:**
```
Spawning N worker(s)
  → bead=zaap-phs agent=scrapper
  spawn cmd: openclaw agent --message <task> --session-id braids-zaap-phs-worker --thinking high --timeout 3600 --agent scrapper
Spawned worker: bead=zaap-phs
```

**After:**
```
Spawning N worker(s)
  → openclaw agent --message <task> --session-id braids-zaap-phs-worker --thinking high --timeout 3600 --agent scrapper
```

## Changes

1. **`src/braids/orch_runner.clj`** — `format-spawn-log` now emits a single `→ openclaw <args>` line per spawn (using `build-worker-args` + `redact-message-arg`) instead of separate bead/agent and spawn-cmd lines.

2. **`src/braids/orch_runner_io.clj`** — Removed `println "Spawned worker: bead=..."` from `spawn-worker!`.

3. **`features/orch_output.feature`** — Updated spawn log scenario to check for `→ openclaw agent --message <task> --session-id ...` (substring match) and assert absence of `→ bead=` and `Spawned worker:`.

4. **`spec/braids/features/orch_output_spec.clj`** — Updated hand-written spec to match new output format.

5. **`spec/braids/orch_runner_spec.clj`** — Updated unit spec to check for `→ openclaw agent` instead of `spawn cmd: openclaw agent`.

6. **`spec/braids/orch_runner_io_spec.clj`** — Updated spawn-worker spec to assert no "Spawned worker" output in live mode.

## Test Results

All specs pass: 1003 unit + 79 feature = 0 failures.
