# Standardize terminology: use 'worker' not 'agent' (projects-skill-nkx)

## Summary
Replaced all references to "agent" (when referring to spawned sessions doing project work) with "worker" throughout SKILL.md and PROJECT.md.

## Details
- `MaxAgents` → `MaxWorkers` in SKILL.md format and PROJECT.md
- `sub-agents` → `workers` throughout SKILL.md
- "Agent Spawning" section → "Worker Spawning"
- `running agents` → `running workers` in concurrency logic
- Preserved "agent" where it refers to the interactive check-in agent or cron payload types (`agentTurn`)
