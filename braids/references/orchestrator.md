# Projects Orchestrator

The braids orchestrator runs via `braids orch` — a CLI command that scans projects,
computes spawn decisions, and optionally starts workers.

## What It Does

1. Scans the registry for active projects with active iterations
2. Checks worker concurrency (MaxWorkers) and ready beads
3. Detects zombie sessions (closed beads, timed-out workers)
4. Prints a human-readable summary to stdout
5. In `--run` mode, spawns workers via `openclaw agent`

## Usage

```bash
braids orch              # Dry-run: show what would happen (default)
braids orch --run        # Actually spawn workers
braids orch --verbose    # Print detailed output
braids orch --help       # Show help
```

All output goes to stdout for easy piping and logging.

## Cron Setup

Use system cron or launchd to run the orchestrator periodically, appending output to a log:

```bash
# System crontab — runs every 5 minutes, appends to /tmp/braids.log
*/5 * * * * /usr/local/bin/braids orch --run >> /tmp/braids.log 2>&1
```

Or via OpenClaw cron:

```bash
openclaw cron add \
  --name braids-orchestrator \
  --every 5m \
  --message "Run: braids orch --run >> /tmp/braids.log 2>&1" \
  --timeout-seconds 60
```

## How Spawning Works

For each spawn entry, the orchestrator runs:

```bash
openclaw agent \
  --agent <agentId> \
  --message "<worker prompt>" \
  --session-id <uuid> \
  --thinking <level> \
  --timeout <seconds> \
  --deliver \
  --reply-channel discord \
  --reply-to <channel> &
```

Workers are fire-and-forget — the orchestrator doesn't wait for them to complete.

## Zombie Detection

Zombies are detected using these criteria:

1. **Non-running status** — sessions with status `completed`, `failed`, `error`, or `stopped`
2. **Closed bead** — the bead is already closed but the session is still running
3. **Excessive runtime** — sessions exceeding the project's `worker-timeout`

Zombies are logged to stdout but not automatically cleaned up from the CLI.

## Troubleshooting

Check the log:
```bash
tail -f /tmp/braids.log
```

Test with dry-run (default):
```bash
braids orch
```

Check what's ready:
```bash
braids ready
```
