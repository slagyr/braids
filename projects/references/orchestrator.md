# Projects Orchestrator

You are the projects orchestrator. You do NOT perform bead work — you spawn workers that do.

## Steps

### 1. Load Registry

Resolve `PROJECTS_HOME` (default: `~/Projects` — where `~` is the user's home directory, NOT the agent workspace). Read `$PROJECTS_HOME/registry.md`. For each project with status `active`:

### 2. Check for Active Iteration

For each active project, scan `iterations/*/ITERATION.md` for one with `Status: active`.
Skip projects with no active iteration (only `planning` or `complete`).

### 3. Check Concurrency

1. Read the project's `PROJECT.md` — note the `MaxWorkers` setting (default 1). **Format tolerance:** If any field is missing from PROJECT.md, use its default value (see SKILL.md §Format Compatibility). Never fail because of a missing or stale field.
2. Call `sessions_list` and count sessions whose label starts with `project:<slug>`
3. If running workers >= MaxWorkers, skip this project

### 4. Find Ready Work

1. Run `bd ready` in the project directory to find unblocked tasks
2. Prioritize tasks listed in ITERATION.md story order first, then by bead priority
3. If no ready beads exist, check Notifications in PROJECT.md — if `no-ready-beads` is `on`, send a message to the project's `Channel`

### 5. Spawn Worker

For each bead to work on (up to MaxWorkers - running workers):

```
sessions_spawn(
  task: "You are a project worker. Read and follow ~/.openclaw/skills/projects/references/worker.md\n\nProject: <path>\nIteration: <N>\nBead: <bead-id>\nBead title: <title>\nChannel: <channel>",
  label: "project:<slug>:<bead-id>"
)
```

### 6. Done

That's it. Do not do any bead work yourself. Just spawn and exit.
