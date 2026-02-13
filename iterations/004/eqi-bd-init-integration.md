# bd init integration — auto-generate AGENTS.md, directory structure, and registry entry (projects-skill-eqi)

## Summary

Created `projects/bin/projects-init`, a shell script that automates the full project creation workflow. Instead of manually performing 10 steps, users run a single command that scaffolds the entire project structure.

## Details

### Script: `projects/bin/projects-init`

Automates the "Creating a Project" workflow from SKILL.md:

1. Creates project directory under `$PROJECTS_HOME/<slug>/`
2. Initializes git repository (`git init`)
3. Initializes bd task tracking (`bd init`)
4. Copies `AGENTS.md` from the skill's `references/agents-template.md` (with inline fallback)
5. Generates `PROJECT.md` template with all required fields and TODO markers
6. Creates `iterations/001/ITERATION.md` in `planning` status
7. Creates or appends to `$PROJECTS_HOME/registry.md`
8. Makes initial git commit

**Safety features:**
- Validates slug format (lowercase alphanumeric + hyphens)
- Rejects duplicate slugs (checks both directory and registry)
- Supports `--projects-home` flag for custom locations
- Respects `$PROJECTS_HOME` environment variable

### SKILL.md Updates

Updated the "Creating a Project" section to reference `projects-init` as the primary workflow, with manual post-setup steps listed after.

### Tests: `tests/test_projects_init.sh`

31 tests covering:
- Full project creation (directory, git, bd, AGENTS.md, PROJECT.md, iterations, registry)
- PROJECT.md content validation (all required fields, sections, notifications table)
- AGENTS.md content validation (worker.md reference, quick reference section)
- ITERATION.md content (planning status, stories section)
- Registry format and entries
- Git commit verification
- Duplicate prevention (directory and registry)
- Multiple project support (registry appending)
- Invalid slug rejection
- Missing arguments handling

## Assets

- `projects/bin/projects-init` — the init script
- `tests/test_projects_init.sh` — test suite (31 tests, all passing)
