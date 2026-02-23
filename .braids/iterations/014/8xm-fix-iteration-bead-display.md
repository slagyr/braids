# Fix braids iteration command bead display (braids-8xm)

## Problem

The `braids iteration` command showed `? : [unknown]` for all beads because `iteration.edn` stores stories as plain string IDs (`["bead-id-1" "bead-id-2"]`), but `annotate-stories` and `story-ids` expected map entries (`{:id "x" :title "y"}`).

## Changes

### `src/braids/iteration.clj`

- Added `normalize-story` helper that converts both string IDs and map entries to `{:id ... :title ...}` format
- Updated `annotate-stories` to handle both formats, pulling titles from bead data when stories are plain strings
- Updated `story-ids` to handle both string and map story entries

### `spec/braids/iteration_spec.clj`

- Added 3 new tests:
  - `annotate-stories` with string story IDs and matching beads
  - `annotate-stories` with string story IDs and missing beads
  - `story-ids` with string story format

## Backwards Compatible

Map-format stories still work as before. String-format stories now resolve titles from bead data, falling back to the bead ID itself if no bead is found.
