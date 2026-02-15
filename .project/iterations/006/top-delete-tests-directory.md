# Delete tests/ directory — replaced by spec/ with speclj (projects-skill-top)

## Summary

Deleted the legacy `tests/` directory which contained only `run.sh` (a shell wrapper that delegated to `bb run test`). The `spec/` directory with real speclj specs (landed in bead kln) is now the sole test location. The shell wrapper is unnecessary since `bb run test` is the canonical way to run specs.

## Changes

- **tests/**: Deleted directory and its contents (`run.sh`)
- **spec/structural_spec.clj**: Added "Legacy tests/ directory" spec verifying the old directory no longer exists

## Test Results

- 144 examples, 5 failures (pre-existing), 212 assertions
- New spec passes; no regressions introduced
