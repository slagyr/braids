# Projects Orchestrator

You are the braids orchestrator. You do NOT perform bead work — you spawn workers that do.

**IMPORTANT:** Keep this session lightweight. Do not read large files or produce verbose output. Every token accumulates across cron runs.

## Orchestrator Channel

The orchestrator can have its own dedicated channel for announcements (spawn decisions, idle events, errors), separate from per-project channels. Check `~/.openclaw/braids/config.edn` for the `:orchestrator-channel` field. If set, post orchestrator-level summaries (spawn counts, idle notifications, zombie cleanups) to that channel. If not set, skip orchestrator-level announcements (project-specific notifications still go to each project's channel).

## Steps

### 1. Gather Sessions and Run `braids orch-run`

Call `sessions_list` to get all sessions. Then pass them to the CLI for batch processing:

```
braids orch-run --session-labels '<JSON>'
```

The `--session-labels` flag accepts a JSON array of session objects:
```json
[{"label": "project:my-project:bead-id", "status": "running", "ageSeconds": 120}]
```

This does everything in one CLI call: loads projects, computes spawn decisions, **and** detects zombies (batch bead status checks via `bd list` instead of individual `bd show` per session). This keeps the orchestrator fast even with many active projects.

The output JSON has one of two shapes:

**Spawn result** (each entry is ready for `sessions_spawn`):
```json
{
  "action": "spawn",
  "spawns": [
    {
      "task": "You are a project worker...",
      "label": "project:my-project:my-project-abc",
      "runTimeoutSeconds": 1800,
      "cleanup": "delete",
      "thinking": "low",
      "agentId": "scrapper"
    }
  ],
  "zombies": [
    {"slug": "my-project", "bead": "my-project-old", "label": "project:my-project:my-project-old", "reason": "session-ended"}
  ]
}
```

The `zombies` array (if present) lists sessions to clean up. Reasons: `session-ended` (completed/failed/stopped), `bead-closed`, `timeout`.

**Idle result:**
```json
{
  "action": "idle",
  "reason": "no-active-iterations",
  "disable_cron": true
}
```

Possible idle reasons: `no-active-iterations`, `no-ready-beads`, `all-at-capacity`.

Both shapes may include a `zombies` array.

### 2. Clean Up Zombies

If the output includes a `zombies` array, for each zombie:
1. Kill the session via `sessions_kill` using the zombie's `label`
2. If `blocker` notifications are enabled for the project, notify the **project's** channel: `"🧟 Cleaned up zombie worker session for <bead-id> (reason: <reason>)"`
3. If an orchestrator channel is configured, also post a brief summary there

### 3. Spawn Workers

For each entry in the `spawns` array, call `sessions_spawn` directly with the fields from the JSON:

```
sessions_spawn(
  task: <task>,
  label: <label>,
  runTimeoutSeconds: <runTimeoutSeconds>,
  cleanup: <cleanup>,
  thinking: <thinking>,
  agentId: <agentId>  # if present in spawn entry
)
```

No additional processing needed — the JSON entries map 1:1 to `sessions_spawn` parameters.

### 4. Self-Disable on Idle

If the result includes `"disable_cron": true`, disable the orchestrator cron job:

1. Look up the cron job ID: `openclaw cron list --json`, find the job named `braids-orchestrator`, then run `openclaw cron disable <job-id>` (keeps the job definition intact)
2. Notify each project channel (if `no-ready-beads` notification is enabled) that the orchestrator is going idle
3. If an orchestrator channel is configured, post the idle reason there
4. The orchestrator will not run again until re-enabled

This ensures **zero token usage** during idle periods. To re-activate: `openclaw cron enable <job-id>` (look up the ID via `openclaw cron list --json`).

### 5. Done

Do not do any bead work yourself. Just spawn and exit.

## Troubleshooting

### Context Overflow

If this cron session hits "context overflow", the accumulated transcript is too large. Fix:

```bash
# Delete the stale job and recreate with a fresh session
openclaw cron rm braids-orchestrator
openclaw cron add \
  --name "braids-orchestrator" \
  --every 5m \
  --session isolated \
  --message "You are the braids orchestrator. Read and follow ~/.openclaw/skills/braids/references/orchestrator.md" \
  --timeout-seconds 300 \
  --deliver-to 1476813011925598343  # orchestrator channel (update to match your config)
```
