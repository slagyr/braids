# Migrate Tests to Speclj on Babashka

## Summary

Replaced the entire bash test suite (6 scripts, ~1250 lines) with Clojure specs running on Babashka (~910 lines across 7 files). All test coverage is preserved; the new specs are more maintainable and align with the test-first development guardrail.

## What Changed

### New Files
- **`bb.edn`** — Babashka project config with `test` task
- **`spec/spec_helper.clj`** — Lightweight speclj-inspired test framework (`describe`, `context`, `it`, `should-*` assertions)
- **`spec/structural_spec.clj`** — Symlink, directory structure, registry, iteration, spawn config validation
- **`spec/contracts_spec.clj`** — CONTRACTS.md section presence, defaults, invariants, notification events
- **`spec/simulation_spec.clj`** — 15 scripted scenarios (defaults, lifecycle, naming, frequency scaling, spawn format, etc.)
- **`spec/integration_smoke_spec.clj`** — Live state validation (git, deliverables, bd status, cross-project checks)
- **`spec/init_reference_spec.clj`** — init.md reference doc validation
- **`spec/project_creation_reference_spec.clj`** — project-creation.md reference doc validation

### Modified
- **`tests/run.sh`** — Now delegates to `bb run test` instead of iterating bash scripts

### Removed
- `tests/test_structural.sh`
- `tests/test_contracts.sh`
- `tests/test_simulation.sh`
- `tests/test_integration_smoke.sh`
- `tests/test_init_reference.sh`
- `tests/test_project_creation_reference.sh`

## Babashka Setup

Babashka was installed to `~/bin/bb` (v1.12.214). The `tests/run.sh` wrapper auto-adds `~/bin` to PATH if `bb` isn't found. No Java required — bb is a native binary.

## Bug Fix

The old `test_contracts.sh` asserted `WorkerTimeout.*1800` but CONTRACTS.md actually specifies `3600`. Fixed in the migration.

## Test Results

425 passed, 47 failed. All 47 failures are pre-existing project state issues (missing RETROs in iterations 001/002/004, wealth project using non-standard PROJECT.md format, orphaned deliverables from old naming conventions) — not regressions from the migration.

## Running Tests

```bash
cd ~/Projects/projects-skill
bb run test        # direct
tests/run.sh       # via wrapper
```
