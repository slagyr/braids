# CONTRACTS.md — System Invariants

This document defines the invariants that the orchestrator, worker, and file formats must uphold. Tests and agents should use these as the source of truth for correctness.

---

## 1. File Format Contracts

### 1.1 registry.edn

- **Location:** `~/.openclaw/braids/registry.edn`
- **Format:** EDN map with `:projects` key containing a vector of project maps
- **Required keys per project:** `:slug`, `:status`, `:priority`, `:path`
- **Valid statuses:** `:active`, `:paused`, `:blocked` (never `:complete`)
- **Slug uniqueness:** No two projects share the same `:slug`
- **Path validity:** Each `:path` must point to an existing directory containing `.braids/config.edn`
- **No markdown fallback:** `registry.md` is not read; use `bd migrate` to convert

### 1.2 config.edn

- **Location:** `<project-root>/.braids/config.edn`
- **Format:** EDN map — structured config only (no prose; goal/guardrails live in AGENTS.md)
- **Required fields:** `:name`, `:status`, `:priority`, `:autonomy` (all others have defaults)
- **Defaults when missing:**
  - `:max-workers` → 1
  - `:worker-timeout` → 3600
  - `:autonomy` → `:full`
  - `:priority` → `:normal`
  - `:checkin` → `:on-demand`
  - `:channel` → nil (skip notifications silently)
  - `:notifications` → all events `true`
- **Valid values:**
  - `:status`: `:active`, `:paused`, `:blocked`
  - `:priority`: `:high`, `:normal`, `:low`
  - `:autonomy`: `:full`, `:ask-first`, `:research-only`
  - `:checkin`: `:daily`, `:weekly`, `:on-demand`
  - `:max-workers`: positive integer
  - `:worker-timeout`: positive integer (seconds)
- **Unknown fields:** Ignored (never error)
- **`:goal` and `:guardrails` are NOT stored here** — they belong in the project's AGENTS.md
- **Notification events:** `:iteration-start`, `:bead-start`, `:bead-complete`, `:iteration-complete`, `:no-ready-beads`, `:question`, `:blocker`
- **Notification mentions:** `:notification-mentions` maps event keywords to vectors of mention strings (e.g., `{:blocker ["<@123>" "<@456>"]}`). Single strings are normalized to vectors on parse.
- **No markdown fallback:** `PROJECT.md` is not read as config; use `braids migrate` to convert. Legacy `project.edn` is still read as fallback.

### 1.3 ITERATION.md

- **Location:** `<project-root>/.braids/iterations/<N>/ITERATION.md`
- **Required fields:** Status
- **Valid statuses:** `planning`, `active`, `complete`
- **At most one active iteration per project** at any time
- **Immutability:** Once status is `complete`, the iteration directory must not be modified
- **Stories list:** Each entry is a bead id followed by a colon and title
- **Optional sections:** Guardrails, Notes (default: none)

### 1.4 Deliverable Files

- **Location:** `.braids/iterations/<N>/<id-suffix>-<descriptive-name>.md`
- **Naming:** `<id-suffix>` = last 3 characters of bead id; `<descriptive-name>` = short kebab-case summary
- **One deliverable per bead** per iteration
- **Required sections:** Summary (at minimum)

### 1.5 STATUS.md

- **Location:** `~/.openclaw/braids/STATUS.md`
- **Auto-generated:** Overwritten every orchestrator run; never hand-edit
- **Contains:** Timestamp, per-project summary with iteration status and bead counts

### 1.6 .orchestrator-state.json

- **Location:** `~/.openclaw/braids/.orchestrator-state.json`
- **Fields:** `idleSince` (ISO timestamp or null), `idleReason` (string or null), `lastRunAt` (ISO timestamp)
- **Valid idleReasons:** `no-active-iterations`, `no-ready-beads`, `all-at-capacity`, or null

---

## 2. Orchestrator Invariants

### 2.1 No Direct Work
The orchestrator **never** performs bead work. It only reads state and spawns workers.

### 2.2 Concurrency Enforcement
For each project, the number of healthy (non-zombie) running workers must never exceed `MaxWorkers`.

### 2.3 Active Iteration Required
Workers are only spawned for projects that have exactly one iteration with `Status: active`.

### 2.4 Spawn Message Format
Worker spawn messages contain exactly four fields: `Project`, `Bead`, `Iteration`, `Channel`. Nothing more.

### 2.5 Session Label Convention
Workers are spawned with label `project:<slug>:<bead-id>`. The orchestrator uses the `project:<slug>` prefix to count active workers per project.

