# Iteration 004

- **Status:** active

## Stories
- projects-skill-9xp: AGENTS.md as universal entry point: point to worker.md, support non-orchestrator agents
- projects-skill-0ox: Orchestrator should detect and clean up zombie worker sessions
- projects-skill-s0m: Remove SKILL.md runtime dependency from orchestrator and worker
- projects-skill-3b3: Simplify worker spawn message — rely on AGENTS.md for onboarding (depends on 9xp)
- projects-skill-k0j: Add runTimeoutSeconds to worker spawn to hard-kill long-running sessions
- projects-skill-u1b: Orchestrator frequency scaling — reduce polling when no active iterations exist
- projects-skill-eqi: bd init integration — auto-generate AGENTS.md, directory structure, and registry entry
- projects-skill-fqr: Support skill migration command via project channel
- projects-skill-463: Handle skill updates gracefully across existing projects

## Guardrails
- 9xp (AGENTS.md entry point) is foundational — must land first
- 3b3 depends on 9xp — do not start until 9xp is closed
- P0/P1 beads before P2
- Test changes against all three active projects (projects-skill, zane-setup, wealth)

## Notes
- Theme: token efficiency, robustness, and universal entry points
- Several beads address issues discovered during iteration 003 (zombie workers, path confusion, token overhead)
