# Rename skill from 'projects' to 'braids' (braids-a4o)

## Summary

Renamed the skill from "projects" to "braids" across the entire codebase, infrastructure, and external config.

## Changes

### Directory & Symlink
- `projects/` → `braids/` (git mv)
- Symlink: `~/.openclaw/skills/projects` → `~/.openclaw/skills/braids`
- State directory: `~/.openclaw/projects/` → `~/.openclaw/braids/`

### Docs
- **SKILL.md:** frontmatter `name: braids`, heading `# Braids`, cron prompt references `braids`
- **README.md:** Updated install instructions, GitHub URL (`slagyr/braids`), all internal links
- **AGENTS.md:** Updated skill name reference and GitHub URLs
- **CONTRACTS.md:** Updated `~/.openclaw/projects/` → `~/.openclaw/braids/` throughout
- **references/**: All 6 reference docs updated (orchestrator.md, worker.md, init.md, migration.md, project-creation.md, agents-template.md, status-dashboard.md)

### Source Code
- `src/braids/core.clj`: Help text updated
- `src/braids/ready_io.clj`: Default state-home path updated

### Specs
- All spec files updated to reference `braids/` paths
- New `rename_spec.clj`: validates no stale "projects skill" references remain in mutable files
- `structural_spec.clj`: symlink target, skill-dir, state-home, cron name all updated

### External Config
- Cron job renamed from `projects-worker` to `braids-orchestrator`, prompt updated

## Migration Notes (Breaking Change)

Existing installs must:
1. Update symlink: `rm ~/.openclaw/skills/projects && ln -s <repo>/braids ~/.openclaw/skills/braids`
2. Move state directory: `mv ~/.openclaw/projects ~/.openclaw/braids`
3. Update cron job message to reference `~/.openclaw/skills/braids/references/orchestrator.md`
4. No data format changes — registry.edn/md and project.edn/PROJECT.md are unchanged

## What Was NOT Changed
- Completed iterations (immutable per Contract 4.5)
- `:projects` key in EDN data structures (domain concept, not skill name)
- `PROJECTS_HOME` env var and concept (still refers to where projects live)
- GitHub repo rename (requires manual action by repo owner)
