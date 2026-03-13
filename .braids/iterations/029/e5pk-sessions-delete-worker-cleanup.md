# braids-e5pk: Invalidate worker session on completion

## Problem

After `openclaw sessions spawn` creates a worker session with a `project:` label, the session entry persists in `~/.openclaw/agents/<agent>/sessions/sessions.json` after the worker completes. The braids orchestrator counts any entry with a `project:` label as an active worker, so completed sessions accumulate as zombie workers.

## Solution

### Part 1: `openclaw sessions delete` CLI subcommand

**File:** `~/openclaw/src/commands/sessions-delete.ts`

New subcommand that removes session entries by label or key:

- `--label <label>` — removes all sessions matching that label across all agent stores
- `--session-key <key>` — removes a specific session entry by key
- At least one option is required
- Scans all `~/.openclaw/agents/*/sessions/sessions.json` files
- Removes matching entries and writes back the file
- Reports count of removed entries

**Wired in:** `~/openclaw/src/cli/program/register.status-health-sessions.ts` as `sessionsCmd.command("delete")`

**Usage:**
```bash
openclaw sessions delete --label project:braids:braids-abc
openclaw sessions delete --session-key "agent:main:subagent:abc-123"
```

### Part 2: Worker template update

**File:** `~/.openclaw/skills/braids/references/worker.md`

Added step 7.3 in the "Close the Bead" section:

```bash
openclaw sessions delete --label project:<slug>:<bead-id>
```

This runs after `bd update <bead-id> -s closed` and the git commit, cleaning up the worker's session entry before sending the completion notification.

## Commits

- `~/openclaw`: `feat(cli): add sessions delete subcommand for worker session cleanup` (71a4ba80a3)
- `~/.openclaw/skills/braids`: `docs: add session cleanup step to worker template` (d8b3eb5)

## Testing

```
$ openclaw sessions delete --help
# Shows usage with --label and --session-key options

$ openclaw sessions delete --label project:braids:braids-test
Removed 0 sessions.
# Correctly reports 0 when no matching sessions exist
```
