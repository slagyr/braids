# Comprehensive braids CLI cleanup (braids-ao6)

## Summary

Cleaned up the braids CLI for better usability and maintainability. Removed legacy features, simplified the orchestrator command, added per-command help, and updated all documentation.

## Changes

### 1. Removed `orch-tick` command
- Removed from `commands` map in `core.clj`
- Removed the entire `:orch-tick` case block from `run`
- The `orch` command now handles everything `orch-tick` did

### 2. Updated `orch` help text
- New description: "Run orchestrator: compute spawns, start workers (defaults to dry run)"

### 3. Per-command `--help` support
- Every sub-command now supports `--help` / `-h` flags
- Each command has a `:help` string in the commands map
- `braids orch --help`, `braids list --help`, etc. all work

### 4. Removed JSON output from `orch`
- `orch` now outputs human-readable text only
- No more `format-orch-tick-json` calls from the runner
- The function remains in `orch.clj` (unused but tested) for backward compat

### 5. `orch` writes to stdout
- All output goes to stdout (was: debug to stderr, JSON to stdout, logs to file)
- Removed the `write-log!` calls and `BRAIDS_ORCH_LOG` env var from the runner
- Clean piping: `braids orch --run >> /tmp/braids.log 2>&1`

### 6. Verbose stdout logging
- `orch` always prints the human-readable project/bead summary
- Shows what it found and what action it will take

### 7. Removed disable logic from `orch`
- No more `disable-cron` handling in the runner
- No cron self-disable — cron job stays active, orch idles cheaply
- Removed `find-cron-id!` and `disable-cron!` from `orch_runner_io.clj`
- Updated CONTRACTS.md §1.5 and SKILL.md

### 8. Cron appends to /tmp/braids.log
- Updated all docs: system cron with `>> /tmp/braids.log 2>&1`
- Updated `references/orchestrator.md`, `references/init.md`, `SKILL.md`

### 9. Documentation updates
- `braids/SKILL.md` — Updated cron section, removed self-disable section, updated orch references
- `braids/references/orchestrator.md` — Complete rewrite for new CLI-first approach
- `braids/references/init.md` — Updated cron setup instructions
- `CONTRACTS.md` — Updated §1.5 to reflect stdout-based output

### 10. Evaluated `list` command
- **Kept.** `braids list` provides a unique overview of all projects with status, iterations, and progress that no other command offers

### Default dry-run
- `braids orch` now defaults to dry-run (safe by default)
- Use `--run` to actually spawn workers
- Updated `parse-cli-args` and all tests

## Files Modified
- `src/braids/core.clj` — Removed orch-tick, added --help, cleaned requires
- `src/braids/orch_runner.clj` — Default dry-run, removed disable logic, simplified log formats
- `src/braids/orch_runner_io.clj` — Stdout output, removed log files, removed disable logic
- `braids/SKILL.md` — Updated cron, removed self-disable
- `braids/references/orchestrator.md` — Complete rewrite
- `braids/references/init.md` — Updated cron setup
- `CONTRACTS.md` — Updated §1.5
- `spec/orch_runner_spec.clj` — Updated for new defaults and formats
- `spec/orch_shell_spec.clj` — Updated for new behavior
