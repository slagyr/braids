# braids-44k: CLI log every step to /tmp/braids.log

## Summary

Added structured logging to `braids orch-run` that appends timestamped entries to `/tmp/braids.log` on every run.

## Changes

- **New: `src/braids/orch_log.clj`** — Pure `format-log-lines` function + `write-log!` side-effect
- **Modified: `src/braids/core.clj`** — Integrated log writing into orch-run handler with timing
- **Modified: `src/braids/orch_io.clj`** — Added `:ready-beads` and `:workers` to debug-ctx returns
- **New: `spec/braids/orch_log_spec.clj`** — 8 specs covering all log entry types

## Log entries include

- Start timestamp header
- Registry project count
- Per-project: slug, status, active iteration (or none), worker counts
- Per-bead: id, status, title
- Zombie detections with reason
- Spawn/idle decision with disable_cron
- Duration in ms

## Verification

```
$ rm -f /tmp/braids.log && bb braids orch-run 2>/dev/null; cat /tmp/braids.log
=== orch-run 2026-02-27T16:32:31 ===
Registry: 5 projects
  braids  status=active  iteration=020  workers=0/1
    braids-73f  open  Rename orch-run back to orch-tick
    braids-7yg  open  Remove verbose mode from orchestrator
    braids-et3  open  CLI: perform zombie detection and include in output
    braids-42h  open  CLI: collect session information internally
    braids-44k  in_progress  CLI: log every step to /tmp/braids.log
  zane-setup  status=active  iteration=001  workers=0/1
    (no open beads)
  wealth  status=active  no active iteration
  zaap  status=active  no active iteration
  cfii  status=active  no active iteration
Decision: spawn 1 worker(s)
  Spawn: braids-42h
Duration: 866ms

$ bb -e "(require '[speclj.core :refer :all] '[braids.orch-log-spec]) (speclj.core/run-specs)"
8 examples, 0 failures, 17 assertions
```
