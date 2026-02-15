# Replace Homegrown Test Framework with Real speclj

## Summary

Replaced the custom `spec_helper.clj` mini-framework with the real speclj library (v3.12.0). All 6 spec files rewritten to use speclj's native `describe`, `it`, `should`, `should=`, `should-contain`, `should-not`, `should-be-nil`, `before-all`, and `context` macros.

## Changes

- **bb.edn**: Added `speclj/speclj {:mvn/version "3.12.0"}` dep; updated test task to use `speclj/run-specs`
- **spec/spec_helper.clj**: Deleted (no longer needed)
- **spec/contracts_spec.clj**: Migrated to speclj; consolidated `for` loops into `doseq` inside `it` blocks
- **spec/simulation_spec.clj**: Migrated to speclj; uses `before-all` for test setup
- **spec/structural_spec.clj**: Migrated to speclj; consolidated dynamic project validation into single `it` blocks with `doseq`
- **spec/integration_smoke_spec.clj**: Migrated to speclj; same consolidation pattern
- **spec/init_reference_spec.clj**: Migrated to speclj
- **spec/project_creation_reference_spec.clj**: Migrated to speclj

## Notes

- **Java dependency**: speclj requires Java for Maven dep resolution. Installed `openjdk` via Homebrew (`brew install openjdk`). Must have `/usr/local/opt/openjdk/bin` in PATH.
- **Test results**: 143 examples, 5 failures, 212 assertions. The 5 failures are pre-existing data issues (missing deliverables in completed iterations, orphaned deliverables) — identical to the 45 failures in the old framework (consolidated into fewer `it` blocks).
- Net reduction: ~160 lines removed while gaining proper test runner output with timing.
