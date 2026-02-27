# braids-42h: CLI collect session information internally

## Summary

`braids orch-run` now collects session information internally by reading openclaw session store files, eliminating the need for the orchestrator to call `sessions_list` and pass `--sessions` flags.

## Changes

### `src/braids/orch_io.clj`
- Added `load-sessions-from-stores` — reads `~/.openclaw/agents/*/sessions/sessions.json`, extracts sessions with `project:` labels, returns `[{:label :status :age-seconds}]` maps
- Added `gather-and-tick-from-stores` and `gather-and-tick-from-stores-debug` — full orch pipeline using internal session collection with zombie detection

### `src/braids/core.clj`
- Changed `orch-run` default (no flags) from `gather-and-tick-debug` (which assumed no sessions) to `gather-and-tick-from-stores-debug` (which reads session stores)
- `--sessions` and `--session-labels` flags still work for backward compatibility

### `spec/braids/orch_io_spec.clj`
- Added 5 tests for `load-sessions-from-stores`: basic extraction, empty stores, multi-agent, non-project label filtering, malformed JSON handling

### `~/.openclaw/skills/braids/references/orchestrator.md`
- Updated Step 1 to show `braids orch-run` with no flags (was `--sessions`)
- Reduced tool calls from 3 to 2 (no more `sessions_list`)
- Updated verbose mode steps (removed sessions_list step, renumbered)

## Verification

```
$ cd ~/Projects/braids && bb braids orch-run 2>/dev/null
{"action":"idle","reason":"all-at-capacity","disable_cron":false}

$ bb braids orch-run 2>&1 1>/dev/null
  braids  active  iteration 020  → 4 beads
    ○ 73f  open
    ○ 7yg  open
    ○ et3  open
    ⚙️ 42h  in_progress
  ...
  → idle: all-at-capacity  [disable_cron: false]

# Backward compat: --sessions still works
$ bb braids orch-run --sessions '' 2>/dev/null
{"action":"spawn","spawns":[...]}

# Tests: 478 examples, 9 failures (all pre-existing), 895 assertions
```
