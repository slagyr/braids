---
name: projects
description: Autonomous background project management with iterative delivery. Use when managing long-running projects, spawning background work sessions, tracking tasks with beads (bd), organizing iterations, or preparing check-in reviews. Triggers on project creation, status checks, work sessions, iteration planning, and review preparation.
---

# Projects

Manage long-running autonomous projects with iterative delivery cycles.

## Directory Layout

```
~/projects/
  registry.md                    # Master list of all projects
  <project-slug>/                # Each project is its own git repo
    PROJECT.md                   # Goals, guardrails, autonomy, budget
    .beads/                      # bd task tracking
    iterations/
      001/
        ITERATION.md             # Status, stories, iteration guardrails
        <id-suffix>-<name>.md   # Deliverable per story (e.g. w9g-extract-worker.md)
        assets/                  # Screenshots, artifacts
      002/
        ...
```

## Registry Format (`~/projects/registry.md`)

```markdown
# Projects

| Slug | Status | Priority | Path |
|------|--------|----------|------|
| my-project | active | normal | ~/projects/my-project |
```

Statuses: `active`, `paused`, `blocked`. No "complete" — pause permanently instead.

## PROJECT.md Format

```markdown
# <Project Name>

Status: active | paused | blocked
Priority: high | normal | low
Budget: unlimited | <constraint>
Autonomy: full | ask-first | research-only
Checkin: daily | weekly | on-demand
Channel: <discord channel id or name for check-ins>
MaxWorkers: <number, default 1>

## Goal
<High-level description of what this project aims to achieve>

## Notifications

| Event | Notify |
|-------|--------|
| iteration-start | on |
| bead-start | on |
| bead-complete | on |
| iteration-complete | on |
| no-ready-beads | on |
| question | on |
| blocker | on |

## Guardrails
<Project-specific constraints and boundaries>
```

No "current focus" here — beads (`bd ready`) determines what to work on.

## Notifications

Workers send notifications to the project's Channel based on the Notifications table in PROJECT.md. Each event type can be toggled `on` or `off` per project.

| Event | Description |
|-------|-------------|
| iteration-start | An iteration begins (status changed to active) |
| bead-start | A worker claims and begins working on a bead |
| bead-complete | A worker completes a bead (include summary in notification) |
| iteration-complete | All stories in the current iteration are done |
| no-ready-beads | No unblocked beads remain for the project |
| question | A question arises needing customer input |
| blocker | A blocker prevents progress |

All events default to `on` if the table is missing from PROJECT.md.

## ITERATION.md Format

```markdown
# Iteration <N>

Status: planning | active | complete

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

Each story produces `iterations/<N>/<id-suffix>-<descriptive-name>.md` where `<id-suffix>` is the last 3 characters of the bead id and `<descriptive-name>` is a short kebab-case summary (e.g., `w9g-extract-worker.md` for bead `projects-skill-w9g`):

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

1. Create directory under `~/projects/<slug>/`
2. Initialize git: `git init`
3. Initialize beads: `bd init`
4. Write PROJECT.md
5. Create a Discord channel for check-ins (see below)
6. Set the `Channel` field in PROJECT.md to the new channel id
7. Create `iterations/001/` with ITERATION.md and initial stories via `bd create`
8. Add to `~/projects/registry.md`
9. Commit

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

During check-in meetings in the channel, the agent should **only create beads and plan iterations** — not do project work. Work happens between meetings via the autonomous worker.

### Working a Project (Background Sessions)

1. Read registry, find active projects
2. For each active project with an active iteration:
   - Read PROJECT.md — note the `MaxWorkers` setting (default 1)
   - Check running sessions (via `sessions_list`) for workers with label prefix `project:<slug>`
   - **If running workers >= MaxWorkers, skip this project**
   - Read ITERATION.md for the ordered story list
   - Run `bd ready` to find unblocked tasks
   - **Prioritize work in ITERATION.md story order first**, then by bead priority for non-iteration tasks
   - Each task: claim (`bd update <id> --claim`), do work, write deliverable to `iterations/<N>/<id-suffix>-<descriptive-name>.md`, close (`bd update <id> -s closed`)
   - Commit after each completed story
3. Notify the project's Channel when all stories are done, no ready beads remain, or a blocker is hit

### Preparing for Check-in

REVIEW.md is **only** created when there are blockers or questions that need customer input. Do not create it for routine progress summaries — deliverables in `iterations/<N>/<story-id>.md` and `bd list` serve that purpose.

When REVIEW.md is needed (`iterations/<N>/REVIEW.md`):
- Blockers preventing progress
- Questions requiring customer decisions
- Proposed direction changes needing approval

Notify the project's Channel when creating a REVIEW.md.

### Iteration Transitions

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
bd dep add <a> <b>    # Add dependency
```

## Cron Integration

Set up a recurring cron job to trigger work sessions:

```json
{
  "schedule": { "kind": "every", "everyMs": 300000 },
  "payload": {
    "kind": "agentTurn",
    "message": "Check ~/projects/registry.md for active projects. For each active project with an active iteration, run bd ready and work on unblocked tasks. Commit progress."
  },
  "sessionTarget": "isolated"
}
```

Adjust frequency based on bandwidth and project needs.

## Worker Spawning & Parallel Execution

The projects system supports parallel worker execution with configurable concurrency per project.

### MaxWorkers

Each project's `PROJECT.md` includes a `MaxWorkers` field (default: 1) that controls how many workers can work on the project simultaneously. This prevents resource contention and keeps work serialized when needed.

### Session Labeling

Sub-agents spawned for project work use the label convention `project:<slug>` (e.g., `project:my-app`). This allows the cron worker to count active workers per project by querying `sessions_list` and filtering for labels with that prefix.

### Concurrency Check Flow

When the cron worker fires, it follows this sequence for each active project:

1. Read `PROJECT.md` to get `MaxWorkers` (default 1)
2. Call `sessions_list` and count sessions whose label starts with `project:<slug>`
3. If `running workers >= MaxWorkers`, skip the project entirely
4. Otherwise, proceed to claim and work tasks

This ensures the system never over-subscribes a project. For most projects, `MaxWorkers: 1` is appropriate — it keeps work sequential and avoids merge conflicts or duplicated effort. Increase it for projects with independent, parallelizable workstreams.

### Cron Worker vs Sub-Agent Spawning

The cron worker itself runs in an isolated session and works tasks directly — it does not spawn additional workers by default. The `MaxWorkers` check prevents multiple overlapping cron runs from working the same project. If a previous cron session is still active and working on the project, the next cron invocation will see it and skip.

For projects that benefit from parallelism (e.g., independent research tasks), the worker can use `sessions_spawn` with label `project:<slug>` to delegate work to workers, each taking a different story. The MaxWorkers cap still applies to the total count of spawned workers plus the worker itself.
