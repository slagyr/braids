# Consolidate list and status into enhanced braids list

**Bead:** projects-skill-261

## Summary

Merged `braids status` functionality into `braids list`, creating a single rich table with colored output. Removed the separate `status` command.

## Changes

- **`src/braids/list.clj`** — Enhanced `format-list` to render 7 columns (SLUG, STATUS, PRIORITY, ITERATION, PROGRESS, WORKERS, PATH) with per-cell ANSI colors:
  - STATUS: active=green, paused/inactive=yellow, blocked=red
  - PRIORITY: high=red, low=yellow, normal=plain
  - PROGRESS: ≥100%=green, ≥50%=yellow, <50%=red
  - Projects without iterations show "—" for ITERATION/PROGRESS
- **`src/braids/list_io.clj`** — Now loads iteration data and worker counts (previously only done by status_io)
- **`src/braids/core.clj`** — Removed `status` command; updated `list` description
- **`spec/braids/list_spec.clj`** — 16 specs covering all columns, colors, empty states, JSON output

## Verification

```
$ braids list
SLUG        STATUS  PRIORITY  ITERATION  PROGRESS    WORKERS  PATH
----------  ------  --------  ---------  ----------  -------  ---------------------
braids      active  high      014        9/12 (75%)  0/1      ~/Projects/braids
zane-setup  active  high      1          0/4 (0%)    0/1      ~/Projects/zane-setup
wealth      active  high      —          —           0/1      ~/Projects/wealth
zaap        active  normal    003        5/5 (100%)  0/4      ~/Projects/zaap

$ braids status
Unknown command: status

$ braids list --json
[{"slug":"braids","status":"active","priority":"high","path":"~/Projects/braids","iteration":{"number":"014","stats":{"total":12,"closed":9,"percent":75}},"workers":0,"max_workers":1}, ...]

$ bb test
426 examples, 12 failures (all pre-existing), 815 assertions
```

All 12 failures are pre-existing integration/install tests unrelated to this change.
