# Add braids config command set (projects-skill-k0p)

## Summary
Added `braids config` command with `get`, `set`, and `list` subcommands for reading and updating braids configuration.

## Details

### New CLI commands
- `braids config list` — displays all config key-value pairs (with defaults applied)
- `braids config get <key>` — retrieves a specific value; exits 1 if key not found
- `braids config set <key> <value>` — updates a key and persists to config.edn

### Implementation
- Added pure functions to `braids.config`: `config-get`, `config-set`, `config-list`, `config-help`
- Wired `config` command into `braids.core` dispatch with subcommand routing
- `config get` returns `{:ok value}` or `{:error msg}` for clean error handling
- `config set` loads existing config, merges the update, and saves back

### Tests
- `spec/braids/config_command_spec.clj` — 9 specs covering list, get, set, missing keys, usage messages, and exit codes
- All specs pass
