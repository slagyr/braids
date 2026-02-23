# Deliverable: braids new — interactive project creation (braids-4kp)

## Summary

Implemented the `braids new` CLI command for creating new projects. The command scaffolds a complete project structure from CLI arguments.

## Usage

```bash
braids new <slug> --name "Project Name" --goal "Project goal" [options]
```

### Required
- `<slug>` — positional, lowercase alphanumeric with hyphens
- `--name` — project display name
- `--goal` — project goal description

### Optional
- `--priority high|normal|low` (default: normal)
- `--autonomy full|ask-first|research-only` (default: full)
- `--checkin daily|weekly|on-demand` (default: on-demand)
- `--channel <id>` — notification channel
- `--max-workers <n>` (default: 1)
- `--worker-timeout <seconds>` (default: 3600)
- `--guardrails "constraint"` (repeatable)
- `--projects-home <path>` (default: ~/Projects)

## What It Creates

1. `<projects-home>/<slug>/` directory
2. `.braids/project.edn` — project config with all settings
3. `.braids/iterations/001/ITERATION.md` — iteration scaffold (planning status)
4. `AGENTS.md` — standard braids project agents file
5. Git repo initialized with initial commit
6. `bd init` for beads tracking
7. Registry entry added to `~/.openclaw/braids/registry.edn`

## Files

- `src/braids/new.clj` — pure logic (validation, config building, registry ops)
- `src/braids/new_io.clj` — IO layer (file operations, CLI arg parsing, git/bd commands)
- `spec/braids/new_spec.clj` — 14 specs for pure logic
- `spec/braids/new_io_spec.clj` — 8 specs for IO layer (with isolated temp dirs)
- `src/braids/core.clj` — wired in as `braids new` command

## Validation

- Slug format: `^[a-z0-9]([a-z0-9-]*[a-z0-9])?$`
- Rejects duplicate directories and duplicate registry slugs
- Requires name and goal

## Tests

All new specs pass. 306 total examples, 9 pre-existing integration failures (unrelated).
