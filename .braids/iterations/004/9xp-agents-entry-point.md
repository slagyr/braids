# AGENTS.md as Universal Entry Point (braids-9xp)

## Summary
Redesigned project-level `AGENTS.md` to serve as a universal entry point for any agent landing in a project repo, whether spawned by the orchestrator or arriving independently.

## Changes

### New AGENTS.md template (`references/agents-template.md`)
- Routes orchestrator-spawned workers directly to `worker.md`
- Provides a self-service path for independent agents: read PROJECT.md → find active iteration → `bd ready` → follow worker.md
- Includes bd quick reference and session completion checklist
- Replaces the old generic bd-only AGENTS.md

### SKILL.md updates
- Added `AGENTS.md` to directory layout documentation
- Added AGENTS.md creation as step 4 in project creation workflow
- Added AGENTS.md Template section pointing to `references/agents-template.md`

### worker.md updates
- Added step 3 in Load Context: read the project's own `AGENTS.md` (with note to skip if already read)

### Applied to all active projects
- Updated AGENTS.md in: projects-skill, zane-setup, wealth, imsg

## Key Decisions
- Template lives as a standalone reference file (`references/agents-template.md`) rather than inline in SKILL.md to avoid markdown nesting issues
- The AGENTS.md is intentionally concise — it routes to worker.md rather than duplicating its content
- Non-orchestrator agents follow the same worker.md workflow, just with a self-service discovery step
