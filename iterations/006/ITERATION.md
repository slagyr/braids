# Iteration 006

- **Status:** active

## Stories
- projects-skill-tw1: Add guardrail: channel agent must not modify project files directly — beads only
- projects-skill-kln: Replace homegrown test framework with actual speclj 3.12.0
- projects-skill-top: Delete tests/ directory — replaced by spec/ with speclj (depends on kln)
- projects-skill-cbf: Remove RETRO.md auto-generation feature — redundant with deliverables
- projects-skill-ly3: Move PROJECT.md and iterations/ into .project/ directory

## Guardrails
- kln must land first — real speclj is the foundation for all other testing
- top depends on kln
- ly3 is a breaking change — must include migration path for existing projects
- All beads must include speclj specs (test-first, use actual speclj library)

## Notes
- Theme: cleanup, simplification, and proper tooling
- kln fixes the biggest gap from iteration 005 — using actual speclj not a reimplementation
- ly3 changes project directory structure — coordinate with other active projects
