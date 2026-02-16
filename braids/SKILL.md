---
name: braids
description: Autonomous background project management with iterative delivery. Use when managing long-running projects, spawning background work sessions, tracking tasks with beads (bd), organizing iterations, or preparing check-in reviews. Triggers on project creation, status checks, work sessions, iteration planning, and review preparation.
---

# Braids

Manage long-running autonomous projects with iterative delivery cycles.

## First-Time Setup

For initial installation (symlink, `bd`, `BRAIDS_HOME`, orchestrator cron), follow [`references/init.md`](references/init.md).

## Path Convention

**`~` always means the user's home directory** (e.g., `/Users/micah` or `/home/micah`), NOT the agent workspace directory. Never create project files inside `~/.openclaw/workspace/`.

**`BRAIDS_HOME`** defines where all projects live. Default: `~/Projects`

Resolve `BRAIDS_HOME` at the start of every session (default: `~/Projects`). All project repos live under `BRAIDS_HOME`. Agent infrastructure files (registry, orchestrator state, STATUS dashboard) live in `~/.openclaw/braids/`.

## Directory Layout

```
~/.openclaw/braids/
  registry.md                    # Master list of all projects
  STATUS.md                      # Auto-generated progress dashboard (see references/status-dashboard.md)
  .orchestrator-state.json       # Orchestrator idle/backoff state

$BRAIDS_HOME/
  <project-slug>/                # Each project is its own git repo
    AGENTS.md                    # Universal entry point for any agent landing in the repo
    .project/
      PROJECT.md                 # Goals, guardrails, autonomy
      iterations/
        001/
          ITERATION.md           # Status, stories, iteration guardrails
          <id-suffix>-<name>.md # Deliverable per story (e.g. w9g-extract-worker.md)
          assets/                # Screenshots, artifacts
        002/
          ...
    .beads/                      # bd task tracking
```

## Registry Format (`~/.openclaw/braids/registry.md`)

```markdown
# Projects

| Slug | Status | Priority | Path |
|------|--------|----------|------|
| my-project | active | normal | $BRAIDS_HOME/my-project |
```

Statuses: `active`, `paused`, `blocked`. No "complete" — pause permanently instead.

## PROJECT.md Format

**Location:** `.project/PROJECT.md`

```markdown
# <Project Name>

- **Status:** active | paused | blocked
- **Priority:** high | normal | low
- **Autonomy:** full | ask-first | research-only
- **Checkin:** daily | weekly | on-demand
- **Channel:** <discord channel id or name for check-ins>
- **MaxWorkers:** <number, default 1>
- **WorkerTimeout:** <seconds, default 3600>

## Goal
<High-level description of what this project aims to achieve>

## Notifications

| Event | Notify |
|-------|--------|
| iteration-start | on |
| bead-start | on |
| bead-complete | on |
| iteration-complete | on (mention <@user-id>) |
| no-ready-beads | on |
| question | on (mention <@user-id>) |
| blocker | on (mention <@user-id>) |

## Guardrails
<Project-specific constraints and boundaries>
```

No "current focus" here — beads (`bd ready`) determines what to work on.

## AGENTS.md Template

Every project gets an `AGENTS.md` as the universal entry point for any agent landing in the repo — whether spawned by the orchestrator, manually directed by a human, or exploring on their own.

See the project's own `AGENTS.md` for the canonical version. The template lives in `references/agents-template.md`.

## Notifications

Workers send notifications to the project's Channel based on the Notifications table in `.project/PROJECT.md`. Each event type can be toggled `on` or `off` per project.

| Event | Description |
|-------|-------------|
| iteration-start | An iteration begins (status changed to active) |
| bead-start | A worker claims and begins working on a bead |
| bead-complete | A worker completes a bead (include summary in notification) |
| iteration-complete | All stories in the current iteration are done |
| no-ready-beads | No unblocked beads remain for the project |
| question | A question arises needing customer input |
| blocker | A blocker prevents progress |

