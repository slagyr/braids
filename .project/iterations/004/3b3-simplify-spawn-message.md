# Simplify Worker Spawn Message (projects-skill-3b3)

## Summary
Simplified the orchestrator's worker spawn message from 5 fields to 4, removing the bead title and the explicit `worker.md` path. Workers now discover their workflow via the project's `AGENTS.md` (established in 9xp).

## Changes

### orchestrator.md
- Spawn message reduced from:
  ```
  "You are a project worker. Read and follow ~/.openclaw/skills/projects/references/worker.md\n\nProject: <path>\nIteration: <N>\nBead: <bead-id>\nBead title: <title>\nChannel: <channel>"
  ```
  To:
  ```
  "Project: <path>\nBead: <bead-id>\nIteration: <N>\nChannel: <channel>"
  ```
- Added explanatory note about why the message is minimal (AGENTS.md handles routing)

### worker.md
- Updated task message field list to match new format (no title field)
- Added instruction to use `bd show` for bead title/details

### agents-template.md
- Updated detection heuristic from "task message includes a bead id" to "task message includes `Project:` and `Bead:` fields" for clearer routing

## Token Savings
- ~30 tokens per spawn (removed preamble sentence, title field, and explicit worker.md path)
- Scales with worker spawns per day across all projects
