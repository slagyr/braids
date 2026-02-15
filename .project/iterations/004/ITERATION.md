# Iteration 004

- **Status:** complete

## Stories
- projects-skill-9xp: AGENTS.md as universal entry point: point to worker.md, support non-orchestrator agents
- projects-skill-0ox: Orchestrator should detect and clean up zombie worker sessions
- projects-skill-s0m: Remove SKILL.md runtime dependency from orchestrator and worker
- projects-skill-3b3: Simplify worker spawn message — rely on AGENTS.md for onboarding (depends on 9xp)
- projects-skill-k0j: Add runTimeoutSeconds to worker spawn to hard-kill long-running sessions
- projects-skill-u1b: Orchestrator frequency scaling — reduce polling when no active iterations exist
- projects-skill-eqi: bd init integration — auto-generate AGENTS.md, directory structure, and registry entry
- projects-skill-rin: Structural test script: validate files, symlinks, registry, and spawn config
- projects-skill-b25: CONTRACTS.md: document invariants for orchestrator, worker, and file formats
- projects-skill-6q1: Simulation tests: scripted orchestrator/worker scenarios against a test project
- projects-skill-szh: Integration smoke tests: end-to-end validation after each iteration
- projects-skill-fqr: Support skill migration command via project channel
- projects-skill-463: Handle skill updates gracefully across existing projects
- projects-skill-zun: Add cleanup: delete to worker spawn to prevent zombie sessions by default

## Guardrails
- 9xp (AGENTS.md entry point) is foundational — must land first
- 3b3 depends on 9xp — do not start until 9xp is closed
- rin and b25 should land early — all subsequent beads must follow test-first development
- P0/P1 beads before P2
- Test changes against all three active projects (projects-skill, zane-setup, wealth)

## Notes
- Theme: token efficiency, robustness, and universal entry points
- Several beads address issues discovered during iteration 003 (zombie workers, path confusion, token overhead)
