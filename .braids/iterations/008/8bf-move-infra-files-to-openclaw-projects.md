# Move registry.md, orchestrator-state, and STATUS.md to ~/.openclaw/projects/ (braids-8bf)

## Summary

Moved agent infrastructure files (registry.md, .orchestrator-state.json, STATUS.md) from `$PROJECTS_HOME` (~/Projects/) to `~/.openclaw/projects/`. These files are agent infrastructure, not project content, so they belong under the OpenClaw directory.

## Details

### Code changes

- **`src/braids/ready_io.clj`** — Added `resolve-state-home` function returning `~/.openclaw/projects/`. `gather-and-compute` now accepts `:state-home` option and defaults to state-home (not projects-home) for loading the registry.
- **`src/braids/orch_io.clj`** — `gather-and-tick` similarly updated to use `state-home` for registry loading.

### Documentation changes

Updated all references from `$PROJECTS_HOME/registry.md` → `~/.openclaw/projects/registry.md` (and same for STATUS.md and .orchestrator-state.json) in:
- `CONTRACTS.md` — Location fields for registry.md (§1.1), STATUS.md (§1.5), .orchestrator-state.json (§1.6), and PROJECTS_HOME Resolution (§4.2)
- `projects/SKILL.md` — Directory layout, registry format heading, orchestrator frequency scaling, PROJECTS_HOME resolution description
- `projects/references/orchestrator.md` — STATUS.md generation path
- `projects/references/init.md` — Registry creation path, verification checklist, section rename
- `projects/references/project-creation.md` — Prerequisites, slug check, registry append path
- `projects/references/status-dashboard.md` — Output location

### Spec changes

- **`spec/braids/ready_io_spec.clj`** — Added tests for `resolve-state-home` and `load-registry` using state-home
- **`spec/structural_spec.clj`** — Registry path updated to `~/.openclaw/projects/registry.md`
- **`spec/integration_smoke_spec.clj`** — Registry, STATUS.md, and .orchestrator-state.json paths updated to `~/.openclaw/projects/`

### Physical file move

Moved the three files from `~/Projects/` to `~/.openclaw/projects/`:
- `registry.md`
- `.orchestrator-state.json`
- `STATUS.md`

### Key design decision

Introduced `state-home` as a separate concept from `projects-home`. `PROJECTS_HOME` (~/Projects) is where project repos live. `~/.openclaw/projects/` is where agent infrastructure lives. This is a fixed path, not configurable — it's part of the OpenClaw directory structure.

## Assets

None.
