# Deliverable: braids-oj3l — Implement project_lifecycle feature specs

## Summary

Extended the Gherkin pipeline (parser, generator, harness) to support all step patterns in `project_lifecycle.feature`. All 8 scenarios are now fully executable GREEN specs.

## Changes

### `src/braids/gherkin.clj` — 30 new step patterns
- Init prerequisites: `bd-not-available`, `bd-available`, `no-registry`, `registry-exists`, `force-not-set`, `force-set`
- Init planning: `braids-dir-not-exists`, `braids-dir-exists`, `braids-home-not-exists`, `braids-home-exists`
- Actions: `check-prerequisites`, `plan-init`, `validate-new-project`, `add-to-registry`, `build-project-config`
- New project params: `new-project-slug`, `new-project-name`, `set-name`, `set-goal`, `registry-with-project`, `new-registry-entry`
- Assertions: `assert-prereq-fail`, `assert-prereq-pass`, `assert-plan-include`, `assert-plan-not-include`, `assert-validation-fail`, `assert-should-fail`, `assert-config-value`, `assert-config-number`

### `src/braids/gherkin_generator.clj` — 30 registry entries
- Maps each IR type to `:text` and `:code` functions for generating executable speclj specs

### `src/braids/features/harness.clj` — Lifecycle helpers
- State builders: `set-bd-not-available`, `set-bd-available`, `set-no-registry`, `set-registry-exists`, etc.
- Actions: `check-prerequisites!`, `plan-init!`, `validate-new-project!`, `add-to-registry!`, `build-project-config!`
- Accessors: `prereq-errors`, `plan-actions`, `validation-errors`, `add-registry-error`, `project-config`

### `spec/braids/gherkin_spec.clj` — 29 new parser tests
- Tests for every new step pattern classification

## Verification

```
$ bb test
711 examples, 0 failures, 1148 assertions

$ bb test:features
66 examples, 0 failures, 47 assertions, 37 pending

$ bb test:all
(both suites pass)
```

The 8 project_lifecycle scenarios are all GREEN (executable). The 37 pending scenarios are from other feature files awaiting their implementation beads.
