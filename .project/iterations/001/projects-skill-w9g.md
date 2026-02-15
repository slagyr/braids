# Extract worker orchestration into projects/references/worker.md (projects-skill-w9g)

## Summary
Moved all worker orchestration logic from the cron prompt into `references/worker.md` within the projects skill. The cron prompt now simply reads: "Read and follow ~/.openclaw/skills/projects/references/worker.md".

## Details
- Created `~/.openclaw/skills/projects/references/worker.md` containing the full worker procedure: registry check, concurrency gate (MaxAgents), work prioritization (iteration order then bead priority), task execution flow, and notification rules.
- Updated the `projects-worker` cron job payload to reference the new file instead of inlining the entire orchestration procedure.
- Symlinked from `~/projects/projects-skill/projects/references/worker.md` → the skill copy for single source of truth.
- This supersedes `projects-skill-a2z` (agent strategy docs) since the orchestration is now documented in the reference file.

## Assets
- `~/.openclaw/skills/projects/references/worker.md` — the worker orchestration reference