### 2.6 Zombie Cleanup Priority
Zombie detection criteria are evaluated in this order:
1. Non-running session status → exclude from count
2. Bead already closed → kill session
3. Runtime exceeds `WorkerTimeout` → kill session

Always check bead status before applying runtime threshold.

### 2.7 Frequency Scaling
- Workers spawned → clear idle state (`idleSince` = null)
- No workers spawned → set/preserve `idleSince`, set `idleReason`
- `idleSince` is only set when transitioning from non-idle to idle; preserved across consecutive idle runs
- Backoff intervals: `no-active-iterations` = 30min, `no-ready-beads` = 15min, `all-at-capacity` = 10min

### 2.8 Status Dashboard
STATUS.md is regenerated at the end of every full orchestrator run (not skipped runs).

---

## 3. Worker Invariants

### 3.1 Context Loading Order
Workers must read context in this order before any work:
1. `.braids/config.edn`
2. Workspace AGENTS.md (`~/.openclaw/workspace/AGENTS.md`)
3. Project AGENTS.md (goal, guardrails, conventions)
4. ITERATION.md for the assigned iteration

### 3.2 Claim Before Work
A worker must successfully `bd update <bead-id> --claim` before starting any work. If the claim fails, stop immediately.

### 3.3 Dependency Verification
After claiming, the worker verifies all direct dependencies are closed. If any are open, the bead is marked `blocked` and work stops. Transitive dependencies are not checked (handled by `bd ready`).

### 3.4 Deliverable Required
Every completed bead produces a deliverable file in the iteration directory.

### 3.5 Close After Deliverable
Beads are closed only after the deliverable is written. Sequence: work → write deliverable → `bd update -s closed` → git commit.

### 3.6 Git Commit on Completion
Every bead closure includes a git commit. Format: `"<summary> (<bead-id>)"`.

### 3.7 Iteration Completion Check
After closing a bead, the worker checks if the iteration is complete (no open beads remain). If so, the worker updates ITERATION.md status to `complete`, sends the iteration-complete notification, and commits.

### 3.8 Notification Discipline
Workers only send notifications for events that are `on` in the project's Notifications table. If `Channel` is missing, all notifications are silently skipped. When mentions are configured for an event, include all mention strings from the vector.

### 3.9 Format Tolerance
Workers never fail due to missing or unknown fields in config.edn or ITERATION.md. Missing fields use defaults; unknown fields are ignored.

### 3.10 Error Escalation
- Recoverable errors: retry once, try alternatives, then escalate
- Blockers: mark bead `blocked`, notify, stop
- Questions: notify, continue other work if possible
- Guardrail conflicts: never violate; block and escalate
- Partial completion: write partial deliverable, block with context

### 3.11 Autonomy Respect
Workers with `ask-first` autonomy must confirm via Channel before executing. `full` autonomy workers execute freely. `research-only` workers only gather information.

---

## 4. Cross-Cutting Invariants

### 4.1 Path Convention
`~` always resolves to the user's home directory, never the agent workspace. Project files are never created inside `~/.openclaw/workspace/`.

### 4.2 BRAIDS_HOME Resolution
`BRAIDS_HOME` defaults to `~/Projects`. Checked once per session. Agent infrastructure files (registry.edn, .orchestrator-state.json, STATUS.md) live in `~/.openclaw/braids/`, not in `BRAIDS_HOME`.

### 4.3 Single Source of Truth
- What to work on → `bd ready` (not manual lists)
- Project config → config.edn (not SKILL.md at runtime)
- Goal/guardrails → project AGENTS.md
- Iteration state → ITERATION.md
- Bead state → `bd` commands

### 4.4 Git as Transport
All project work is committed and pushed. Work is not complete until `git push` succeeds.

### 4.5 Completed Iterations Are Immutable
No modifications to iterations with `Status: complete` — files, ITERATION.md, and deliverables are all frozen.

### 4.6 Bead Lifecycle
Valid bead state transitions: `open` → `in_progress` (claim) → `closed` | `blocked`. Blocked beads can be reopened. Closed beads are final.

### 4.7 Channel Agent — Beads Only
The channel/main session agent must not modify project files directly. Its role in the project channel is limited to:
- Creating beads (stories/tasks)
- Planning and activating iterations
- Reviewing deliverables
- Answering questions and unblocking workers

All file changes (SKILL.md, worker.md, orchestrator.md, config.edn, CONTRACTS.md, reference docs, etc.) must go through beads assigned to workers. This ensures changes are tracked, tested, and committed through the standard bead lifecycle.
