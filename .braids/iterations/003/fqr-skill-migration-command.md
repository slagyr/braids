# Support skill migration command via project channel (braids-fqr)

## Summary
Added user-triggered skill migration support to the projects skill. Users can request format updates via the project's Channel, and the agent will diff current files against the canonical SKILL.md format and apply/report changes.

## Details

### Changes Made

1. **`SKILL.md`** — Added "Skill Migration" section documenting the feature: trigger phrases, behavior, and pointer to the reference doc.

2. **`references/migration.md`** — New reference file with the full migration procedure:
   - Read canonical format from SKILL.md
   - Read project's existing files
   - Diff and identify gaps
   - Apply changes (respecting Autonomy setting) or report for approval
   - Edge cases: immutable completed iterations, custom field preservation, conflict handling
   - Example Channel notification format

### Design Decisions

- **User-triggered only** — No automatic migration. User must explicitly request it via Channel.
- **Additive by default** — Migrations add missing fields with sensible defaults; they never remove or rename existing values without flagging.
- **Autonomy-aware** — Full autonomy projects get auto-applied migrations; ask-first projects get a report and wait for approval.
- **Immutable completed iterations** — Migration never touches completed iteration files, only notes differences.
