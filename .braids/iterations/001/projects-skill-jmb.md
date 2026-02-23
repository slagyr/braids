# Add MaxAgents to PROJECT.md, Cron Respects Running Agent Count (braids-jmb)

## Summary
Updated SKILL.md worker workflow to check MaxAgents before starting work on a project. The worker checks running sessions for sub-agents with the `project:<slug>` label prefix and skips projects that are already at capacity.

## Details
- MaxAgents was already in the PROJECT.md format (default 1)
- Added three steps to the worker workflow: read MaxAgents, check running sessions, skip if at capacity
- Sub-agent sessions use label prefix `project:<slug>` for identification
- This prevents over-parallelization and resource contention

## Assets
None.
