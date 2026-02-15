# Structural Test Script (projects-skill-rin)

## Summary

Created `tests/run.sh` (test runner) and `tests/test_structural.sh` (structural validation) that validate the projects skill file structure, symlinks, registry, and spawn config across all registered projects.

## Details

### Test Runner (`tests/run.sh`)
- Discovers and runs all `test_*.sh` files in the tests directory
- Reports pass/fail counts and lists failed tests
- Exits non-zero if any test fails

### Structural Tests (`tests/test_structural.sh`)
Validates the following categories:

**Skill Symlink**
- Symlink exists at `~/.openclaw/skills/projects`
- Target is a valid directory
- Points to the correct source (inode-level comparison for case-insensitive filesystem compatibility)

**Skill Directory Contents**
- SKILL.md exists with valid YAML frontmatter (name + description fields)
- All required reference files present: orchestrator.md, worker.md, agents-template.md, status-dashboard.md, migration.md

**Registry**
- registry.md exists with correct table header
- Each registered project validated for: directory exists, PROJECT.md, AGENTS.md, .beads/, iterations/, git repo
- Registry status and priority values are valid enums

**Per-Project Validation**
- PROJECT.md has Status, Goal, and Guardrails sections
- Iteration directories follow NNN naming convention
- ITERATION.md has Status and Stories sections
- Iteration status values are valid (planning/active/complete)
- Completed iterations have RETRO.md
- Active iteration bead references exist in the bd tracker

**Spawn Config**
- Orchestrator cron job exists
- Active projects have valid MaxWorkers (positive integer)
- Active projects have Channel set for notifications

### Issues Found by Initial Run
The tests correctly identified several pre-existing issues:
1. `iterations/4` — stray directory with wrong naming convention (should be `004`)
2. `iterations/001` and `002` — completed without RETRO.md (pre-dates the convention)
3. `wealth` PROJECT.md — missing Status, Goal, and Guardrails sections (format tolerance applies at runtime, but structural tests flag it)
4. `wealth/iterations/001` — completed without RETRO.md

These are legitimate findings, not test bugs.

## Assets
- `tests/run.sh` — test runner
- `tests/test_structural.sh` — structural validation tests
