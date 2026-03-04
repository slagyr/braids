# Binary Path Config in Global Braids Config (braids-vnm)

## Summary

Added `:env-path`, `:bd-bin`, and `:openclaw-bin` keys to the global braids config (`~/.openclaw/braids/config.edn`) so all subprocess calls work in cron and other minimal-PATH environments without requiring a `PATH=` line in crontab.

## Details

### New config keys

| Key | Default | Description |
|-----|---------|-------------|
| `:env-path` | `nil` | Extra PATH directories prepended to subprocess PATH |
| `:bd-bin` | `"bd"` | Binary name or full path for the `bd` CLI tool |
| `:openclaw-bin` | `"openclaw"` | Binary name or full path for the `openclaw` CLI tool |

### New namespace: `braids.sys`

Pure helper functions for subprocess environment:
- `subprocess-env` — returns env map with `:env-path` prepended to PATH, for use as `:extra-env` in babashka.process calls
- `bd-bin` — resolves bd binary from config
- `openclaw-bin` — resolves openclaw binary from config

### Wiring

All subprocess callers now use config-driven binary resolution:
- `orch_io.clj` — `load-bead-statuses` and `load-open-beads` use `sys/bd-bin` + `sys/subprocess-env`
- `ready_io.clj` — `load-ready-beads` uses `sys/bd-bin` + `sys/subprocess-env`
- `orch_runner_io.clj` — `spawn-worker!` uses `sys/openclaw-bin` + `sys/subprocess-env`

Environment variable overrides (`BD_BIN`, `OPENCLAW_BIN`) still take precedence for test compatibility.

### CLI and docs

- `braids config` help text lists all five config keys with defaults
- `init.md` adds step 4 "Configure Binary Paths (Recommended)" before cron setup
- `SKILL.md` global config table includes the three new keys with examples

### Bug fix

Fixed `orch_runner_io/spawn-worker!` calling `build-worker-args` with wrong arity (1 arg instead of required 2: `config` + `spawn`).

## Files changed

- `src/braids/config.clj` — added defaults for new keys + help text
- `src/braids/sys.clj` — new namespace
- `src/braids/orch_io.clj` — wired sys/config-io for bd subprocess calls
- `src/braids/ready_io.clj` — wired sys/config-io for bd subprocess calls
- `src/braids/orch_runner_io.clj` — wired sys/config-io for openclaw subprocess calls + arity fix
- `spec/braids/config_spec.clj` — updated expected values for new defaults
- `spec/braids/config_io_spec.clj` — updated expected values for new defaults
- `spec/braids/sys_spec.clj` — new spec for sys namespace
- `braids/SKILL.md` — documented new config keys
- `braids/references/init.md` — added binary path configuration step
