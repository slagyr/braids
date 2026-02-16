# Specs Must Not Assume OpenClaw Is Installed

## Summary

Removed all hard dependencies on OpenClaw installation (`~/.openclaw/` paths) from spec files so they pass on a fresh clone.

## Changes

### simulation_spec.clj
- **Worker.md references** (4 occurrences): Changed from `home/.openclaw/skills/projects/references/worker.md` to `project-root/projects/references/worker.md` (project-relative)
- **Workspace AGENTS.md check**: Changed from asserting the installed file exists to verifying the contract documents the requirement (portable)

### structural_spec.clj
- **Skill Directory & SKILL.md Format tests**: Changed from using `skill-symlink` (`~/.openclaw/skills/projects`) to `skill-dir` (`skill-source`, project-relative path)
- **Skill Symlink tests**: Made conditional — skip gracefully if OpenClaw not installed
- **`openclaw cron list` test**: Already handled (catches exception, skips)

## Verification

All 144 specs run. The same 6 pre-existing failures remain (related to live project state assumptions, covered by bead `projects-skill-ilo`). No new failures introduced.
