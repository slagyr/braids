# Handle skill updates gracefully across existing projects (braids-463)

## Summary

Added a format compatibility strategy so workers and orchestrators tolerate older PROJECT.md/ITERATION.md formats without failing, and documented the approach for breaking changes.

## Details

### Changes Made

1. **SKILL.md — new "Format Compatibility" section** (placed before "Skill Migration"):
   - **Worker Tolerance**: Documents all default values for missing fields (MaxWorkers→1, Autonomy→full, Priority→normal, Checkin→on-demand, Channel→skip notifications, Notifications→all on, etc.). Workers silently use defaults for missing fields and ignore unknown fields.
   - **Breaking Changes**: Documents that when tolerance isn't enough, skill maintainers should create migration beads in affected projects. Breaking changes should be rare; prefer additive changes.

2. **worker.md — "Load Context" step**: Added format tolerance note with defaults for each field and explicit instruction to never fail or block due to missing fields.

3. **orchestrator.md — "Check Concurrency" step**: Added format tolerance reminder pointing to SKILL.md §Format Compatibility.

### Design Decisions

- **Tolerance is automatic, migration is user-triggered** — workers never auto-migrate files. They just cope with what's there using defaults.
- **Old field names are treated as missing** — no complex mapping logic. The migration path handles renaming.
- **Unknown fields are preserved** — projects can have custom fields without breaking anything.
