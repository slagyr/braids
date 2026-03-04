# Clean up braids orch output format (braids-eca)

## Changes

### 1. Removed timestamps from output
- `orch_runner/log-line` now returns the message as-is instead of prepending `[yyyy-MM-ddTHH:mm:ss]`

### 2. DRY-RUN/CONFIRMED banner at top
- `run-orch!` now prints `── DRY-RUN ──` or `── CONFIRMED ──` as the very first line

### 3. Removed disable_cron from display
- `format-debug-output` no longer shows `[disable_cron: true/false]` in the decision line
- The field is still present in the tick result data and JSON output (used by cron logic)

### 4. Removed duplicate idle messages
- The separate `format-idle-log` call was removed from `run-orch!` — the debug output summary already shows the decision
- Also removed the "Orchestrator tick complete" noise line

### 5. Removed DRY-RUN bottom note
- The old `DRY-RUN mode — no workers were spawned` line at the bottom is gone — the banner at top is sufficient

## Files Changed
- `src/braids/orch_runner.clj` — simplified `log-line`
- `src/braids/orch.clj` — removed `disable_cron` from `format-debug-output`
- `src/braids/orch_runner_io.clj` — added banner, removed duplicate/noise lines
- `spec/orch_runner_spec.clj` — updated log-line tests
- `spec/orch_shell_spec.clj` — updated log-line and CLI tests
- `spec/braids/orch_spec.clj` — removed `disable_cron` display assertion

## Verification

```
$ braids orch
── DRY-RUN ──

  braids  active  iteration 024  → 2 beads
    ⚙️ eca  in_progress
    ○ 3sm  open
  wealth  active  (no iteration)
  zaap  active  (no iteration)
  cfii  active  (no iteration)

  → idle: all-at-capacity
```

```
$ braids orch --run
── CONFIRMED ──
(same clean output, workers actually spawned)
```

Test suite: 528 examples, 15 failures (all pre-existing integration/structural tests), 984 assertions. No regressions from this change.
