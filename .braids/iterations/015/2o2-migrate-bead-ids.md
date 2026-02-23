# Migrate Bead IDs from projects-skill-* to braids-*

## Summary

Renamed all 107 bead IDs from `projects-skill-*` prefix to `braids-*` using `bd rename-prefix braids-` and bulk sed updates across iteration.edn files, deliverable .md files, and SKILL.md.

## What Was Done

1. **`bd rename-prefix braids-`** — Renamed all 107 beads in the JSONL database
2. **Updated all iteration.edn files** (001–015) — sed replaced `projects-skill-` with `braids-` in story references
3. **Updated all deliverable .md files** — References within deliverable content updated
4. **Updated SKILL.md** — Example bead IDs now use `braids-` prefix
5. **Updated PROJECT.md, AGENTS.md, init.md** — All references updated
6. **Cleaned up auxiliary files** — `.beads/last-touched`, `.braids/iterations/005/.completing`

## Verification

```
$ bd list | head -3
◐ braids-2o2 [● P2] [task] @zane - Migrate bead IDs from projects-skill- to braids-

$ bd show braids-2o2
◐ braids-2o2 · Migrate bead IDs from projects-skill- to braids-   [● P2 · IN_PROGRESS]
Owner: zane · Assignee: zane · Type: task

$ bd ready
✨ No ready work found (all issues have blocking dependencies)

$ grep -rc 'projects-skill-' .braids/iterations/*/iteration.edn
(all zeros — no stale references in iteration configs)
```

## Test Results

`bb test`: 437 examples, 11 failures. All 11 failures are **pre-existing** integration smoke tests related to iteration format mismatches (plain string vs map stories in iteration.edn) and other project state issues — not caused by this rename.

## Notes

- Historical references to `projects-skill-` in deliverable prose (e.g., "migrated from projects-skill") were intentionally left as-is — they describe what happened, not active IDs
- The `.beads/beads.db` binary still contains old references but is rebuilt from JSONL on next access
