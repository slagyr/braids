# CONTRACTS.md: Document Invariants (projects-skill-b25)

## Summary

Created `CONTRACTS.md` at the project root documenting all system invariants for the orchestrator, worker, and file formats. Added `tests/test_contracts.sh` with 39 checks validating the document's completeness and consistency.

## Details

### CONTRACTS.md Structure

Four major sections:

1. **File Format Contracts** — Invariants for registry.md, PROJECT.md, ITERATION.md, deliverables, RETRO.md, STATUS.md, and .orchestrator-state.json. Includes required fields, valid values, and defaults.

2. **Orchestrator Invariants** — No direct work, concurrency enforcement, zombie cleanup priority order, session label convention, frequency scaling rules, status dashboard generation.

3. **Worker Invariants** — Context loading order, claim-before-work, dependency verification, deliverable requirement, close sequence, git commit format, iteration completion check, notification discipline, format tolerance, error escalation, autonomy respect.

4. **Cross-Cutting Invariants** — Path convention (~), PROJECTS_HOME resolution, single source of truth, git as transport, completed iteration immutability, bead lifecycle states.

### Test Coverage

`tests/test_contracts.sh` validates:
- Document exists and has all four major sections
- All file formats are documented
- Key defaults match SKILL.md (MaxWorkers=1, WorkerTimeout=1800, etc.)
- All notification events are covered
- Critical invariants are present (zombie detection, concurrency, format tolerance, etc.)
- Valid status enumerations are documented

## Assets

- `/CONTRACTS.md` — The contracts document
- `/tests/test_contracts.sh` — Structural tests (39 checks, all passing)
