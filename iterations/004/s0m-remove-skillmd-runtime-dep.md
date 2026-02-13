# Remove SKILL.md Runtime Dependency (projects-skill-s0m)

## Summary
Removed all SKILL.md references from `orchestrator.md` and `worker.md` so they are fully self-contained at runtime.

## Details
Both `references/worker.md` (step 1, format tolerance note) and `references/orchestrator.md` (step 3, concurrency check) contained a reference to "see SKILL.md §Format Compatibility" for default field values. This created an unnecessary runtime dependency — workers and orchestrators had to read SKILL.md just to know the defaults.

**Fix:** Inlined the key defaults directly into each file:
- `MaxWorkers` → 1
- `Autonomy` → full
- `Priority` → normal
- `Checkin` → on-demand
- `Channel` → none (skip notifications)
- `Notifications` table → all events on
- Unknown fields → ignore

This saves a file read per worker/orchestrator invocation and makes the reference docs self-contained.

## Files Changed
- `~/.openclaw/skills/projects/references/worker.md` — inlined format tolerance defaults
- `~/.openclaw/skills/projects/references/orchestrator.md` — inlined format tolerance defaults
