# Integration Tests (braids-j9i)

## Summary

Added comprehensive integration test suite at `spec/braids/integration_spec.clj` that creates real temporary projects, iterations, and beads, then runs all CLI commands to verify end-to-end workflows.

## Test Coverage

### Project Creation (6 tests)
- Creates project with correct directory structure (.braids/config.edn, iterations/001, AGENTS.md, .git)
- Verifies config values (name, status, priority, autonomy)
- Adds project to registry correctly
- Creates initial iteration in planning status
- Creates initial git commit
- Rejects duplicate project slugs
- Creates high-priority projects with correct registry entry

### Bead CRUD Workflow (6 tests)
- Creates beads with `bd q`
- Shows bead details with `bd show`
- Lists beads with `bd list --json`
- Shows ready beads with `bd ready --json`
- Closes beads with `bd close`
- Claims beads with `bd update --claim`

### Dependency Management (2 tests)
- Adds and lists dependencies with `bd dep add` / `bd dep list`
- Verifies blocked beads aren't ready until dependency is closed

### Iteration Lifecycle (4 tests)
- Starts with iteration 001 in planning status
- Activates iteration by updating iteration.edn
- Completes iteration after closing all beads
- Creates second iteration

### CLI Dispatch (4 tests)
- Help command, unknown command, --help flag, no-args

### Pure Function Contracts (11 tests)
- ready-beads: empty registry, max-workers enforcement, capacity availability
- orch/tick: idle with disable-cron when no projects
- list/format-list: empty and populated registries
- iteration parsing and completion stats
- Slug validation (valid and invalid)
- Config get/set

### End-to-End Worker Workflow (1 test)
- Full cycle: create bead → set up iteration → verify ready → claim → write deliverable → close → git commit → verify

### Multi-Project Registry (2 tests)
- Manages multiple projects in registry
- Preserves priority ordering

## Approach

Tests use isolated temporary directories with their own registry and config, avoiding interference with real projects. Each describe block gets its own environment via `setup-test-env` / `cleanup-test-env`. Tests exercise real `bd` CLI commands (not mocks) for true integration coverage.

## Total: 36 new test examples, all passing
