# braids iteration: show active iteration and bead statuses (braids-604)

## Summary
Added `braids iteration` CLI command that displays the active iteration for a project with story statuses, dependencies, and completion percentage.

## Details

### New files
- `src/braids/iteration.clj` — Pure functions for parsing ITERATION.md stories, annotating with bead data, computing completion stats, and formatting output (human-readable table + JSON)
- `src/braids/iteration_io.clj` — IO layer that finds the active iteration, loads beads via `bd list --all --json`, and wires everything together
- `spec/braids/iteration_spec.clj` — 12 specs covering parsing, annotation, stats, and formatting

### CLI usage
```
braids iteration              # Show active iteration for current project
braids iteration --json       # JSON output
braids iteration --project ~/Projects/foo  # Specify project path
```

### Output format
```
Iteration 009 [active] — 2/6 done (33%)
───────────────────────────────────────
  ✓ proj-abc: Done thing [closed P1]
  ▶ proj-def: In progress [in_progress P1] ← proj-dep
  ○ proj-ghi: Not started [open P1]
```

Status icons: ✓ closed, ▶ in_progress, ✗ blocked, ○ open, ? unknown

### Approach
- TDD: wrote specs first, then implementation
- Reused `orch-io/find-active-iteration` for locating the active iteration
- Uses `bd list --all --json` to include closed beads in status display
- Pure/IO separation consistent with existing codebase patterns
