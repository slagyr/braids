# Orchestrator should detect and clean up zombie worker sessions (projects-skill-0ox)

## Summary

Added zombie session detection and cleanup logic to the orchestrator's concurrency check. The orchestrator now identifies lingering worker sessions that should no longer count toward `MaxWorkers`, kills them, and notifies the project channel.

## Details

**Problem:** Workers that complete their bead but hit context window limits linger as sessions. The orchestrator counts them as running workers, hits the `MaxWorkers` cap, and skips the project — blocking the entire pipeline.

**Solution:** Updated `references/orchestrator.md` step 3 ("Check Concurrency") to include zombie detection before counting workers. Three detection criteria (in priority order):

1. **Non-running session status** — `completed`, `failed`, `error`, `stopped` sessions are excluded from the count
2. **Closed bead check** — if the bead referenced in the session label is already closed (`bd show`), the worker finished but the session lingered. Primary zombie signal.
3. **Runtime threshold** — sessions running >60 minutes are treated as likely zombies (only applied after checking bead status to avoid killing slow-but-working sessions)

**Cleanup actions:** Kill the session via `sessions_kill`, exclude from count, and send a `🧟` notification to the project channel.

**Files changed:**
- `~/.openclaw/skills/projects/references/orchestrator.md` — Added §Zombie Detection subsection under step 3
- `~/.openclaw/skills/projects/SKILL.md` — Added §Zombie Session Detection under Concurrency Check Flow

## Assets

None.
