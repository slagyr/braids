# Spec: Zombie Detection

## Summary
Created comprehensive Gherkin specifications for zombie detection behavior, covering scenarios for detecting stale worker sessions based on bead status and timeouts, as well as cleanup procedures. The specs define how the orchestrator identifies and removes zombie sessions to maintain system health.

## Changes Made
- Created `spec/features/zombie_detection.feature` with 7 scenarios covering:
  - Detection based on closed beads vs active sessions
  - Timeout-based detection when sessions exceed worker-timeout
  - Avoiding false positives for active workers
  - Cleanup procedures for detected zombies
  - Cross-project zombie detection
  - Error handling for missing session data
  - Reporting successful cleanup operations

- Created `spec/step_defs/zombie_detection.clj` with regex-based step definitions that mock zombie detection logic and verify expected behavior

## Scenarios Covered
1. **Zombie detection for closed beads**: Sessions active when bead is closed are marked as zombies
2. **Timeout detection**: Sessions exceeding worker-timeout are marked as zombies
3. **Active worker protection**: In-progress beads with active sessions are not marked as zombies
4. **Zombie cleanup**: Detected zombies are killed and logged
5. **Cross-project detection**: Zombies detected regardless of which project they belong to
6. **Missing session handling**: System continues processing when session data is missing
7. **Cleanup reporting**: Successful cleanup generates reports with killed sessions and reasons

## Verification
### Feature Run
```
$ bb features
Zombie detection - passed
```
All scenarios executed successfully with exit code 0.

### Tests
```
$ bb test
Testing braids.gherkin-spec
...
Finished in 0.00342 seconds
18 examples, 0 failures
```

## File Structure
- `spec/features/zombie_detection.feature` - Gherkin scenarios for zombie detection and cleanup
- `spec/step_defs/zombie_detection.clj` - Step definitions with regex matching and behavior verification

These specs will guide the implementation of zombie detection logic in the orchestrator and serve as acceptance tests for session cleanup behavior.