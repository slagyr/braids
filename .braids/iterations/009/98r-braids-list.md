# Deliverable: braids list (braids-98r)

## Summary

Implemented the `braids list` command that reads registry.edn and displays all registered projects.

## Files Added/Changed

- `src/braids/list.clj` — Pure formatting functions: `format-list` (human table) and `format-list-json` (JSON)
- `src/braids/list_io.clj` — IO layer: loads registry via existing `ready-io` infrastructure
- `spec/braids/list_spec.clj` — 9 specs covering empty/single/multi projects, headers, alignment, and JSON output
- `src/braids/core.clj` — Wired `list` command into CLI dispatch

## Usage

```bash
bb braids list          # Human-friendly table
bb braids list --json   # Machine-consumable JSON
```

## Output Example

```
SLUG            STATUS  PRIORITY  PATH
--------------  ------  --------  -------------------------
projects-skill  active  high      ~/Projects/projects-skill
zane-setup      active  high      ~/Projects/zane-setup
```
