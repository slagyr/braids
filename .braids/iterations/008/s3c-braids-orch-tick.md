# braids orch tick: orchestrator decision engine (braids-s3c)

## Summary

Implemented `braids orch-tick` — the orchestrator's decision engine as a pure CLI command. Scans all projects, finds ready beads, applies concurrency rules, and outputs JSON: either a spawn list or idle with reason. Replaces ~80% of orchestrator.md's decision logic with deterministic, testable code.

## Details

### Architecture: Pure + IO (same pattern as `ready`)

- **`braids.orch`** — Pure decision logic. `tick` takes registry, configs, active iterations map, ready beads, worker counts, and notifications → returns `{action: "spawn", spawns: [...]}` or `{action: "idle", reason: "..."}`. Zero side effects.
- **`braids.orch-io`** — IO layer. Finds active iterations by scanning `.braids/iterations/*/ITERATION.md`, reuses `ready-io` for registry/config/bead loading, composes everything into `tick`.
- **`braids.core`** — Wired `orch-tick` command; outputs JSON to stdout.

### Spawn decision output

Each spawn entry contains everything the orchestrator needs:
- `project` (slug), `bead` (id), `iteration` (number), `channel`, `path`
- `label` (session label convention: `project:<slug>:<bead-id>`)
- `worker_timeout` (from project config, default 3600)

### Idle reasons (maps directly to CONTRACTS.md §2.7)

- `no-active-iterations` — no projects have an active iteration
- `no-ready-beads` — active iterations exist but no unblocked beads
- `all-at-capacity` — ready beads exist but all projects at MaxWorkers

### Key decisions

- **No backoff logic in CLI** — CLI is zero-cost to run; backoff belongs in the orchestrator agent/cron layer that calls this command
- **Reuses ready-io** for registry, config, and bead loading — no duplication
- **Capacity-limited spawns** — each project gets at most `max-workers - current-workers` spawns
- **`no-ready-beads-projects` helper** — exposed for the orchestrator to send notifications to projects that have active iterations but no work

### Tests (13 specs)

- Idle: no projects, no active iterations, no ready beads, all at capacity
- Spawn: single bead, capacity-limited, multi-project, default timeout
- Edge cases: paused config override, notification mentions passthrough
- JSON formatting: spawn and idle outputs

## Assets

- `src/braids/orch.clj` — Pure tick logic
- `src/braids/orch_io.clj` — IO layer
- `spec/braids/orch_spec.clj` — 13 specs
- `src/braids/core.clj` — Updated command dispatch
