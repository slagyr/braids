# Set Up Cron Job for Autonomous Work Sessions (braids-3w7)

## Summary
A recurring cron job (`523f5816-fd79-42d3-bd2a-75e0907f320e`, label: `projects-worker`) is configured and operational. It triggers isolated agent sessions that autonomously work on active projects.

## Details
The cron job:
- **Schedule:** Recurring (every 4 hours recommended in SKILL.md; actual interval set by user)
- **Session target:** `isolated` — runs in its own session, doesn't pollute the main conversation
- **Payload:** `agentTurn` that instructs the agent to read the projects registry, find active projects, and work unblocked tasks
- **Delivery:** Announces results back to the triggering context

The worker session follows this flow:
1. Read `~/projects/registry.md` for active projects
2. For each active project: read PROJECT.md, check MaxAgents against running sessions, read the active iteration, run `bd ready`, and work tasks in iteration order
3. Commit after each completed story
4. Notify the project's Channel when done or blocked

The SKILL.md documents the recommended cron configuration for other users to set up.

## Assets
None.
