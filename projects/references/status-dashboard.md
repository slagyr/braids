# Status Dashboard

Auto-generate `$PROJECTS_HOME/STATUS.md` — a progress dashboard across all active projects.

## When to Generate

The orchestrator generates STATUS.md at the **end of every run**, after spawning workers (Step 6 in orchestrator.md). This keeps the dashboard current without extra cron jobs.

## Output

Write to `$PROJECTS_HOME/STATUS.md`. This file is **auto-generated and overwritten** each run — do not hand-edit it.

## Format

```markdown
# Projects Dashboard

*Auto-generated: YYYY-MM-DD HH:MM*

## <Project Name> — <status>

**Iteration:** <N> (<iteration status>)
**Priority:** <priority> | **Autonomy:** <autonomy>

| Status | Count |
|--------|-------|
| Closed | X |
| In Progress | X |
| Open | X |
| Blocked | X |

**Active beads:**
- `<bead-id>`: <title> — <status>
- ...

---

## <Next Project> — <status>
...
```

## Generation Steps

For each active project in the registry:

1. Read `.project/PROJECT.md` for project name, status, priority, autonomy
2. Find the active iteration (scan `.project/iterations/*/ITERATION.md` for `Status: active`)
3. Run `bd list` in the project directory to get all beads
4. Count beads by status (closed, in_progress, open, blocked)
5. List non-closed beads as "Active beads"

If a project has no active iteration, show it with a note: "No active iteration."

## Example Output

```markdown
# Projects Dashboard

*Auto-generated: 2026-02-13 03:00*

## Projects Skill — active

**Iteration:** 003 (active)
**Priority:** high | **Autonomy:** full

| Status | Count |
|--------|-------|
| Closed | 10 |
| In Progress | 1 |
| Open | 1 |
| Blocked | 0 |

**Active beads:**
- `projects-skill-1xn`: Add progress dashboard — in_progress
- `projects-skill-9yv`: Auto-generate iteration retrospective — open

---
```
