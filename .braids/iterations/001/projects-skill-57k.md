# Move SKILL.md to projects/SKILL.md Subdirectory (braids-57k)

## Summary
Verified that SKILL.md is already in the correct location. The skill directory (`~/.openclaw/skills/projects/`) is symlinked to the project repo (`~/projects/projects-skill/`), so the skill definition lives inside its own project — no move needed.

## Details
The directory structure is already correct:
- `~/.openclaw/skills/projects/` → symlink to `~/projects/projects-skill/`
- `SKILL.md` lives at the root of this directory, which is the standard OpenClaw skill layout

This is a self-referential project: the skill manages itself. The symlink approach means changes to the skill are tracked in the project's git repo.

## Assets
None.
