# Replace projects-init shell script with interactive project-creation.md reference doc

**Bead:** projects-skill-8pk
**Status:** Complete

## Summary

Replaced the `projects-init` shell script with `references/project-creation.md` — a conversational guide agents follow when a human says "create a new project." The script produced PROJECT.md files full of TODOs; the new reference doc instructs agents to gather real information from the human and generate populated content.

## Changes

- **Created** `projects/references/project-creation.md` — 11-step interactive guide covering: gathering project info, suggesting defaults, creating Discord channels, scaffolding directories, generating real PROJECT.md content, setting up AGENTS.md, initializing iteration 001, seeding stories, adding to registry, committing, and reviewing with the human.
- **Removed** `projects/bin/projects-init` — the shell script
- **Removed** `projects/bin/` — now-empty directory
- **Removed** `tests/test_projects_init.sh` — tests for the deleted script
- **Created** `tests/test_project_creation_reference.sh` — validates the new reference doc exists, has all required sections, old script is gone, and SKILL.md/init.md are updated (24 tests, all pass)
- **Updated** `projects/SKILL.md` — "Creating a Project" section now points to `project-creation.md` instead of describing the script
- **Updated** `projects/references/init.md` — step 5 now points to `project-creation.md`
- **Updated** `README.md` — "Create a Project" section references the new doc
- **Updated** `tests/test_init_reference.sh` — checks for `project-creation.md` instead of `projects-init`

## Test Results

New test `test_project_creation_reference.sh`: 24 passed, 0 failed. All other test failures are pre-existing.
