# braids status: dashboard view

## Summary

Implemented the `braids status` CLI command that shows a dashboard across all projects or detailed view for a single project.

## What was built

### Pure logic (`src/braids/status.clj`)
- `build-dashboard` — assembles dashboard data from registry, configs, iterations, and worker counts
- `format-dashboard` — human-friendly overview of all projects with iteration progress and worker counts
- `format-dashboard-json` — JSON output for machine consumption
- `format-project-detail` — detailed single-project view showing all stories with status icons

### IO layer (`src/braids/status_io.clj`)
- `load-and-status` — orchestrates loading registry, configs, iteration data, and workers
- Supports `--json` flag and optional project slug for single-project detail

### CLI integration (`src/braids/core.clj`)
- Added `status` command to command map and dispatch
- Supports: `braids status`, `braids status <slug>`, `braids status --json`

### Tests (`spec/braids/status_spec.clj`)
- 14 specs covering: build-dashboard, format-dashboard, format-dashboard-json, format-project-detail
- All pass (264 total examples, 9 pre-existing integration failures unrelated to this work)

## Output examples

**Dashboard view:**
```
BRAIDS STATUS
────────────────────────────────────────
  projects-skill [active] iter:009 3/6 (50%) workers:0/1
  zane-setup [active]
  wealth [active]
```

**Single project detail:**
```
projects-skill [active] — Iteration 009 — 3/6 (50%) — workers: 0/1
────────────────────────────────────────────────────────────
  ✓ braids-98r: braids list [closed]
  ▶ braids-v1n: braids status [in_progress]
  ○ braids-57e: braids orch spawn-msg [open]
```
