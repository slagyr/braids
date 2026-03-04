# braids-e4d: Convert Shell Script Orchestrator to Babashka

## Summary

Replaced `braids/bin/braids-orch.sh` with a Babashka-native implementation integrated directly into the braids CLI as `braids orch`.

## What Changed

### New Files

1. **`src/braids/orch_runner.clj`** — Pure functions:
   - `parse-cli-args` — parse `--dry-run` / `--verbose` flags
   - `build-worker-task` — format the task message for a worker spawn
   - `build-worker-args` — build `openclaw agent` CLI args vector
   - `log-line` — format timestamped log lines
   - `format-spawn-log` / `format-idle-log` / `format-zombie-log` — structured log output

2. **`src/braids/orch_runner_io.clj`** — IO effects:
   - `spawn-worker!` — fire `openclaw agent` as fire-and-forget background process
   - `disable-cron!` — find and disable the `braids-orchestrator` cron job
   - `run-orch!` — main orchestration: calls `orch-io/gather-and-tick-from-stores`, acts on result
   - `run-orch-command!` — parse CLI args and run

3. **`spec/orch_runner_spec.clj`** — 24 tests covering:
   - CLI arg parsing (dry-run, verbose, error handling)
   - Worker task/args building (agent selection, thinking, timeout, session ID uniqueness)
   - Log formatting (spawn, idle, zombies)

### Modified Files

4. **`src/braids/core.clj`** — Added `orch` command:
   - New entry: `"orch"` → dispatches to `orch-runner-io/run-orch-command!`
   - Visible in `braids help`

5. **`install.sh`** — Removed shell script installation; `braids orch` is built-in.

6. **`spec/orch_shell_spec.clj`** — Emptied (shell script removed; covered by orch_runner_spec).

### Deleted Files

7. **`braids/bin/braids-orch.sh`** — Removed entirely.

## Verification

```
$ braids orch --dry-run
# (no stdout; logs to /tmp/braids-orch.log)

$ cat /tmp/braids-orch.log
[2026-03-04T07:43:08] orch-tick action=idle
[2026-03-04T07:43:08] Idle: reason=no-ready-beads disable_cron=false
[2026-03-04T07:43:08] Orchestrator tick complete

$ braids help | grep orch
  orch         Run orchestrator: compute spawns, fire workers (replaces braids-orch script)
  orch-tick    Orchestrator tick: ...

# Tests: 24 new tests, all passing
```

## Benefits Over Shell Script

- **Type safety**: Clojure data structures, no string-splitting JSON with jq
- **Testable**: Pure functions in `orch-runner.clj` are fully unit-tested
- **Consistent**: Same language, tooling, and conventions as the rest of braids
- **Maintainable**: No bash edge cases, proper error handling via exceptions
- **Integrated**: `braids orch` is discoverable via `braids help`
