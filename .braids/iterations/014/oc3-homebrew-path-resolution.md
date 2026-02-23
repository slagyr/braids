# Fix Homebrew braids path resolution (braids-oc3)

## Problem

The Homebrew-installed `braids` (v0.1.0 at `/usr/local/Cellar/braids/0.1.0/`) has stale code that:
1. Only looks for `.project/iterations/*/ITERATION.md` (old markdown format)
2. Doesn't check `.braids/iterations/*/iteration.edn` (current EDN format)
3. Uses `parse-iteration-status` (regex on markdown) instead of `parse-iteration-status-edn` (EDN parsing)

This causes "No active iteration found" and "No ready beads" when the actual project uses `.braids/iterations/` with `iteration.edn` files.

The dev wrapper at `/usr/local/bin/braids` (`cd ~/Projects/braids && exec bb braids "$@"`) masks the issue by running source directly, but the actual Homebrew binary at `/usr/local/Cellar/braids/0.1.0/bin/braids` fails.

## Root Cause

The Homebrew formula pins to `tag: "v0.1.0"` which was cut before the migration from `.project/` to `.braids/` and from markdown to EDN. The installed code at the Cellar path is frozen at that old version.

## Changes

### Formula (Formula/braids.rb)
- Added `head` directive pointing to `main` branch, enabling `brew install --HEAD slagyr/tap/braids` for latest code
- This lets users get current code without waiting for a tagged release

### Test fixes
- **spec/braids/homebrew_spec.clj**: Updated repo reference from `slagyr/project-skill` to `slagyr/braids` (repo was renamed)
- **spec/braids/homebrew_spec.clj**: Added test verifying HEAD install support
- **spec/braids/install_spec.clj**: Updated repo reference from `slagyr/project-skill` to `slagyr/braids`

## Resolution Path

To fully fix for end users, a new version tag (e.g., `v0.2.0`) should be cut after iteration 014 completes, then the Formula's `url` tag should be updated. In the meantime, `brew install --HEAD` provides the fix.

## Verification

```
$ /usr/local/Cellar/braids/0.1.0/bin/braids iteration
No active iteration found.

$ braids iteration   # (dev wrapper - works)
Iteration 014 [active] — 10/12 done (83%)
...

$ grep "head " Formula/braids.rb
  head "https://github.com/slagyr/braids.git", branch: "main"
```

Test results: 2 previously failing specs (homebrew repo reference, install.sh repo reference) now pass. Remaining failures are unrelated integration tests checking live project state.
