# Replace PROJECT.md with .braids/config.edn (braids-6c5)

## Summary

Replaced PROJECT.md with `.braids/config.edn` as the canonical project configuration file. Config is now structured EDN only — goal and guardrails moved to AGENTS.md as prose. Added support for multiple mentions per notification event.

## Changes

### Source
- **`src/braids/project_config.clj`**: Added `normalize-mentions` to convert single mention strings to vectors. `parse-project-config` now strips `:goal` and `:guardrails` (those belong in AGENTS.md).
- **`src/braids/ready_io.clj`**: `load-project-config` now reads `.braids/config.edn` (preferred) or `.braids/project.edn` (legacy). Removed all markdown fallbacks and the `braids.migration` import.
- **`src/braids/new.clj`**: `build-project-config` no longer includes `:goal` or `:guardrails`. `build-agents-md` references `config.edn` and includes goal/guardrails sections.
- **`src/braids/new_io.clj`**: Writes `config.edn` instead of `project.edn`.
- **`src/braids/migration.clj`**: `plan-migration` generates `:write-config-edn` actions, handles legacy `project.edn` → `config.edn` migration.

### Docs
- **`CONTRACTS.md`**: Section 1.2 renamed from PROJECT.md to config.edn with EDN format, mentions support, no markdown fallback.
- **`braids/SKILL.md`**: All PROJECT.md references → config.edn; registry format now shows EDN; config.edn format shows EDN with notification-mentions.
- **`braids/references/worker.md`**: Worker loads config.edn; guardrails referenced from AGENTS.md.
- **`braids/references/project-creation.md`**: Step 5 generates EDN config; step 6 adds goal/guardrails to AGENTS.md.
- **`AGENTS.md`**: Now contains goal and guardrails sections; references config.edn.

### Project Config
- **`.braids/config.edn`**: Created for this project with all settings migrated from PROJECT.md.

### Tests
- Updated 9 spec files to reference config.edn, test mention normalization, verify goal/guardrails excluded from config.

## Test Results

368 examples, 6 failures (all pre-existing: homebrew/install.sh repo name, integration checks on live project state). Zero new failures.
