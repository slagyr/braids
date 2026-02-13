# Deliverable: Workers must read PROJECT.md on startup (projects-skill-xrv)

## Changes

Updated `~/.openclaw/skills/projects/references/worker.md` Step 1 (Load Context):

- Made the step header explicitly **mandatory** ("do this before anything else")
- Enumerated the specific fields workers must extract from PROJECT.md: Autonomy, Guardrails, Notifications, Channel, MaxWorkers, Priority
- Added full path hints for AGENTS.md and ITERATION.md to reduce ambiguity
- Clarified that AGENTS.md provides workspace-wide conventions and safety rules
- Clarified that ITERATION.md provides iteration-level guardrails, story ordering, and notes

## Files Modified

- `~/.openclaw/skills/projects/references/worker.md` — Step 1 rewritten
