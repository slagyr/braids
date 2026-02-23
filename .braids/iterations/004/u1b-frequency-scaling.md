# Orchestrator frequency scaling (braids-u1b)

## Summary
Added frequency scaling to the orchestrator so it reduces polling when there's no work, saving tokens and API calls.

## Details

### Problem
The orchestrator cron runs every 5 minutes regardless of whether any projects have active iterations. When all projects are paused or between iterations, this wastes tokens on redundant registry reads and status checks.

### Solution
Introduced a state file (`$PROJECTS_HOME/.orchestrator-state.json`) that tracks idle state between orchestrator runs. The orchestrator now has a "Step 0" that checks this file and exits early if the backoff interval hasn't elapsed.

**Backoff tiers:**
| Idle Reason | Backoff | Rationale |
|-------------|---------|-----------|
| No active iterations | 30 min | New iterations require human action |
| No ready beads | 15 min | Beads may unblock via external input |
| All at capacity | 10 min | Workers may finish soon |
| Work found | 0 (normal) | Resume 5-min cron cadence |

**State file format:**
```json
{
  "idleSince": "2026-02-13T08:00:00Z",
  "idleReason": "no-active-iterations",
  "lastRunAt": "2026-02-13T08:30:00Z"
}
```

### Design decisions
- **State file over cron modification**: Cron stays at 5 min; the orchestrator decides whether to actually run. This is simpler than dynamically adjusting cron schedules and self-corrects if the state file is deleted.
- **`idleSince` preserved across runs**: Tracks continuous idle duration (useful for future metrics). `lastRunAt` tracks when the last full run happened, which is what backoff is measured against.
- **Clearing idle on work found**: As soon as a worker is spawned, idle state is cleared so the next run proceeds normally.

## Files Modified
- `~/.openclaw/skills/projects/references/orchestrator.md` — Added Step 0 (frequency scaling check) and Step 7 (state file update)
- `~/.openclaw/skills/projects/SKILL.md` — Added "Orchestrator Frequency Scaling" section
