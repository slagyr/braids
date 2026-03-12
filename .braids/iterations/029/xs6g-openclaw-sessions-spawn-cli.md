# Deliverable: `openclaw sessions spawn` CLI Subcommand

## Summary

Added `openclaw sessions spawn` — a new CLI subcommand that creates truly isolated agent sessions. This replaces the pattern of shelling out to `openclaw agent --agent X --session-id Y` which failed to isolate sessions (all workers landed in `agent:X:main` and picked up the Discord deliveryContext, causing NO_REPLY loops).

## Command Syntax

```
openclaw sessions spawn [options]

Options:
  --agent <id>         Agent id to run (required)
  --task <text>        Task / message to send to the agent (required)
  --model <model>      Model override (e.g. anthropic/claude-opus-4-20250514)
  --thinking <level>   Thinking level: off | low | medium | high
  --timeout <seconds>  Agent timeout in seconds (default: 600)
  --label <label>      Session label for identification
  --json               Output JSON instead of just the session key
```

### Examples

```bash
# Basic spawn
openclaw sessions spawn --agent main --task "Summarize recent emails"

# With model + thinking override
openclaw sessions spawn --agent main --task "Build feature X" \
  --model anthropic/claude-opus-4-20250514 --thinking high

# With timeout and label
openclaw sessions spawn --agent main --task "Quick check" \
  --timeout 120 --label "health-check"

# JSON output for programmatic use
openclaw sessions spawn --agent main --task "Generate report" --json
```

## How It Works Internally

1. **Session key generation**: Creates `agent:<agentId>:subagent:<uuid>` using `crypto.randomUUID()` — this is the same format used by the `sessions_spawn` tool internally, ensuring proper isolation.

2. **System prompt**: Builds a minimal subagent system prompt that includes the task context and behavioral rules (stay focused, be ephemeral, etc.).

3. **Gateway call**: Calls the gateway with:
   - `method: "agent"` 
   - `lane: "subagent"` (CommandLane.Subagent) — prevents the session from being treated as a main session
   - `deliver: false` — no delivery back to any channel
   - `expectFinal: false` — fire-and-forget; doesn't wait for agent completion
   - Session key, message, label, thinking, timeout, model passed through

4. **Output**: Prints the session key to stdout (plain text by default, JSON with `--json`).

## Files Changed

- `src/commands/sessions-spawn.ts` — New command implementation
- `src/cli/program/register.status-health-sessions.ts` — Wired `spawn` subcommand into the existing `sessions` command alongside `cleanup`

## Caveats

- **Fire-and-forget**: The command dispatches to the gateway and returns immediately. It does not wait for the agent to complete. Use `openclaw sessions` to check status.
- **No parent context**: Unlike the `sessions_spawn` tool (used by agents), this CLI version does not pass parent session context (channel, accountId, threadId, groupId). The spawned session is fully standalone.
- **No spawn depth enforcement**: The CLI bypasses the spawn-depth limiter since there's no parent session to measure depth from. The gateway itself may still enforce limits.
- **Agent validation**: The agent ID must exist in the config (`openclaw agents list`). Invalid IDs are rejected before the gateway call.

## Commit

`feat(cli): add sessions spawn subcommand for isolated agent sessions` on the `main` branch of `~/openclaw`.
