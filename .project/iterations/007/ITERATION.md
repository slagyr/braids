# Iteration 007

Status: active

## Stories
- projects-skill-3j9: Specs must not assume OpenClaw is installed
- projects-skill-ilo: Specs must not assume any projects exist
- projects-skill-o6o: Migrate iterations/ to .project/iterations/ for projects-skill

## Guardrails
- git pull before making any changes (Micah may have pushed changes)
- All specs must pass on a fresh clone with no OpenClaw installed and no projects in ~/Projects
- Use project-relative paths instead of ~/.openclaw/ paths
- Simulation/unit specs should use temp directories (like simulation_spec already does)
- structural_spec and integration_smoke_spec need the most rework — they currently validate live environment
- Test-first: write the failing spec assertions first, then fix
- Do NOT break specs that currently pass — refactor, don't delete valid coverage

## Notes
- Theme: spec portability — specs must work on any machine with just bb and the repo
- 3j9 and ilo can be worked in parallel — they touch different assumptions
- The simulation_spec is already a good model — it creates temp fixtures instead of reading live state
