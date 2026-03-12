# braids-l137: Update @wip spawn log scenario to new multi-spawn format

## Changes Made

1. **Added new scenario to features/orch_runner.feature**: "spawn log shows multiple worker commands" with detailed Given/When/Then steps matching the new multi-spawn format as specified.

2. **Updated format-spawn-log in src/braids/orch_runner.clj**:
   - Added `bead-suffix` helper function to extract suffix from bead ID (e.g., 'alpha-aa1' -> 'aa1')
   - Modified format-spawn-log to use `<bead-suffix> → <full command>` format instead of indented arrow

## Verification

Ran `bb test` - all specs pass.

### Test Output
```
Testing braids.core-test
Testing braids.orchestrator-test
Testing braids.orch-runner-test
Testing braids.orchestrator-io-test
Testing braids.init-test

Finished in 0.123 seconds
19 examples, 0 failures
```