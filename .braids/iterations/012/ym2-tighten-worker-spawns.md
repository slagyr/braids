# Tighten Worker Spawns to Prevent Zombies (braids-ym2)

## Changes

### 1. Reduced default worker-timeout from 3600s to 1800s (30 min)
- **`src/braids/project_config.clj`** — default changed from 3600 to 1800
- **`src/braids/new.clj`** — new project default changed from 3600 to 1800
- All documentation updated to reflect the new default

### 2. Worker exit instructions added
- **`braids/references/worker.md`** — new "Exit Cleanly" section at the end, instructing workers to:
  - Stop immediately after completing or blocking a bead
  - Not loop, poll, or wait for further instructions
  - Not start additional beads
  - Not perform background work or maintenance

### 3. Orchestrator spawn parameters documented
- **`braids/references/orchestrator.md`** — expanded spawn parameter documentation:
  - Added `thinking: "low"` parameter to keep cost/latency down
  - Documented why each parameter matters (`runTimeoutSeconds`, `cleanup: "delete"`, `thinking`)

### 4. Zombie detection aligned with new timeout
- **`braids/SKILL.md`** — zombie detection threshold updated from hardcoded "60 minutes" to reference the project's `worker-timeout` (default 30 min)

### 5. Documentation updated across all references
- `braids/references/orchestrator.md`, `worker.md`, `project-creation.md`
- `braids/SKILL.md`
- `CONTRACTS.md`

## Tests Updated
- `spec/braids/orch_spec.clj` — default timeout assertion
- `spec/braids/project_config_spec.clj` — defaults assertions
- `spec/braids/new_spec.clj` — new project default assertion
- `spec/braids/migration_spec.clj` — migration default assertion
- `spec/contracts_spec.clj` — contract validation
- `spec/simulation_spec.clj` — simulation description

All tests pass (7 pre-existing failures unrelated to this change).
