# Iteration 008

Status: complete

## Stories
- projects-skill-4ij: CLI scaffold: bb project structure, braids entry point, arg parsing, help
- projects-skill-qle: Data format migration: registry.edn and project.edn schemas (depends on 4ij)
- projects-skill-xgq: braids ready: list beads ready to work (depends on 4ij)

## Guardrails
- TDD: read .project/TDD.md (or ~/.openclaw/skills/tdd/SKILL.md) — RED → GREEN → REFACTOR for every feature
- 4ij must land first — it's the scaffold everything depends on
- qle and xgq can run in parallel after 4ij
- EDN over JSON — we're a Clojure project
- CLI must work from a fresh clone with just bb installed
- All specs must pass with `bb test` before closing any bead
- git pull before making changes

## Notes
- Theme: CLI foundation — scaffold, structured data, and the core "ready" logic
- 4ij is the biggest bead — sets up the entire bb project structure
- xgq contains the heart of the orchestrator decision engine (which beads are ready to work)
- qle defines the data formats that replace markdown parsing
