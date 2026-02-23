# braids ready: list beads ready to work (braids-xgq)

## Summary

Implemented the `braids ready` CLI command — the core orchestrator decision engine extracted into deterministic, testable code. The command lists beads that are open, have deps met, and whose project is not at worker capacity.

## Details

### Architecture: Pure + IO separation

- **`braids.ready`** — Pure logic module. `ready-beads` takes registry, project configs, ready beads per project, and worker counts, returns a flat ordered list of ready beads. `format-ready-output` handles human-readable formatting. No side effects.
- **`braids.ready-io`** — IO layer. Loads registry (EDN or MD fallback), project configs (EDN or MD fallback), shells out to `bd ready --json` per project, and counts workers from session labels. Composes everything and calls the pure logic.
- **`braids.core`** — Wired `ready` command dispatch to the IO pipeline.

### Key decisions

- **Pure/IO split** makes the decision engine fully testable without filesystem or process mocking. The `orch tick` command (next bead) can reuse `ready-beads` directly.
- **Format tolerance** — reads both EDN and markdown registry/project files, falling back gracefully.
- **Project priority ordering** — results sorted by project priority (high → normal → low).
- **Worker capacity** via `count-workers` parsing session labels with `project:<slug>:*` convention.
- Priority displayed as P0/P1/P2 format matching bd conventions.

### Tests

- 11 specs for `braids.ready` (pure logic): empty registry, inactive projects, capacity limits, default max-workers, config status override, multi-project, priority ordering, output formatting.
- 4 specs for `braids.ready-io` (count-workers): empty, counting, non-project label filtering, slug-only labels.

## Assets

None.
