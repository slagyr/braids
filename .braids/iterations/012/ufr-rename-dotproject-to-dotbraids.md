# Rename .project directory to .braids (braids-ufr)

## Summary

Renamed the `.project/` directory to `.braids/` across the entire codebase — directory structure, source code, specs, skill docs, reference docs, and contracts.

## Changes

### Directory Rename
- `git mv .project .braids` — physical directory rename in this project

### Source Code (with backward compatibility)
- `src/braids/ready_io.clj` — Config loading checks `.braids/` first, falls back to `.project/`, then root
- `src/braids/orch_io.clj` — Active iteration scanner checks `.braids/iterations/` first, falls back to `.project/iterations/`
- `src/braids/status_io.clj` — Status dashboard resolves iteration dir with fallback
- `src/braids/iteration_io.clj` — Iteration display resolves dir with fallback
- `src/braids/new_io.clj` — New project creation uses `.braids/`
- `src/braids/new.clj` — AGENTS.md template references `.braids/`
- `src/braids/migration.clj` — Migration checks both `.braids/` and `.project/` paths

### Skill Docs & References
- `braids/SKILL.md` — All `.project/` → `.braids/`
- `braids/references/worker.md` — All `.project/` → `.braids/`
- `braids/references/orchestrator.md` — (no changes needed)
- `braids/references/agents-template.md` — All `.project/` → `.braids/`
- `braids/references/project-creation.md` — All `.project/` → `.braids/`
- `braids/references/init.md` — All `.project/` → `.braids/`
- `braids/references/status-dashboard.md` — All `.project/` → `.braids/`
- `CONTRACTS.md` — All `.project/` → `.braids/`
- `AGENTS.md` — All `.project/` → `.braids/`

### Specs
- `spec/rename_spec.clj` — Added 3 new tests for `.braids` directory existence, `.project` absence, and no stale `.project/` references; plus a test verifying `.braids/` appears before `.project/` in fallback chains
- `spec/structural_spec.clj` — Registry validation checks `.braids/`, `.project/`, and root (3-tier fallback)
- `spec/integration_smoke_spec.clj` — Integration helpers resolve iterations/config with 3-tier fallback
- `spec/simulation_spec.clj` — All `.project/` → `.braids/`
- `spec/braids/new_io_spec.clj` — All `.project/` → `.braids/`
- `spec/braids/orch_io_spec.clj` — All `.project/` → `.braids/`
- `spec/braids/migrate_command_spec.clj` — All `.project/` → `.braids/`

## Backward Compatibility

All IO code that reads project config or iteration data now uses a 3-tier fallback:
1. `.braids/` (new canonical location)
2. `.project/` (previous location — for projects not yet migrated)
3. Root-level (legacy — pre-iteration-006)

This ensures existing projects using `.project/` continue to work until they are migrated.

## Test Results

All rename-specific tests pass. Pre-existing failures (homebrew formula, symlink, registry, install.sh, cross-project integration) are unaffected by this change.
