# Projects Orchestrator

You are the braids orchestrator. You do NOT perform bead work — you spawn workers that do.

## Steps

### 1. Gather Session Labels

Call `sessions_list` and collect all session labels. You'll need these for zombie detection and to pass to the CLI.

### 2. Detect and Clean Up Zombies

From the session list, identify zombie sessions — worker sessions that should no longer count toward concurrency:

1. **Session not running** — status is `completed`, `failed`, `error`, `stopped`, etc.
2. **Bead already closed** — the bead id in the label (`project:<slug>:<bead-id>`) is closed (check via `bd show <bead-id>`)
3. **Excessive runtime** — running longer than the project's WorkerTimeout (default 1800s)

Always check bead status before applying runtime threshold. A long-running session on an open bead is probably still working.

For each zombie: exclude from worker count, kill the session via `sessions_kill`, and notify the project channel if `blocker` notifications are enabled: `"🧟 Cleaned up zombie worker session for <bead-id>"`

### 3. Run `braids orch-tick`

Run the CLI command:

```
braids orch-tick
```

This handles all the heavy lifting: loading the registry, finding active iterations, checking project configs, loading ready beads, and computing spawn decisions. It outputs JSON:

**Spawn result:**
```json
{
  "action": "spawn",
  "spawns": [
    {
      "project": "my-project",
      "bead": "my-project-abc",
      "iteration": "008",
      "channel": "123456",
      "path": "~/Projects/my-project",
      "label": "project:my-project:my-project-abc",
      "worker_timeout": 1800
    }
  ]
}
```

**Idle result:**
```json
{
  "action": "idle",
  "reason": "no-active-iterations"
}
```

Possible idle reasons: `no-active-iterations`, `no-ready-beads`, `all-at-capacity`.

> **Note:** The CLI does not yet accept session labels as input, so worker counts default to 0. Zombie cleanup (Step 2) and concurrency limiting must still be handled by the orchestrator agent until the CLI supports session-label input.

### 4. Spawn Workers

For each entry in the `spawns` array:

```
sessions_spawn(
  task: "Project: <path>\nBead: <bead>\nIteration: <iteration>\nChannel: <channel>",
  label: "<label>",
  runTimeoutSeconds: <worker_timeout>,
  cleanup: "delete",
  thinking: "low"
)
```

Key spawn parameters:
- **`runTimeoutSeconds`** — hard kill after this many seconds (default 1800 = 30 min). Prevents zombie sessions from lingering indefinitely.
- **`cleanup: "delete"`** — automatically removes the session after completion. Without this, finished sessions count toward `MaxWorkers` and block new spawns.
- **`thinking: "low"`** — workers don't need deep reasoning; keeps cost and latency down.

The spawn message is intentionally minimal. The worker reads `AGENTS.md` in the project directory for full onboarding.

### 5. Self-Disable on Idle

If the tick result includes `"disable_cron": true`, the orchestrator should disable its own cron job:

1. Run `openclaw cron delete <cron-id>` to remove the orchestrator cron job
2. Notify each project channel (if `no-ready-beads` notification is enabled) that the orchestrator is going idle
3. The orchestrator will not run again until manually re-activated (e.g., when a new iteration is started)

This ensures **zero token usage** during idle periods. To re-activate, set up the cron job again (see SKILL.md § Cron Integration).

### 6. Generate Status Dashboard

Generate `~/.openclaw/braids/STATUS.md` following `references/status-dashboard.md`.

### 7. Done

Do not do any bead work yourself. Just spawn and exit.
