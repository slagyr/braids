# braids-yhv: Replace LLM Orchestrator with Shell Script

## Summary

Replaced the LLM agent orchestrator with a shell script (`braids/bin/braids-orch.sh`) that provides true fire-and-forget worker spawning with zero LLM reasoning overhead.

## What Changed

1. **New: `braids/bin/braids-orch.sh`** — Shell script that:
   - Runs `braids orch-tick` and parses JSON output with `jq`
   - For each spawn, fires `openclaw agent --agent <id> --message <task> --session-id <uuid> &` in the background
   - Handles idle scenarios including `disable_cron` (finds and disables cron job)
   - Logs zombies (can't kill sessions from CLI, but logs them)
   - Supports `--dry-run` and `--verbose` flags
   - Logs to `$BRAIDS_ORCH_LOG` (default: `/tmp/braids-orch.log`)

2. **Updated: `braids/references/orchestrator.md`** — Rewritten to document the shell script approach

3. **Updated: `install.sh`** — Installs `braids-orch` to `/usr/local/bin`

4. **Updated: Cron job** — Changed message to invoke `braids-orch` instead of reading orchestrator.md

5. **New: `spec/orch_shell_spec.clj`** — Tests for script existence, executability, argument handling, and mocked scenarios

## Verification

```
$ braids-orch --dry-run --verbose
[2026-03-03T21:50:31] orch-tick action=idle
[2026-03-03T21:50:31] Idle: reason=all-at-capacity disable_cron=false
[2026-03-03T21:50:31] Orchestrator tick complete

$ braids-orch --bogus
Unknown arg: --bogus

# Mock spawn test (with mock braids returning spawn JSON):
[2026-03-03T21:49:38] orch-tick action=spawn
[2026-03-03T21:49:38] Spawning 1 worker(s)
[2026-03-03T21:49:38] Spawning worker: bead=test-abc agent=scrapper session=33d9dcf9-...
[2026-03-03T21:49:38] All workers spawned

# Mock disable-cron test:
[2026-03-03T21:49:46] Idle: reason=no-ready-beads disable_cron=true
[2026-03-03T21:49:46] Disabling cron job abc-123
```

## Known Limitations

- **No session labels:** `openclaw agent` CLI doesn't support `--label`, so zombie detection for shell-spawned sessions relies on session store scanning by `braids orch-tick`
- **No session kill from CLI:** Zombies are logged but not automatically cleaned up
- **Still uses agent turn for cron:** OpenClaw cron only supports agent turns, so the cron triggers an agent that runs the script. This adds ~5-10s overhead but eliminates the multi-minute orchestrator reasoning
