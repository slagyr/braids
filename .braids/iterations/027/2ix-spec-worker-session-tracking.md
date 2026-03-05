# Spec: Worker Session Tracking

## Summary
Created comprehensive Gherkin specifications for worker session tracking, focusing on deterministic session ID generation and prevention of duplicate spawning. The specs define how session IDs are created consistently from bead IDs and how the system prevents multiple workers from spawning for the same bead.

## Changes Made
- Created `spec/features/worker_session_tracking.feature` with 7 scenarios covering:
  - Deterministic session ID generation from bead IDs
  - Consistency of session IDs for the same bead
  - Uniqueness of session IDs for different beads
  - Prevention of duplicate spawning when session is active
  - Allowing spawning when no active session exists
  - Handling missing bead data gracefully
  - Detection and handling of session ID collisions

- Created `spec/step_defs/worker_session_tracking.clj` with regex-based step definitions that mock session tracking logic and verify expected behavior

## Scenarios Covered
1. **Deterministic ID generation**: Session IDs are generated predictably from bead IDs (format: "braids-<bead-id>-worker")
2. **Consistency**: Same bead always generates the same session ID
3. **Uniqueness**: Different beads generate different session IDs
4. **Duplicate prevention**: Cannot spawn worker if session already active for bead
5. **Normal spawning**: Allows spawning when no active session exists
6. **Missing data handling**: Sessions without corresponding beads are marked for cleanup
7. **Collision detection**: System detects and handles rare session ID collisions

## Verification
### Feature Run
```
$ bb features
Worker session tracking - passed
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
- `spec/features/worker_session_tracking.feature` - Gherkin scenarios for session tracking behavior
- `spec/step_defs/worker_session_tracking.clj` - Step definitions with regex matching and behavior verification

These specs will guide the implementation of session tracking logic in the orchestrator and serve as acceptance tests for preventing duplicate worker spawning.