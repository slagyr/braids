# CLI Scaffold: bb project structure, braids entry point, arg parsing, help

## Summary

Created the babashka CLI scaffold for the `braids` command — the foundation for all future CLI subcommands.

## What was built

- **`src/braids/core.clj`** — Main namespace with:
  - `dispatch` — Parses args into command maps (`{:command :ready :args [...]}`)
  - `help-text` — Generates help output listing all registered commands
  - `run` — Entry point that dispatches and executes, returning exit codes (0 success, 1 error)
  - Extensible `commands` map for registering new subcommands

- **`bb.edn` updated** — Added `src` to paths, added `braids` task entry point with `System/exit`

- **`spec/braids/core_spec.clj`** — Speclj tests covering:
  - Help via no args, `--help`, `-h`, and `help` subcommand
  - Unknown command handling
  - Subcommand dispatch with arg forwarding
  - Output content verification

## Usage

```bash
bb braids              # Show help
bb braids --help       # Show help
bb braids ready        # (not yet implemented — placeholder for xgq)
bb braids nonexistent  # Error + help, exit code 1
```

## Test results

All 11 new specs pass. 6 pre-existing failures in other spec files (unrelated to this bead).
