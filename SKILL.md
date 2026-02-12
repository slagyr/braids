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
        <story-id>.md            # Deliverable per story
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
MaxAgents: <number, default 1>

## Goal
<High-level description of what this project aims to achieve>

## Guardrails
<Project-specific constraints and boundaries>
```

No "current focus" here — beads (`bd ready`) determines what to work on.

## Notifications

Send a message to the project's Channel when:
- All stories planned for the current iteration are complete
- No more ready (unblocked) beads exist for the project
- A question arises that requires customer input
- A blocker is encountered that prevents progress

## ITERATION.md Format

```markdown
# Iteration <N>

Status: active | complete

## Stories
- bd-xxxx: <title>
- bd-yyyy: <title>

## Guardrails
<Iteration-specific constraints>

## Notes
<Any relevant context>
```

Completed iterations are **immutable** — never modify them.

## Story Deliverables

Each story produces `iterations/<N>/<story-id>.md`:

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

### Discord Channel for Check-ins

When creating a project, create a dedicated Discord text channel for notifications and check-ins:

- Use the `message` tool with `action: channel-create`
- Name: `project-<slug>` (e.g., `project-my-app`)
- Place under a "Projects" category if one exists (create it if not)
- Set the channel topic to the project goal from PROJECT.md
- Record the channel id in PROJECT.md's `Channel` field

This channel receives:
- Iteration completion notifications
- Blocker/question alerts
- Check-in review summaries

### Working a Project (Background Sessions)

1. Read registry, find active projects
2. For each active project with an active iteration:
   - Read PROJECT.md — note the `MaxAgents` setting (default 1)
   - Check running sessions (via `sessions_list`) for sub-agents with label prefix `project:<slug>`
   - **If running agents >= MaxAgents, skip this project**
   - Read ITERATION.md for the ordered story list
   - Run `bd ready` to find unblocked tasks
   - **Prioritize work in ITERATION.md story order first**, then by bead priority for non-iteration tasks
   - Each task: claim (`bd update <id> --claim`), do work, write deliverable to `iterations/<N>/<story-id>.md`, close (`bd update <id> -s closed`)
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
  "schedule": { "kind": "every", "everyMs": 14400000 },
  "payload": {
    "kind": "agentTurn",
    "message": "Check ~/projects/registry.md for active projects. For each active project with an active iteration, run bd ready and work on unblocked tasks. Commit progress."
  },
  "sessionTarget": "isolated"
}
```

Adjust frequency based on bandwidth and project needs.