All events default to `on` if the table is missing from `.project/PROJECT.md`.

### Mentions

On channels that support mentions (Discord, Slack, etc.), add `mention <user-ref>` in the Notify column to ping the project owner. This is configurable per event — add it only to high-signal events where a phone notification is desired.

By default, `iteration-complete`, `blocker`, and `question` include mentions — these are events that need human attention. Add or remove mentions on other events as needed per project.

Example:
```
| iteration-complete | on (mention <@discord-user-id>) |
| blocker | on (mention <@discord-user-id>) |
| question | on (mention <@discord-user-id>) |
```

Workers should include the mention in the notification message when the Notify value contains a `mention` directive.

## ITERATION.md Format

**Location:** `.project/iterations/<N>/ITERATION.md`

```markdown
# Iteration <N>

- **Status:** planning | active | complete

## Stories
- bd-xxxx: <title>
- bd-yyyy: <title>

## Guardrails
<Iteration-specific constraints>

## Notes
<Any relevant context>
```

- **planning**: Stories are being defined; workers must not pick up tasks yet
- **active**: Ready for work; workers claim and complete tasks from this iteration
- **complete**: All stories delivered; immutable — never modify completed iterations

Completed iterations are **immutable** — never modify them.

## Story Deliverables

Each story produces `.project/iterations/<N>/<id-suffix>-<descriptive-name>.md` where `<id-suffix>` is the last 3 characters of the bead id and `<descriptive-name>` is a short kebab-case summary (e.g., `w9g-extract-worker.md` for bead `projects-skill-w9g`):

```markdown
# <Story Title> (bd-xxxx)

## Summary
<What was accomplished>

## Details
<Detailed description, decisions made, approach taken>

## Assets
<Links to screenshots, code, or other artifacts in assets/ subdirectory>
```

## Workflow

### Creating a Project

Follow [`references/project-creation.md`](references/project-creation.md) — an interactive guide the agent walks through with the human. It covers gathering project info, scaffolding the directory, generating real PROJECT.md content (not TODO templates), creating a Discord channel, seeding stories, and reviewing with the human before committing.

### Channel for Check-ins

Each project needs a channel for notifications and check-ins. Set the `Channel` field in PROJECT.md to a channel id or name.

When creating a new channel (e.g., on Discord):
- Use the `message` tool with `action: channel-create`
- Choose any descriptive name (e.g., `project-my-app`, `my-app-updates`, etc.)
- Optionally place it under a category
- Set the channel topic to the project goal from PROJECT.md
- Record the channel id in PROJECT.md's `Channel` field

This channel is for **planning and notifications only** — all actual work happens asynchronously via beads and the cron worker. The channel receives:
- Iteration completion notifications
- Blocker/question alerts
- Check-in review summaries

### Channel Agent Convention

The channel/main session agent must **not** modify project files directly. Its role is strictly limited to:
- **Creating beads** (stories/tasks)
- **Planning and activating iterations**
- **Reviewing deliverables**
- **Answering questions** and unblocking workers

All file changes — SKILL.md, worker.md, orchestrator.md, PROJECT.md, CONTRACTS.md, reference docs, etc. — must go through beads assigned to workers. This ensures every change is tracked, tested, and committed through the standard bead lifecycle. The channel agent should never `Edit` or `Write` project files itself; instead, it creates a bead describing the desired change and lets a worker execute it.

### Working a Project (Background Sessions)

1. Read registry, find active projects
2. For each active project with an active iteration:
   - Read `.project/PROJECT.md` — note the `MaxWorkers` setting (default 1)
   - Check running sessions (via `sessions_list`) for workers with label prefix `project:<slug>`
   - **If running workers >= MaxWorkers, skip this project**
   - Read ITERATION.md for the ordered story list
   - Run `bd ready` to find unblocked tasks
   - **Prioritize work in ITERATION.md story order first**, then by bead priority for non-iteration tasks
   - Each task: claim (`bd update <id> --claim`), do work, write deliverable to `.project/iterations/<N>/<id-suffix>-<descriptive-name>.md`, close (`bd update <id> -s closed`)
   - Commit after each completed story
