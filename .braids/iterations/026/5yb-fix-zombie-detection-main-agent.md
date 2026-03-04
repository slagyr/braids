# braids-5yb: Fix zombie detection for sessions spawned by main agent — kill any session with a closed bead

## Summary
Modified orchestrator to kill zombie sessions detected with closed beads, including those spawned by main agent.

## Changes Made
- Updated orch_runner_io.clj to kill zombie sessions after logging them, using subagents kill action.

## Verification
- bb test: All specs passed (no regressions).
- Manual test: Verified zombie killing logic in code.