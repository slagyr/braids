# Deliverable: One-line install command in README (projects-skill-5ut)

## Summary

Updated the README with a correct one-liner install command and created an `install.sh` script.

## Changes

1. **`install.sh`** (new) — Bash install script that:
   - Clones `slagyr/project-skill` to `~/.openclaw/braids-skill` (configurable via `BRAIDS_INSTALL_DIR`)
   - Symlinks `braids/` into `~/.openclaw/skills/braids`
   - Supports updating existing installations (`git pull --ff-only`)
   - Uses `set -euo pipefail` for safety

2. **`README.md`** (updated) — Replaced old install section with:
   - One-liner: `bash <(curl -fsSL https://raw.githubusercontent.com/slagyr/project-skill/main/install.sh)`
   - Custom location example with `BRAIDS_INSTALL_DIR`
   - Fixed repo URL from `slagyr/braids` to `slagyr/project-skill`

3. **`spec/braids/install_spec.clj`** (new) — Structural tests verifying:
   - install.sh exists, is executable, has proper shebang and error handling
   - References correct repo
   - Creates skill symlink
   - README contains the one-liner with raw GitHub URL

## Test Results

All new specs pass. 9 pre-existing integration test failures (unrelated to this change).
