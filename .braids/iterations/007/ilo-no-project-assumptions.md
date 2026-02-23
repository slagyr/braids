# Specs must not assume any projects exist (braids-ilo)

## Summary
Updated `structural_spec.clj` and `integration_smoke_spec.clj` to gracefully skip environment-dependent tests when no projects registry exists.

## Details

### structural_spec.clj
- **Registry describe block**: All three tests (`registry.md exists`, `has table header`, `all registered projects are valid`) now skip when no registry or projects home exists
- **Spawn Config**: `active projects have valid spawn config` now wrapped in `(when (fs/exists? registry) ...)` guard

### integration_smoke_spec.clj
- **Cross-Project Checks**: `STATUS.md exists and is fresh` and `orchestrator state is valid` now wrapped in `(when (fs/exists? registry) ...)` — these files only exist when the orchestrator has run with active projects
- Note: The per-project integration tests were already guarded by `(when (fs/exists? registry) ...)` at the top level

### Verification
- Tested with `PROJECTS_HOME=/tmp/nonexistent` — all registry/integration/cross-project tests skip cleanly (0 failures from these tests)
- Normal run preserves existing behavior — same 6 pre-existing failures (from live environment data issues, not from this bead's scope)
