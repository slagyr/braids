# Remove braids migrate command (projects-skill-0m5)

## Summary

Removed the `migrate` CLI command and all related source/spec files. Migration is now agent-only when needed (via `references/migration.md`).

## Changes

### Deleted files
- `src/braids/migration.clj` — migration logic
- `src/braids/migration_io.clj` — migration I/O
- `spec/braids/migration_spec.clj` — migration specs
- `spec/braids/migrate_command_spec.clj` — migrate command specs

### Modified files
- `src/braids/core.clj` — removed migrate command from dispatch, help, and require
- `src/braids/iteration.clj` — removed legacy markdown parsing functions (only used by migration)
- `spec/braids/iteration_spec.clj` — removed legacy parse/migrate specs
- `spec/structural_spec.clj` — no changes needed (migration.md reference kept)
- `spec/rename_spec.clj` — updated exclusion for migration.md `.project/` references

### Preserved
- `braids/references/migration.md` — kept as agent-only migration reference

## Test Results

All specs pass (12 pre-existing failures in integration/homebrew tests unrelated to this change).
