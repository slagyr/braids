# Migrate iterations/ to .braids/iterations/ (braids-o6o)

## Summary

Verified that the iterations directory migration from `iterations/` to `.braids/iterations/` is already complete for the projects-skill project. All specs and reference docs already use the `.braids/iterations/` path. No hardcoded references to the old `iterations/` location remain (except in `migration.md` which documents the change — correct behavior).

## Details

### What was checked

1. **Physical directory**: `iterations/` does not exist at project root. All iteration directories (001-007) are under `.braids/iterations/`.
2. **Spec files**: All 5 spec files (`simulation_spec.clj`, `contracts_spec.clj`, `integration_smoke_spec.clj`, `structural_spec.clj`, `init_reference_spec.clj`) use `.braids/iterations/` paths.
3. **Reference docs**: All references under `projects/references/` use `.braids/iterations/` paths.
4. **SKILL.md and CONTRACTS.md**: Already reference `.braids/iterations/`.
5. **Fallback logic**: `integration_smoke_spec.clj` has `resolve-iterations-dir` that checks `.braids/iterations` first, falls back to `iterations/` — this is correct backward-compatibility behavior.

### Cleanup note

Found an empty `4` directory under `.braids/iterations/` (not zero-padded). This appears to be leftover from an earlier issue but is harmless — the spec's `\d{3}` regex filter skips it.

### Pre-existing test failures

6 test failures exist but are unrelated to this migration — they involve data issues in other projects (wealth, zaap) and carried-over beads in iteration 002 whose deliverables live in iteration 001.
