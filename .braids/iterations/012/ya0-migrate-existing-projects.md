# Migrate Existing Projects to New Format (braids-ya0)

## Summary

Added cleanup of old markdown/legacy files after migration to EDN format. Fixed a paren-balancing bug in `migration.clj` that caused `plan-migration` to return `nil`.

## Changes

### Bug Fix: `plan-migration` returning nil
- `src/braids/migration.clj` had a missing closing paren — the `doseq` body wasn't properly closed, causing `@actions` to be evaluated inside `doseq` (discarded) instead of as the `let` return value.

### Feature: Delete old files after migration
- `plan-migration` now emits `:delete-file` actions for source files after creating their EDN replacements:
  - `registry.md` → deleted after `registry.edn` is written
  - `PROJECT.md` (in `.braids/` or `.project/`) → deleted after `config.edn` is written
  - Legacy `project.edn` → deleted after `config.edn` is written
- `migration_io.clj` executes `:delete-file` actions (using `fs/delete`)
- `format-migration-report` displays delete actions

### Tests
- 5 new specs in `migrate_command_spec.clj`:
  - Plans deletion of `PROJECT.md` after migration
  - Plans deletion of legacy `.project/PROJECT.md`
  - Plans deletion of legacy `project.edn`
  - Plans deletion of `registry.md`
  - Includes delete-file in migration report

All new tests pass. 8 pre-existing failures remain (unrelated: defaults mismatch, repo rename references, integration checks).
