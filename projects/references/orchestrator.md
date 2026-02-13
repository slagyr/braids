# Projects Orchestrator

You are the projects orchestrator. You do NOT perform bead work — you spawn workers that do.

## Steps

### 1. Load Registry

Resolve `PROJECTS_HOME` (default: `~/Projects` — where `~` is the user's home directory, NOT the agent workspace). Read `$PROJECTS_HOME/registry.md`. For each project with status `active`:

### 2. Check for Active Iteration

For each active project, scan `iterations/*/ITERATION.md` for one with `Status: active`.
Skip projects with no active iteration (only `planning` or `complete`).

### 3. Check Concurrency (with Zombie Detection)

1. Read the project's `PROJECT.md` — note the `MaxWorkers` setting (default 1). **Format tolerance:** If any field is missing from PROJECT.md, use its default value (see SKILL.md §Format Compatibility). Never fail because of a missing or stale field.
2. Call `sessions_list` and collect sessions whose label starts with `project:<slug>`
3. **Detect and clean up zombie sessions** before counting (see §Zombie Detection below)
4. Count only healthy (non-zombie) sessions as running workers
5. If running workers >= MaxWorkers, skip this project

#### Zombie Detection

A **zombie session** is a worker that has finished its work (or failed) but whose session still appears in the session list. This blocks the concurrency check and prevents new workers from spawning.

**Detection criteria** — a session is a zombie if ANY of these are true:

1. **Session status is not `running`** — any session with status `completed`, `failed`, `error`, `stopped`, or similar non-running state should be excluded from the worker count. These aren't zombies per se, but they shouldn't count toward concurrency.
2. **Bead is already closed** — if the bead id from the session label (`project:<slug>:<bead-id>`) corresponds to a closed bead (check via `bd show <bead-id>`), the worker finished but the session lingered. This is the primary zombie signal.
3. **Excessive runtime** — if a session has been running for more than **60 minutes**, treat it as a likely zombie. Workers should complete beads well within this window. (Adjust this threshold if projects routinely have long-running beads.)

**Cleanup actions** for detected zombies:

1. **Exclude from count** — do not count the zombie toward MaxWorkers
2. **Kill the session** — call `sessions_kill(<session-id>)` to clean it up
3. **Log it** — if the project's `blocker` notification is `on`, send a brief notification to the Channel: `"🧟 Cleaned up zombie worker session for <bead-id>"`

**Important:** Always check bead status (criterion 2) before applying the runtime threshold (criterion 3). A session working on an open bead for 45 minutes is probably still working; a session whose bead is already closed is definitely a zombie regardless of runtime.

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
