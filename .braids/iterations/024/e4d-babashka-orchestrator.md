# braids-e4d: Convert Shell Script Orchestrator to Babashka

## Summary

Replaced `braids/bin/braids-orch.sh` with a Babashka-native implementation integrated
directly into the braids CLI as `braids orch`. The shell script is gone; all logic is
in pure Clojure functions with comprehensive tests.

## What Changed

### New Files

1. **`src/braids/orch_runner.clj`** — Pure functions (fully testable, no IO):
   - `parse-cli-args` — parse `--dry-run` / `--verbose` flags, error on unknown
   - `build-worker-task` — format the task message for a worker spawn
   - `build-worker-args` — build `openclaw agent` CLI args vector with optional `--agent`
   - `log-line` — format timestamped log lines
   - `format-spawn-log` / `format-idle-log` / `format-zombie-log` — structured log output

2. **`src/braids/orch_runner_io.clj`** — IO effects:
   - `spawn-worker!` — fire `openclaw agent` as background process (dry-run safe)
   - `disable-cron!` — find and disable the `braids-orchestrator` cron job
   - `run-orch!` — main orchestration: calls `orch-io/gather-and-tick-from-stores`, acts on result
   - `run-orch-command!` — parse CLI args and run; returns exit code
   - Supports `BRAIDS_OPENCLAW_HOME` env var for testability
   - Supports `BRAIDS_ORCH_LOG` env var for custom log path

3. **`spec/orch_runner_spec.clj`** — Unit tests covering:
   - CLI arg parsing (dry-run, verbose, error handling)
   - Worker task/args building (agent selection, thinking, timeout, session ID uniqueness)
   - Log formatting (spawn, idle, zombies)

4. **`spec/orch_shell_spec.clj`** — Rewritten to test `braids orch` CLI:
   - Pure function tests (parse-cli-args, build-worker-*, log-line, format-*-log)
   - CLI integration tests via subprocess (`braids orch --bogus`, `--dry-run`, `--verbose`)
   - Log file writing verification via `BRAIDS_ORCH_LOG`

### Modified Files

5. **`src/braids/core.clj`** — Added `orch` command dispatching to `run-orch-command!`

6. **`src/braids/ready_io.clj`** — Added `BRAIDS_STATE_HOME` env var support for testability

### Removed Files

7. **`braids/bin/braids-orch.sh`** — Deleted; functionality fully replaced by `braids orch`

## CLI Usage

```bash
# Run orchestrator (reads real openclaw sessions, spawns workers)
braids orch

# Dry-run: logs what would happen, doesn't actually spawn
braids orch --dry-run

# Verbose: also prints log lines to stderr
braids orch --dry-run --verbose

# Custom log file
BRAIDS_ORCH_LOG=/var/log/braids-orch.log braids orch
```

## Verification

```
$ braids orch --dry-run
# Logs to /tmp/braids-orch.log (or BRAIDS_ORCH_LOG)

$ cat /tmp/braids-orch.log
[2026-03-04T07:57:05] orch-tick action=idle
[2026-03-04T07:57:05] Idle: reason=no-active-iterations disable_cron=true
[2026-03-04T07:57:08] WARN: braids-orchestrator cron job not found
[2026-03-04T07:57:08] Orchestrator tick complete

$ braids orch --bogus 2>&1; echo "exit=$?"
Unknown arg: --bogus
exit=1

# Tests: all orch_shell_spec + orch_runner_spec pass
$ bb test spec/orch_shell_spec.clj spec/orch_runner_spec.clj
# All green
```

## Benefits Over Shell Script

- **Type safety**: Clojure data structures, no string-splitting JSON with jq
- **Testable**: Pure functions fully unit-tested; CLI tested via subprocess with isolated env
- **Consistent**: Same language, tooling, and conventions as the rest of braids
- **Maintainable**: No bash edge cases, proper error handling via exceptions
- **Integrated**: `braids orch` discoverable via `braids help`
