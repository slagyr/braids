# braids-7yg: Remove verbose mode from orchestrator

## Changes

1. **`src/braids/config.clj`** — Removed `:verbose false` from defaults map
2. **`braids/references/orchestrator.md`** — Removed entire "Verbose Mode" section (lines 11-51), which documented step-by-step posting to orchestrator channel
3. **`spec/braids/config_spec.clj`** — Removed `:verbose false` from all expected maps and removed "parses verbose flag" test
4. **`spec/braids/config_io_spec.clj`** — Removed `:verbose false` from expected maps
5. **`~/.openclaw/braids/config.edn`** — Removed `:verbose false` from live config

## Verification

```
$ bb test 2>&1 | tail -3
Finished in 23.44 seconds
481 examples, 9 failures, 898 assertions
```

All 9 failures are pre-existing integration tests (project state validation), same as on main branch (482 examples, 9 failures, 900 assertions). Config-related specs all pass.

## Rationale

Verbose mode was causing orchestrator timeouts by adding multiple message tool calls per tick. The CLI log file (`/tmp/braids.log`) from iteration 020 work provides full observability without Discord overhead.
