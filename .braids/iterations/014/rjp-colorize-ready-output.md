# Colorize braids ready output (braids-rjp)

## Summary

Added ANSI color output to `braids ready` command's `format-ready-output` function.

## Changes

### `src/braids/ready.clj`
- Added ANSI color map (`colors`) with red, green, yellow, cyan, bold-white, reset
- Added `colorize` helper function
- Added `priority-color` function: P0/P1 → red, P2 → yellow, P3 → green
- Updated `format-ready-output` to colorize:
  - Priority badges (P0/P1=red, P2=yellow, P3=green)
  - Bead titles (bold white)
  - Project names (cyan)

### `spec/braids/ready_spec.clj`
- Added 6 new tests for color output:
  - P1 priority renders in red
  - P2 priority renders in yellow
  - P3 priority renders in green
  - P0 priority renders in red (high priority)
  - Bead title renders in bold white
  - Project name renders in cyan

## Verification

```
$ bb -e '(require (quote [braids.ready :as r])) (println (r/format-ready-output [{:project "myproj" :id "abc-123" :title "Fix the bug" :priority "P1"} {:project "other" :id "def-456" :title "Add feature" :priority "P3"}]))'
1) [P1] abc-123: Fix the bug (myproj)    # P1 in red, title bold white, project cyan
2) [P3] def-456: Add feature (other)      # P3 in green, title bold white, project cyan

$ bb test | grep examples
432 examples, 12 failures, 821 assertions
# All 12 failures are pre-existing (integration/install tests), none from this change
```
