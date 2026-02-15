# Move PROJECT.md and iterations/ into .project/ directory (projects-skill-ly3)

## Summary

Moved `PROJECT.md` and `iterations/` into a `.project/` subdirectory to reduce root clutter, keeping `AGENTS.md` at root as the agent entry point convention. Updated all references across the entire skill — SKILL.md, CONTRACTS.md, all reference docs (worker.md, orchestrator.md, project-creation.md, migration.md, agents-template.md, status-dashboard.md, init.md), all spec files, and the project's own AGENTS.md. Migrated this project (projects-skill) as part of the bead.

## Changes

### Directory Structure
- **`PROJECT.md`** → **`.project/PROJECT.md`**
- **`iterations/`** → **`.project/iterations/`**
- `AGENTS.md` stays at root (agent entry point convention)

### Skill Documentation
- **SKILL.md**: Updated directory layout diagram, all path references to use `.project/` prefix, added location annotations to PROJECT.md and ITERATION.md format sections, added directory migration fallback note to format tolerance section
- **CONTRACTS.md**: Updated locations for PROJECT.md (`.project/PROJECT.md`), ITERATION.md (`.project/iterations/<N>/ITERATION.md`), deliverables (`.project/iterations/<N>/...`), and worker context loading order

### Reference Files
- **worker.md**: Updated PROJECT.md path, ITERATION.md path, deliverable output path, notifications reference
- **orchestrator.md**: Updated iteration scan glob, PROJECT.md read path, notification check, spawn timeout reference
- **project-creation.md**: Updated scaffold command (`mkdir -p .project`), PROJECT.md write path, iteration directory creation
- **migration.md**: Updated all PROJECT.md references, added breaking change documentation with migration steps
- **agents-template.md**: Updated PROJECT.md and iterations path references
- **status-dashboard.md**: Updated PROJECT.md and iteration scan paths
- **init.md**: Updated PROJECT.md reference

### Specs (test-first)
- **structural_spec.clj**: Added fallback resolution — checks `.project/PROJECT.md` first, falls back to `PROJECT.md` at root for legacy projects
- **simulation_spec.clj**: All test project setup uses `.project/` paths
- **integration_smoke_spec.clj**: Extracted `resolve-iterations-dir` and `resolve-project-md` helper functions for fallback resolution across all test sections

### Migration
- **This project (projects-skill)**: Files moved via `git mv`
- **Backwards compatibility**: Workers/orchestrators check `.project/` first, then fall back to root paths for unmigrated projects
- **Migration path**: Documented in `references/migration.md` under "Known Breaking Changes"

## Test Results

- 140 examples, 5 failures (all pre-existing), 276 assertions
- No regressions introduced
