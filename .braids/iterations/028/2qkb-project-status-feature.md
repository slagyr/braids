# Deliverable: braids-2qkb — Write project_status.feature

## Summary

Wrote `features/project_status.feature` with 6 scenarios covering `status.clj` pure logic:

1. **Dashboard includes all projects with enriched data** — verifies project count, status, iteration number, worker counts, missing iteration handling
2. **Dashboard handles missing iterations** — project with no iteration has nil iteration
3. **Project detail shows iteration progress and stories** — verifies progress fraction, percent, story ids and titles in detailed view
4. **Project detail shows no-iteration fallback** — shows "no active iteration" message
5. **Dashboard JSON output includes all project data** — JSON contains project count, status, iteration percent
6. **Dashboard handles empty registry** — returns "No projects registered."

## Files Modified

- `features/project_status.feature` (new) — 6 scenarios, ~90 lines of Gherkin

## Verification

This is a `spec` bead — only `features/*.feature` was modified. No `src/` or `spec/` files touched. The downstream implementation bead (`braids-s574`) will wire up parser, generator, and harness support.

Pre-existing test failures (2) in gherkin parser/generator file-count specs are unrelated to this bead.
