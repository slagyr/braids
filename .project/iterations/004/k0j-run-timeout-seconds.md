# Add runTimeoutSeconds to Worker Spawn (projects-skill-k0j)

## Summary

Added `runTimeoutSeconds` to the worker spawn call in the orchestrator, providing a hard kill for long-running worker sessions. This leverages OpenClaw's native `sessions_spawn` parameter (default 0 = no timeout) and sets a project-configurable default of 1800 seconds (30 minutes).

## Changes

### SKILL.md (PROJECT.md format)
- Added `WorkerTimeout` field (seconds, default 1800) to the PROJECT.md template

### references/orchestrator.md
- **§5 Spawn Worker**: Added `runTimeoutSeconds` parameter to the `sessions_spawn` call, sourced from the project's `WorkerTimeout` setting
- **§3 Zombie Detection**: Updated excessive runtime criterion to use `WorkerTimeout` instead of hardcoded 60 minutes; noted that `runTimeoutSeconds` is the primary defense while zombie detection handles edge cases
- **§3 Format tolerance**: Added `WorkerTimeout` → 1800 to the defaults list

### references/worker.md
- **Format tolerance**: Added `WorkerTimeout` → 1800 to the defaults list

## Design Decisions

- **30-minute default**: Most beads complete in 5-15 minutes. 30 minutes provides generous headroom while still catching runaway sessions within a reasonable window.
- **Per-project configurability**: Projects with known long-running beads can increase `WorkerTimeout` in their PROJECT.md.
- **Complementary to zombie detection**: `runTimeoutSeconds` is a hard guarantee from OpenClaw's runtime. Zombie detection remains as a secondary cleanup mechanism for sessions spawned before this change or edge cases where the hard kill didn't fire.
- **No changes to worker.md behavior**: Workers don't need to know about their timeout — it's enforced externally by the OpenClaw runtime.
