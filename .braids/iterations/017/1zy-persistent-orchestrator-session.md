# Run orchestrator in persistent session with aggressive compaction (braids-1zy)

## Summary

Switched the braids orchestrator cron job from isolated sessions to a persistent session using `sessionKey: braids:orchestrator`. Updated SKILL.md and orchestrator.md documentation accordingly.

## Changes

### Cron job configuration
- Set `sessionKey: braids:orchestrator` on the live cron job via `openclaw cron edit`
- Session target changed from `isolated` to `main` (with session key routing)

### SKILL.md (Cron Integration section)
- Updated JSON example to show `sessionTarget: "main"` + `sessionKey: "braids:orchestrator"`
- Added explanation of why persistent sessions are used and how to configure/reset them

### orchestrator.md (Troubleshooting section)
- Replaced the "delete and recreate" overflow fix with the simpler `--clear-session-key` / `--session-key` reset flow
- Documented that the gateway automatically compacts old turns

## How it works

With a `sessionKey`, the gateway routes all orchestrator cron ticks to the same session. The gateway's automatic transcript compaction summarizes prior turns when the context grows, keeping each tick lightweight. Combined with the 3-tool-call orchestrator flow (sessions_list → orch-run → spawn), this eliminates the expensive context reload that happened with isolated sessions.

## Verification

```
$ openclaw cron list --json (filtered)
{
  "sessionTarget": "main",
  "sessionKey": "braids:orchestrator"
}

$ grep -A2 sessionTarget braids/SKILL.md
  "sessionTarget": "main",
  "sessionKey": "braids:orchestrator",
  "delivery": "none"

$ bb test → 455 examples, 10 failures (all pre-existing integration test failures in zaap project checks, unrelated to this change)
```
