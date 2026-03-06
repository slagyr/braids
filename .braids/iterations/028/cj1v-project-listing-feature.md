# Deliverable: braids-cj1v — Write project_listing.feature

## Summary

Wrote `features/project_listing.feature` with 5 scenarios covering the pure logic in `list.clj`:

1. **List shows projects with all columns populated** — verifies all column headers appear, slugs are listed, iteration numbers, progress percentages, and worker counts are rendered correctly.
2. **List shows dash placeholders for missing data** — verifies that projects without iteration data show dash characters for iteration and progress columns.
3. **List handles empty registry** — verifies the "No projects registered." message for empty/nil project lists.
4. **List colorizes status and priority** — verifies ANSI color coding: active=green, paused=yellow for status; high=red, low=yellow for priority; 100% progress=green.
5. **List JSON output includes all project data** — verifies JSON output contains slug, status, priority, iteration number, workers, and max_workers fields.

## Files Modified

- `features/project_listing.feature` (new) — 5 scenarios, ~50 lines of Gherkin

## Verification

This is a `spec` bead — the feature file defines acceptance criteria only. No `src/` or `spec/` files were modified. The implementation bead (`braids-5ti2`) will add parser patterns, generator registry entries, and harness functions to make the pipeline produce executable specs from this feature file.

Pre-existing test failures (2) related to `gherkin_spec.clj` and `gherkin_generator_spec.clj` counting feature files — these were already broken by the previously-staged `project_lifecycle.feature` and are not caused by this bead.
