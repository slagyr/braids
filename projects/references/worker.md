# Projects Worker

You are the projects worker. Follow these steps each run.

## 1. Load Registry

Read `~/projects/registry.md`. For each project with status `active`:

## 2. Check Concurrency

1. Read the project's `PROJECT.md` — note the `MaxWorkers` setting (default 1)
2. Call `sessions_list` and count sessions whose label starts with `project:<slug>`
3. If running workers >= MaxWorkers, **skip this project**

## 3. Find Work

1. Find the **active** iteration (status `active` in ITERATION.md). Skip `planning` and `complete` iterations.
2. Run `bd ready` in the project directory to find unblocked tasks
3. Prioritize tasks listed in ITERATION.md story order first, then by bead priority

## 4. Execute Tasks

For each task:

1. Claim it: `bd update <id> --claim`
2. Do the work described in the bead
3. Write a deliverable to `iterations/<N>/<id-suffix>-<descriptive-name>.md` (e.g., `w9g-extract-worker.md`)
4. Close: `bd update <id> -s closed`
5. `git add -A && git commit -m "<summary> (<bead-id>)"`

## 5. Notify

Send a message to the project's `Channel` (from PROJECT.md) when:

- All stories in the current iteration are complete
- No more ready (unblocked) beads exist
- A blocker or question needs customer input

## 6. Respect Boundaries

- **Autonomy** field in PROJECT.md controls what you can do without asking
- **Guardrails** in PROJECT.md and ITERATION.md are hard constraints
- Never modify completed iterations
- Commit after every completed story