3. Notify the project's Channel when all stories are done, no ready beads remain, or a blocker is hit

### Preparing for Check-in

REVIEW.md is **only** created when there are blockers or questions that need customer input. Do not create it for routine progress summaries — deliverables in `.project/iterations/<N>/<story-id>.md` and `bd list` serve that purpose.

When REVIEW.md is needed (`.project/iterations/<N>/REVIEW.md`):
- Blockers preventing progress
- Questions requiring customer decisions
- Proposed direction changes needing approval

Notify the project's Channel when creating a REVIEW.md.

### Iteration Transitions

When the last bead in an iteration closes, the worker auto-generates `RETRO.md` (see `references/worker.md` §8) before marking the iteration complete. This retrospective is available for the next check-in.

After check-in:
1. Mark current iteration complete in ITERATION.md (if agreed)
2. Create next iteration directory
3. Write new ITERATION.md with stories from review discussion
4. Create beads tasks for new stories

## Beads Quick Reference

```
bd ready              # List unblocked tasks
bd create "Title"     # Create task
bd update <id> --claim  # Claim a task
bd update <id> --close  # Close completed task
bd show <id>          # View task details
bd list               # List all tasks
bd dep add <a> <b>    # Add dependency (a depends on b)
bd dep list <id>      # List dependencies of a bead
```

## Cron Integration

Set up a recurring cron job for the orchestrator:

```json
{
  "schedule": { "kind": "every", "everyMs": 300000 },
  "payload": {
    "kind": "agentTurn",
    "message": "You are the braids orchestrator. Read and follow ~/.openclaw/skills/braids/references/orchestrator.md"
  },
  "sessionTarget": "isolated"
}
```

The orchestrator checks for work and spawns workers — it never does bead work itself.

## Worker Spawning & Parallel Execution

The projects system supports parallel worker execution with configurable concurrency per project.

### MaxWorkers

Each project's `.project/PROJECT.md` includes a `MaxWorkers` field (default: 1) that controls how many workers can work on the project simultaneously. This prevents resource contention and keeps work serialized when needed.

### Session Labeling

Sub-agents spawned for project work use the label convention `project:<slug>` (e.g., `project:my-app`). This allows the cron worker to count active workers per project by querying `sessions_list` and filtering for labels with that prefix.

### Concurrency Check Flow

When the cron worker fires, it follows this sequence for each active project:

1. Read `.project/PROJECT.md` to get `MaxWorkers` (default 1)
2. Call `sessions_list` and collect sessions whose label starts with `project:<slug>`
3. **Detect and clean up zombie sessions** (see below) — exclude them from the count
4. If `healthy running workers >= MaxWorkers`, skip the project entirely
5. Otherwise, proceed to claim and work tasks

This ensures the system never over-subscribes a project. For most projects, `MaxWorkers: 1` is appropriate — it keeps work sequential and avoids merge conflicts or duplicated effort. Increase it for projects with independent, parallelizable workstreams.

### Zombie Session Detection

A **zombie session** is a worker that has finished (or stalled) but still appears in the session list. Without cleanup, zombies block new workers from spawning due to `MaxWorkers` limits.

The orchestrator detects zombies using these criteria (in priority order):

1. **Non-running status** — sessions with status `completed`, `failed`, `error`, or `stopped` are excluded from the worker count
2. **Closed bead** — if the bead id from the session label is already closed, the worker is done but the session lingered
3. **Excessive runtime** — sessions running longer than 60 minutes are treated as likely zombies

When a zombie is detected, the orchestrator kills the session and sends a brief notification to the project's Channel (if `blocker` notifications are enabled).

See `references/orchestrator.md` §Zombie Detection for full details.

### Orchestrator vs Worker Architecture

