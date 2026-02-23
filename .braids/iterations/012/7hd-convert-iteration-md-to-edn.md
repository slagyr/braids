# Convert ITERATION.md to iteration.edn (braids-7hd)

## Summary

Replaced ITERATION.md with iteration.edn across the entire codebase. All iteration state is now stored as EDN — no markdown config files remain.

## Changes

### New EDN Format

```edn
{:number "012"
 :status :active
 :stories [{:id "bead-id" :title "Story title"} ...]
 :guardrails ["constraint"]
 :notes ["context"]}
```

Required keys: `:number`, `:status`, `:stories`. Defaults: `:guardrails` → `[]`, `:notes` → `[]`.

### Source Changes

- **src/braids/iteration.clj** — Added `parse-iteration-edn`, `validate-iteration`, `iteration->edn-string`, `migrate-iteration-md`, plus legacy MD parsing helpers for migration. Removed `cheshire.core` dependency in favor of `clojure.edn`.
- **src/braids/orch_io.clj** — `find-active-iteration` now reads `iteration.edn` instead of `ITERATION.md`. Renamed `parse-iteration-status` → `parse-iteration-status-edn`.
- **src/braids/iteration_io.clj** — `load-and-show` reads `iteration.edn`.
- **src/braids/status_io.clj** — `load-iteration-data` reads `iteration.edn`.
- **src/braids/new_io.clj** — Already updated by prior bead to write `iteration.edn`.

### Spec Changes

- **spec/braids/iteration_spec.clj** — Added tests for EDN parsing, validation, round-tripping, and migration from ITERATION.md.
- **spec/braids/orch_io_spec.clj** — Updated to use `iteration.edn` and test `parse-iteration-status-edn`.
- **spec/simulation_spec.clj** — All iteration scenarios use `iteration.edn`.
- **spec/integration_smoke_spec.clj** — Reads `iteration.edn` for all integration checks.
- **spec/structural_spec.clj** — Iteration validation reads `iteration.edn`.
- **spec/contracts_spec.clj** — Updated format list.
- **spec/braids/new_io_spec.clj** — Checks for `iteration.edn`.

### Documentation Changes

- **CONTRACTS.md** — Already had iteration.edn section (from prior work).
- **braids/SKILL.md** — Updated format section, directory tree, and all references.
- **braids/references/worker.md** — Already updated (from prior work).
- **braids/references/migration.md** — Updated references.

### Data Migration

Converted all 12 existing ITERATION.md files in `.braids/iterations/` to `iteration.edn` and removed the markdown originals.

## Test Results

370 examples, 6 failures (all pre-existing), 675 assertions.
