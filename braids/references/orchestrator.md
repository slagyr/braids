# Projects Orchestrator

The braids orchestrator is a **shell script** (`braids/bin/braids-orch.sh`) that runs on a cron schedule.
It does NOT use an LLM agent — it's pure shell logic for zero reasoning overhead and true fire-and-forget spawning.

## What It Does

1. Runs `braids orch-tick` to get spawn decisions (JSON)
2. For each spawn, fires `openclaw agent` in the background (`&`)
3. Handles idle/disable-cron scenarios
4. Logs to `/tmp/braids-orch.log` (configurable via `BRAIDS_ORCH_LOG`)
5. Exits immediately — no waiting for workers

## Installation

The script is installed by `braids init` or manually:

```bash
# Install or update the script
cp braids/bin/braids-orch.sh /usr/local/bin/braids-orch
chmod +x /usr/local/bin/braids-orch
```

## Cron Setup

```bash
# Create the cron job (runs every 5 minutes)
openclaw cron add \
  --name braids-orchestrator \
  --every 5m \
  --message "Run braids-orch" \
  --timeout-seconds 60

# Or update existing agent-based cron to use the script:
openclaw cron edit <job-id> \
  --message "Run braids-orch" \
  --timeout-seconds 60
```

**Note:** The cron job still uses an `agentTurn` payload because OpenClaw cron only supports agent turns.
The agent's only job is to execute the shell script — one `exec` tool call vs. the old approach of
reading orchestrator.md, parsing JSON, constructing prompts, and making multiple `sessions_spawn` calls.

Alternatively, the shell script can be run directly from system cron or launchd:

```bash
# System crontab (bypass OpenClaw cron entirely)
*/5 * * * * /usr/local/bin/braids-orch >> /tmp/braids-orch.log 2>&1
```

## Usage

```bash
braids-orch             # Normal run
braids-orch --dry-run   # Log what would happen without spawning
braids-orch --verbose   # Also print to stderr
```

## Environment

- `BRAIDS_ORCH_LOG` — Log file path (default: `/tmp/braids-orch.log`)

## How Spawning Works

For each spawn entry from `braids orch-tick`, the script runs:

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

The `&` makes it fire-and-forget. The script doesn't wait for workers to complete.

## Limitations vs. LLM Orchestrator

- **No session labels:** `openclaw agent` doesn't support `--label`, so zombie detection
  based on session labels won't work for sessions spawned by the shell script.
  The `braids orch-tick` command still reads session stores directly for zombie detection.
- **No session cleanup:** Can't kill zombie sessions from the CLI (no `sessions kill` command).
  Zombies are logged but not cleaned up automatically.
- **No channel notifications for zombies:** The shell script logs zombies but doesn't send
  Discord messages about them (would require the message CLI or an agent).

## Troubleshooting

Check the log:
```bash
tail -f /tmp/braids-orch.log
```

Test with dry-run:
```bash
braids-orch --dry-run --verbose
```

Check what orch-tick would return:
```bash
braids orch-tick
```
