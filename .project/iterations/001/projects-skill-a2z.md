# Document Agent Spawning Strategy in SKILL.md (projects-skill-a2z)

## Summary
Added a comprehensive "Agent Spawning & Parallel Execution" section to SKILL.md documenting MaxAgents, session labeling, concurrency checks, and the cron worker execution model.

## Details
The new section covers:
- **MaxAgents**: Per-project concurrency cap in PROJECT.md (default 1)
- **Session Labeling**: `project:<slug>` convention for identifying active agents per project
- **Concurrency Check Flow**: Step-by-step description of how the cron worker decides whether to work a project
- **Cron Worker vs Sub-Agent Spawning**: Clarifies that the cron worker works tasks directly by default, and explains when/how to use `sessions_spawn` for parallel workstreams
