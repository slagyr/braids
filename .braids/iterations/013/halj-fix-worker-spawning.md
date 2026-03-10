# braids-halj: Fix Worker Spawning — Use Isolated Cron Sessions

## Problem

Workers spawned via `openclaw agent --agent scrapper --message <task> --session-id braids-XXX-worker` ended up in Discord channel context. The root cause:

- `--session-id` sets the session **ID** but not the session **KEY**
- DM sessions collapse to `agent:<agentId>:main` regardless of `--session-id`
- The scrapper's main session (`agent:scrapper:main`) had `deliveryContext.channel: "discord"` and `lastChannel: "discord"` from prior Discord interactions
- Scrapper saw the task as a Discord group message, applied group chat NO_REPLY rules
- Bead never got claimed → orchestrator re-spawned every 5 minutes forever

## Solution

Changed worker spawning from `openclaw agent` to `openclaw cron add` with isolated session keys:

### Key changes:

1. **`src/braids/orch_runner.clj`** — `build-worker-args` now produces `cron add` CLI args:
   - `--session-key agent:<agent>:braids-<bead>-worker` — creates a fresh, isolated session in the agent's store with NO inherited Discord context
   - `--at +0s` — fires immediately  
   - `--delete-after-run` — auto-cleans the cron job after completion
   - `--timeout-seconds` — worker timeout (was `--timeout`)
   - New helper `build-worker-session-key` constructs the isolated key

2. **`src/braids/orch.clj`** — Added `parse-worker-session-key` to extract bead-id from session keys like `agent:scrapper:braids-halj-worker`

3. **`src/braids/orch_io.clj`** — `load-sessions-from-stores` now also detects workers by session key pattern (third `cond` branch), not just session-id or project label

4. **`src/braids/features/harness.clj`** — Updated `build-worker-args!` to extract `--session-key` instead of `--session-id`

### Spec updates:

- `spec/braids/orch_runner_spec.clj` — Updated for `cron add` args, `--session-key`, `--timeout-seconds`, `build-worker-session-key`
- `spec/braids/orch_runner_io_spec.clj` — Updated to verify `cron add` subcommand
- `spec/braids/orch_spec.clj` — Added specs for `parse-worker-session-key`
- `spec/braids/features/orch_runner_spec.clj` — Updated feature spec for new arg format
- `spec/braids/features/harness_spec.clj` — Updated harness spec for `--session-key`

## Why cron add?

| Approach | Verdict |
|---|---|
| `openclaw agent --session-id` | ❌ Session key collapses to `agent:<id>:main` — inherits Discord context |
| `openclaw agent --local` | ❌ No gateway connection → no `message` tool for notifications |
| `openclaw agent --channel <X>` | ❌ Changes main session's delivery context, affecting all future interactions |
| `openclaw cron add --session-key` | ✅ Creates truly isolated session key, agent runs through gateway with full tools |

## Test Results

1000 examples, 0 new failures (4 pre-existing failures from missing feature EDN files unrelated to this change).

## Files Changed

- `src/braids/orch_runner.clj` — `build-worker-args`, `build-worker-session-key`
- `src/braids/orch.clj` — `parse-worker-session-key`
- `src/braids/orch_io.clj` — `load-sessions-from-stores` (session-key detection branch)
- `src/braids/features/harness.clj` — `build-worker-args!`
- `spec/braids/orch_runner_spec.clj`
- `spec/braids/orch_runner_io_spec.clj`
- `spec/braids/orch_spec.clj`
- `spec/braids/features/orch_runner_spec.clj`
- `spec/braids/features/harness_spec.clj`
