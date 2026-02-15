# Iteration 004 Retrospective

## Summary
Iteration 004 focused on token efficiency, robustness, and universal entry points. All 14 stories were completed, establishing AGENTS.md as the universal project entry point, adding zombie detection, frequency scaling, structured testing (structural, simulation, integration), and formalizing system contracts. Two bonus beads (8pk, qho) were also delivered.

## Completed
| Bead | Title | Deliverable |
|------|-------|-------------|
| projects-skill-9xp | AGENTS.md as universal entry point | 9xp-agents-entry-point.md |
| projects-skill-0ox | Detect and clean up zombie worker sessions | (no separate deliverable — integrated into orchestrator.md) |
| projects-skill-s0m | Remove SKILL.md runtime dependency | s0m-remove-skillmd-runtime-dep.md |
| projects-skill-3b3 | Simplify worker spawn message | 3b3-simplify-spawn-message.md |
| projects-skill-k0j | Add runTimeoutSeconds to worker spawn | k0j-run-timeout-seconds.md |
| projects-skill-u1b | Orchestrator frequency scaling | u1b-frequency-scaling.md |
| projects-skill-eqi | bd init integration | eqi-bd-init-integration.md |
| projects-skill-rin | Structural test script | rin-structural-tests.md |
| projects-skill-b25 | CONTRACTS.md: document invariants | b25-contracts-document.md |
| projects-skill-6q1 | Simulation tests | 6q1-simulation-tests.md |
| projects-skill-szh | Integration smoke tests | szh-integration-smoke-tests.md |
| projects-skill-fqr | Support skill migration command | (no separate deliverable — documented in references/migration.md) |
| projects-skill-463 | Handle skill updates gracefully | (no separate deliverable — tolerance built into worker.md) |
| projects-skill-zun | Add cleanup: delete to worker spawn | (no separate deliverable — already fixed in orchestrator.md) |

### Bonus Deliverables
| Bead | Title | Deliverable |
|------|-------|-------------|
| projects-skill-8pk | Replace init script with project-creation.md | 8pk-replace-init-script.md |
| projects-skill-qho | INIT.md: one-time skill setup reference | qho-init-setup-reference.md |

## Key Decisions
- AGENTS.md became the universal entry point for all agents landing in a project, replacing direct worker.md references in spawn messages
- CONTRACTS.md formalized all system invariants as a single source of truth for correctness
- Test suite established with three tiers: structural (file validation), simulation (scripted scenarios), integration (live state checks)
- Worker spawn simplified to 4 fields (Project, Bead, Iteration, Channel) — workers self-onboard via AGENTS.md
- Frequency scaling uses idle state tracking with backoff intervals per reason

## Lessons Learned
- Multiple workers finishing beads simultaneously can race on iteration completion — led to duplicate notifications (addressed in iteration 005)
- RETRO.md was not generated despite being required — the race condition meant no single worker took responsibility (also addressed in iteration 005)
- Four beads had no separate deliverable files because the work was integrated directly into existing reference docs — deliverable naming convention should account for this

## Carry-Forward
- projects-skill-um4: Prevent duplicate iteration-complete notifications (race condition)
- projects-skill-rbl: Ensure RETRO.md is always generated on iteration completion
- Migrate bash test scripts to speclj + Babashka (projects-skill-9zf)
