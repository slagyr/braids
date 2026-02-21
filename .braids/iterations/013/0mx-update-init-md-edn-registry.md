# Update init.md for EDN Registry Format (projects-skill-0mx)

## Changes

### `braids/references/init.md`
- Changed registry creation from `registry.md` (markdown table) to `registry.edn` with `{:projects []}` EDN format
- Updated all references from `registry.md` → `registry.edn`
- Changed cron job message from "projects orchestrator" to "braids orchestrator"
- Updated verification checklist to reference `registry.edn`

### `spec/init_reference_spec.clj`
- Added test: `references registry.edn (not registry.md)` — ensures no `registry.md` references remain
- Added test: `uses EDN {:projects []} format for registry` — validates correct EDN format
- Added test: `cron message says 'braids orchestrator' not 'projects orchestrator'` — validates updated cron message

## Acceptance Criteria — All Met
- ✅ registry.edn format instead of registry.md
- ✅ Uses EDN `{:projects []}` instead of markdown table
- ✅ All references `registry.md` → `registry.edn`
- ✅ Cron job message: 'projects orchestrator' → 'braids orchestrator'
