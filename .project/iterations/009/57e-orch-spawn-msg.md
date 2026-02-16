# braids orch spawn-msg: emit spawn message for sessions_spawn (projects-skill-57e)

## Summary
Added `spawn-msg` CLI command and pure functions to generate the spawn message that the orchestrator passes to `sessions_spawn` when spawning a worker.

## Details

### Pure functions (orch.clj)
- `spawn-msg` — takes a spawn entry map (as returned by `tick`) and returns the 4-line message string: `Project: <path>\nBead: <id>\nIteration: <num>\nChannel: <id>`
- `format-spawn-msg-json` — wraps the message with `label` and `worker_timeout` in JSON, ready for the orchestrator to pass to session spawning

### CLI command (core.clj)
- `braids spawn-msg '<spawn-json>'` — outputs the plain-text spawn message
- `braids spawn-msg '<spawn-json>' --json` — outputs JSON with message, label, and worker_timeout
- Handles snake_case→kebab-case key normalization from JSON input

### Tests
- `spawn_msg_spec.clj` — 3 specs covering message generation, empty channel, and JSON output
- `core_spec.clj` — 3 specs covering CLI plain output, JSON output, and usage error
