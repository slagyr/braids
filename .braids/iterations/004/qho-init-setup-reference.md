# INIT.md: One-Time Skill Setup Reference (braids-qho)

## Summary

Created `references/init.md` — a step-by-step setup guide for first-time installation of the projects skill. Updated README.md and SKILL.md to point to it.

## Details

### New file: `projects/references/init.md`
A structured guide covering:
1. Symlink skill to `~/.openclaw/skills/projects`
2. Verify `bd` is installed
3. Create `PROJECTS_HOME` (`~/Projects`) and `registry.md`
4. Set up orchestrator cron job with recommended config
5. Optionally create first project via `projects-init`
6. Verification checklist

### Updated: `README.md`
- Replaced verbose manual installation steps with a pointer to `references/init.md` as the automated/guided approach
- Kept condensed manual steps as fallback
- Fixed paths to use `~/Projects` (capital P) consistently

### Updated: `projects/SKILL.md`
- Added "First-Time Setup" section near the top pointing to `references/init.md`

### New test: `tests/test_init_reference.sh`
Validates:
- `init.md` exists
- Contains all required sections
- References `projects-init` and `orchestrator.md`
- Both SKILL.md and README.md reference `init.md`
