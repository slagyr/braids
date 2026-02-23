# Fix PROJECT.md: incorrect skill path in guardrails (braids-7nd)

## Summary
Corrected the guardrails path in PROJECT.md to reference the actual SKILL.md location.

## Details
The guardrails section referenced `/Users/micahmartin/openclaw/skills/projects/SKILL.md`, but the skill definition lives in the project repo at `~/projects/projects-skill/SKILL.md` (which is symlinked from `~/.openclaw/skills/projects/`). Updated the path accordingly.
