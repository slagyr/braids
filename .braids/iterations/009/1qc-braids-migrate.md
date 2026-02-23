# braids migrate: migrate existing installs (braids-1qc)

## Summary

Implemented the `braids migrate` CLI command that converts markdown-based configs (registry.md, PROJECT.md) to EDN format (registry.edn, project.edn).

## What was built

### Pure logic (`src/braids/migration.clj` — extended)
- `plan-migration` — pure function that takes injectable IO fns (`read-file`, `file-exists?`) and returns a vector of action maps describing what to write
- `format-migration-report` — human-readable summary of planned/executed actions
- Skips files that already exist in EDN format (idempotent)
- Handles both `.braids/PROJECT.md` and root `PROJECT.md` fallback paths

### IO layer (`src/braids/migration_io.clj`)
- `run-migrate` — executes the migration plan, writing EDN files to disk
- Supports `--dry-run` flag to preview without writing

### CLI integration (`src/braids/core.clj`)
- Added `migrate` command: `braids migrate [--dry-run]`

### Tests (`spec/braids/migrate_command_spec.clj`)
- Round-trip tests for registry and project config (md → edn → parse)
- Plan-migration tests: registry migration, project.edn migration, skip-when-exists
- Report formatting tests

## Design decisions
- **Pure/IO separation**: `plan-migration` is fully testable with injected file operations
- **Idempotent**: Won't overwrite existing EDN files
- **No destructive action**: Original .md files are left in place
