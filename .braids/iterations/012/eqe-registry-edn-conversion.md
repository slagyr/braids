# Convert registry.md to registry.edn

## Summary

Verified and finalized the registry.edn migration. The CLI already reads/writes only `registry.edn` with no markdown fallback — this was implemented incrementally across prior iterations. This bead confirmed completeness and fixed remaining issues.

## Changes

1. **Updated live `~/.openclaw/braids/registry.edn`** — Fixed stale slug (`projects-skill` → `braids`) and path (`~/Projects/projects-skill` → `~/Projects/braids`) left over from the repo rename.

2. **Fixed `spec/structural_spec.clj`** — Updated `skill-source` path from `projects-skill/braids` to `braids/braids` to match the renamed repo.

## Verification

- `braids.registry` — All parse/validate/round-trip tests pass
- `braids.ready-io` — `load-registry` reads only `.edn`, no markdown fallback, returns empty for missing file
- `braids.migration` — `parse-registry-md` preserved for `bd migrate` command (converts old `.md` to `.edn`)
- `CONTRACTS.md` §1.1 — Already specifies "No markdown fallback: `registry.md` is not read; use `bd migrate` to convert"

## Pre-existing Failures (not caused by this bead)

- Structural/integration tests fail for live filesystem issues (wealth/zaap iteration structure, orphaned deliverables, Homebrew formula old repo reference, install.sh old repo reference)
