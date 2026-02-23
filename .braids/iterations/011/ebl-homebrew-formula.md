# Homebrew Formula for Braids (braids-ebl)

## Summary
Created `Formula/braids.rb` Homebrew formula and updated README with brew install as the primary installation method.

## Details

### Formula (`Formula/braids.rb`)
- Class `Braids < Formula` with description and homepage
- Source: git clone from `slagyr/project-skill` repo, tagged `v0.1.0`
- Dependencies: `borkdude/brew/babashka` (bb) and `beads` (bd)
- Installs source to libexec, creates a `braids` bin wrapper that runs `bb --config <libexec>/bb.edn braids "$@"`
- Includes a test block verifying `braids help` output

### README Updates
- Homebrew (`brew install slagyr/tap/braids`) listed as primary install method
- Manual install via `install.sh` retained for non-Mac users
- Post-install setup section guides users to ask their agent to "set up braids"

### Tests
All 12 homebrew spec tests pass (formula structure, dependencies, bin wrapper, README content).

## Note
The formula references `v0.1.0` tag — this tag needs to exist on the GitHub repo before `brew install` will actually work. The tap repo (`homebrew-tap` under `slagyr`) also needs to contain this formula or users need to use `brew install --formula` pointing at the local file.