The system uses a two-tier architecture:

1. **Orchestrator** (cron job) — Runs every 5 minutes. Reads registry, finds active iterations, checks concurrency, and spawns worker sessions via `sessions_spawn`. Does NO bead work itself. See `references/orchestrator.md`.

2. **Worker** (spawned session) — Receives a specific bead assignment. Claims the bead, does the work, writes a deliverable, closes the bead, commits, and sends notifications. See `references/worker.md`.

Workers are spawned with label `project:<slug>:<bead-id>` so the orchestrator can count active workers per project. The `MaxWorkers` cap applies to spawned workers — the orchestrator doesn't count toward it.

### Orchestrator Frequency Scaling

The orchestrator automatically reduces polling frequency when there's no work to do. It writes `~/.openclaw/braids/.orchestrator-state.json` after each run, tracking idle state and reason. On subsequent runs, it checks this file and skips execution if the backoff interval hasn't elapsed:

- **No active iterations** → polls every 30 minutes (vs. default 5)
- **No ready beads** → polls every 15 minutes
- **All projects at capacity** → polls every 10 minutes
- **Work available** → clears idle state, resumes normal 5-minute polling

This saves significant token usage when projects are paused or between iterations.

### Worker Error Handling

Workers handle errors via a structured escalation path (see `references/worker.md` for full details):

1. **Recoverable errors** — Retry once, try alternatives, then escalate
2. **Blockers** — Mark bead as `blocked`, notify Channel, stop work
3. **Questions** — Notify Channel, continue other aspects if possible
4. **Guardrail conflicts** — Never violate; block and escalate
5. **Partial completion** — Write deliverable for completed work, block with context

## Format Compatibility

When the skill format evolves, existing projects may have stale `.project/PROJECT.md` or ITERATION.md files. The system handles this gracefully at two levels:

### Worker Tolerance (Automatic)

Workers and orchestrators **must be tolerant of older formats**. When reading `.project/PROJECT.md` or ITERATION.md:

- **Directory migration fallback:** If `.project/PROJECT.md` doesn't exist, check for `PROJECT.md` at root. Similarly, if `.project/iterations/` doesn't exist, check `iterations/` at root. This provides backwards compatibility during migration.

- **Missing fields → use defaults.** If a field doesn't exist, use its default value silently. Key defaults:
  - `MaxWorkers` → `1`
  - `Autonomy` → `full`
  - `Priority` → `normal`
  - `Checkin` → `on-demand`
  - `Channel` → (none — skip notifications if missing)
  - `Notifications` table → all events `on`
  - ITERATION.md `Guardrails` section → (none — no extra constraints)
  - ITERATION.md `Notes` section → (none)
- **Unknown fields → ignore.** Projects may have custom fields. Don't error on them.
- **Old field names → treat as missing.** If a field was renamed, treat the old name as absent and apply the default. Don't try to map old names automatically.

This means workers never crash or block due to a missing field in PROJECT.md or ITERATION.md. They degrade gracefully.

### Skill Migration (User-Triggered)

For intentional format updates, users can request migration via the project's Channel. See `references/migration.md`.

### Breaking Changes (Migration Beads)

When a skill update introduces a **breaking change** that tolerance alone can't handle (e.g., a required new directory, a fundamentally restructured workflow, or removed functionality), the skill maintainer should:

1. Create migration beads in affected projects describing the required changes
2. Document the breaking change and migration path in the skill's commit message
3. Update `references/migration.md` if the migration process itself changes

Breaking changes should be rare. Prefer additive, backwards-compatible changes whenever possible.

## Skill Migration

When the skill format evolves (e.g., PROJECT.md fields change, ITERATION.md structure updates), existing projects need migration. This is **user-triggered, not automatic**.

### How It Works

A user sends a message in the project's Channel like:
- "update to latest skill format"
- "migrate project format"
- "sync with current skill template"

The agent handling the channel message should then follow `references/migration.md`.
