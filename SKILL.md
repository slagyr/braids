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
5. Create `iterations/001/` with ITERATION.md and initial stories via `bd create`
6. Add to `~/projects/registry.md`
7. Commit

### Working a Project (Background Sessions)

1. Read registry, find active projects
2. For each active project with an active iteration:
   - Run `bd ready` to find unblocked tasks
   - Spawn sub-agents for parallelizable work
   - Each sub-agent: claim task (`bd update <id> --claim`), do work, write deliverable, close task
   - Update PROGRESS in beads comments
3. Commit progress

### Preparing for Check-in

Before a scheduled review:

1. Prepare `iterations/<N>/REVIEW.md`:
   - Summary of work completed this iteration
   - Demo points (what to show)
   - Blockers or decisions needed
   - Proposed stories for next iteration
2. Notify via configured channel

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
