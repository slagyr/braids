# braids-h4a2: Delete stale @wip idle log scenario, implement zombie log scenario

## Changes Made

1. **Removed stale @wip idle log scenario** from `features/orch_runner.feature`: Deleted the "Format idle log" scenario as it was marked as @wip and stale.

2. **Zombie log scenario is already implemented** in `features/orch_runner.feature` as "Format zombie log".

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