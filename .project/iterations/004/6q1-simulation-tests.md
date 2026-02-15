# Simulation Tests

## Summary

Added `tests/test_simulation.sh` — 61 simulation tests covering 15 scenarios that validate orchestrator/worker behavior contracts against a temporary test project.

## What It Tests

1. **PROJECT.md defaults** (§1.2) — missing fields use correct defaults
2. **Iteration lifecycle** (§1.3, §4.5) — active count, completion immutability
3. **Deliverable naming** (§1.4) — `<id-suffix>-<name>.md` convention
4. **Frequency scaling** (§1.7, §2.7) — state file transitions, backoff intervals
5. **Worker context loading** (§3.1) — all required files exist
6. **Spawn message format** (§2.4) — exactly 4 fields
7. **Session label convention** (§2.5) — `project:<slug>:<bead-id>`
8. **Bead lifecycle** (§4.6) — valid state transitions documented
9. **Registry validation** (§1.1) — columns, valid/invalid statuses
10. **Worker error handling** (§3.10) — partial deliverable with remaining work
11. **RETRO.md format** (§1.5) — required sections present
12. **Orchestrator invariants** (§2.1) — no-direct-work, concurrency, active iteration
13. **Git conventions** (§3.6) — commit message format
14. **STATUS.md** (§1.6) — auto-generated, never hand-edit
15. **Path conventions** (§4.1, §4.2) — home resolution, PROJECTS_HOME default

## Approach

Tests create a temporary project structure in `/tmp`, populate it with mock files, and validate contracts from CONTRACTS.md. No actual sessions are spawned — this tests the file-level invariants that orchestrator and worker must uphold.

## Note

Pre-existing structural test failures (iterations/4 naming, wealth PROJECT.md format) are unrelated to this bead.
