# Spec: Orchestrator Spawning Behavior

## Summary
Created comprehensive Gherkin specifications for orchestrator spawning behavior, covering tick decisions, spawn conditions, and idle conditions. The specs define scenarios for how the orchestrator decides when to spawn workers, when to remain idle, and the conditions that influence these decisions.

## Changes Made
- Created `spec/features/orch_spawning.feature` with 8 scenarios covering:
  - Tick decisions for spawning based on ready beads and capacity
  - Partial spawning when fewer beads than max-workers
  - Idle decisions when no beads ready or at capacity
  - Spawn condition checks for project readiness
  - Idle condition reporting with specific reasons

- Created `spec/step_defs/orch_spawning.clj` with regex-based step definitions that mock and verify the behavior described in the scenarios

## Scenarios Covered
1. **Tick spawns when beads ready and capacity available**: Spawns up to max-workers when beads are ready
2. **Partial spawn when fewer beads**: Spawns only the number of ready beads when less than max-workers
3. **Idle when no ready beads**: No spawning when no work available
4. **Idle when at capacity**: No spawning when all workers are busy
5. **Spawn conditions for active iteration**: Project must have active iteration to spawn
6. **Reject spawn for planning iteration**: No spawning for non-active iterations
7. **Idle when no beads globally**: Reports idle when no work anywhere
8. **Idle when workers at capacity**: Reports idle when workers busy despite available work

## Verification
### Feature Run
```
$ bb features
Orchestrator spawning behavior - passed
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
- `spec/features/orch_spawning.feature` - Gherkin scenarios for spawning behavior
- `spec/step_defs/orch_spawning.clj` - Step definitions with regex matching and behavior verification

These specs will guide the implementation of orchestrator logic and serve as acceptance tests for the spawning behavior.