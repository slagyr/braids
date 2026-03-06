# Deliverable: braids-p9x3 — Write ready_beads.feature

## Summary

Wrote `features/ready_beads.feature` with 7 scenarios covering `ready.clj` pure logic:

1. **Filters to active projects only** — paused projects excluded from results
2. **Respects worker capacity** — project at max-workers returns no beads
3. **Returns beads when under capacity** — project below max-workers returns its beads
4. **Orders by project priority** — high before normal before low
5. **Skips project paused in config** — registry active but config paused is excluded
6. **Format ready output shows numbered list** — formatted output contains bead id, title, project
7. **Format ready output for empty beads** — returns "No ready beads."

## Files Modified

- `features/ready_beads.feature` (new) — 7 scenarios, ~90 lines of Gherkin

## Verification

This is a `spec` bead — only `features/*.feature` was modified. No `src/` or `spec/` files touched. The downstream implementation bead (`braids-enh9`) will wire up parser, generator, and harness support.

Pre-existing test failures (2) in gherkin parser/generator file-count specs are unrelated to this bead.
