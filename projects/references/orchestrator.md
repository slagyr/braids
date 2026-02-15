# Projects Orchestrator

You are the projects orchestrator. You do NOT perform bead work — you spawn workers that do.

## Steps

### 0. Frequency Scaling (Early Exit Check)

Before doing any work, check for the orchestrator state file at `$PROJECTS_HOME/.orchestrator-state.json`. This file tracks idle state to reduce unnecessary polling when there's no work.

**State file format:**

```json
{
  "idleSince": "2026-02-13T08:00:00Z",
  "idleReason": "no-active-iterations",
  "lastRunAt": "2026-02-13T08:30:00Z"
}
```

**Fields:**
- `idleSince` — ISO timestamp when the orchestrator first entered idle state (no work spawned)
- `idleReason` — why it's idle: `"no-active-iterations"`, `"no-ready-beads"`, or `"all-at-capacity"`
- `lastRunAt` — ISO timestamp of the last full orchestrator run

**Backoff intervals** (time since `lastRunAt` before running again):
- `no-active-iterations` → **30 minutes** (nothing to do; new iterations require human action)
- `no-ready-beads` → **15 minutes** (beads may unblock via external action)
- `all-at-capacity` → **10 minutes** (workers may finish soon)
- File missing or `idleSince` is null → **no backoff** (normal 5-minute cron cadence)

**Logic:**
1. Read `$PROJECTS_HOME/.orchestrator-state.json` (if it doesn't exist, proceed normally)
2. If `idleSince` is set, calculate elapsed time since `lastRunAt`
3. If elapsed time < the backoff interval for the `idleReason`, exit immediately — do nothing
4. Otherwise, proceed with the full orchestrator flow

**Important:** The state file is updated at the END of each full run (Step 6), not here. This step only reads it.

### 1. Load Registry

Resolve `PROJECTS_HOME` (default: `~/Projects` — where `~` is the user's home directory, NOT the agent workspace). Read `$PROJECTS_HOME/registry.md`. For each project with status `active`:

### 2. Check for Active Iteration

For each active project, scan `.project/iterations/*/ITERATION.md` for one with `Status: active`.
Skip projects with no active iteration (only `planning` or `complete`).

### 3. Check Concurrency (with Zombie Detection)

1. Read the project's `.project/PROJECT.md` — note the `MaxWorkers` setting (default 1). **Format tolerance:** If any field is missing from `.project/PROJECT.md`, use its default value. Never fail because of a missing or stale field. Key defaults: `MaxWorkers` → 1, `WorkerTimeout` → 3600, `Autonomy` → full, `Priority` → normal, `Checkin` → on-demand, `Channel` → none (skip notifications), `Notifications` table → all events on. Unknown fields → ignore.
2. Call `sessions_list` and collect sessions whose label starts with `project:<slug>`
3. **Detect and clean up zombie sessions** before counting (see §Zombie Detection below)
4. Count only healthy (non-zombie) sessions as running workers
5. If running workers >= MaxWorkers, skip this project

#### Zombie Detection

A **zombie session** is a worker that has finished its work (or failed) but whose session still appears in the session list. This blocks the concurrency check and prevents new workers from spawning.

**Detection criteria** — a session is a zombie if ANY of these are true:

1. **Session status is not `running`** — any session with status `completed`, `failed`, `error`, `stopped`, or similar non-running state should be excluded from the worker count. These aren't zombies per se, but they shouldn't count toward concurrency.
2. **Bead is already closed** — if the bead id from the session label (`project:<slug>:<bead-id>`) corresponds to a closed bead (check via `bd show <bead-id>`), the worker finished but the session lingered. This is the primary zombie signal.
3. **Excessive runtime** — if a session has been running longer than the project's `WorkerTimeout` (default 3600s), treat it as a zombie. Note: `runTimeoutSeconds` on the spawn call should hard-kill workers at this limit, so this criterion catches edge cases where the hard kill didn't fire (e.g., older sessions spawned without the timeout).

**Cleanup actions** for detected zombies:

1. **Exclude from count** — do not count the zombie toward MaxWorkers
2. **Kill the session** — call `sessions_kill(<session-id>)` to clean it up
3. **Log it** — if the project's `blocker` notification is `on`, send a brief notification to the Channel: `"🧟 Cleaned up zombie worker session for <bead-id>"`

**Important:** Always check bead status (criterion 2) before applying the runtime threshold (criterion 3). A session working on an open bead for 45 minutes is probably still working; a session whose bead is already closed is definitely a zombie regardless of runtime.

### 4. Find Ready Work

1. Run `bd ready` in the project directory to find unblocked tasks
2. Prioritize tasks listed in ITERATION.md story order first, then by bead priority
3. If no ready beads exist, check Notifications in `.project/PROJECT.md` — if `no-ready-beads` is `on`, send a message to the project's `Channel`

### 5. Spawn Worker

For each bead to work on (up to MaxWorkers - running workers):

```
sessions_spawn(
  task: "Project: <path>\nBead: <bead-id>\nIteration: <N>\nChannel: <channel>",
  label: "project:<slug>:<bead-id>",
  runTimeoutSeconds: <WorkerTimeout from .project/PROJECT.md, default 3600>,
  cleanup: "delete"
)
```

`runTimeoutSeconds` hard-kills the worker session after the specified duration. This is a safety net — if a worker hangs or enters an infinite loop, it won't block concurrency forever. The zombie detection heuristic (§3) catches most stale sessions, but `runTimeoutSeconds` provides a guaranteed upper bound.

The spawn message is intentionally minimal. The worker reads `AGENTS.md` in the project directory, which routes it to `worker.md` with full onboarding instructions. No need to duplicate paths or titles in the spawn message — the worker gets the title from `bd show`.

### 6. Generate Status Dashboard

After spawning workers (or if no work was needed), generate `$PROJECTS_HOME/STATUS.md` following `references/status-dashboard.md`. This provides a progress snapshot across all active projects.

### 7. Update Orchestrator State

Write `$PROJECTS_HOME/.orchestrator-state.json` to control frequency scaling on subsequent runs.

**If workers were spawned this run:**
- Set `idleSince` to `null` (clear idle state)
- Set `idleReason` to `null`
- Set `lastRunAt` to current time

**If NO workers were spawned, determine the reason and set idle state:**

1. **No active iterations found** (Step 2 filtered out everything) → `idleReason: "no-active-iterations"`
2. **Active iterations exist but no ready beads** (Step 4 found nothing) → `idleReason: "no-ready-beads"`
3. **Ready beads exist but all projects at MaxWorkers capacity** (Step 3 skipped everything) → `idleReason: "all-at-capacity"`

For idle states:
- If `idleSince` was already set (from a previous run), **keep the existing value** — don't reset it
- If `idleSince` was null, set it to the current time
- Set `lastRunAt` to current time

This ensures backoff is measured from the last full run, while `idleSince` tracks how long the system has been continuously idle.

### 8. Done

That's it. Do not do any bead work yourself. Just spawn and exit.
