# Label-Based Agent Routing

## Summary
Implemented label-based agent routing in the orchestrator, allowing beads with specific labels to be routed to specialized worker agents. Beads labeled "spec" are now routed to the "prowl" agent, while all others use the default "scrapper" agent.

## Changes Made
- Added `determine-worker-agent` function in `src/braids/orch_runner.clj` that checks bead labels for "spec" and routes accordingly
- Modified `build-worker-args` to use the determined worker agent
- Updated `src/braids/ready_io.clj` to properly extract label names from bead data
- Added comprehensive tests in `test/braids/orch_runner_test.clj` for label-based routing scenarios

## Implementation Details
- **Routing Logic**: Beads with "spec" label → "prowl" agent, others → "scrapper" (default)
- **Label Extraction**: Fixed ready bead gathering to extract label names as strings
- **Backward Compatibility**: Existing behavior preserved when no labels present

## Verification
### Tests
```
$ bb test
Testing braids.orch-runner-spec
...
Label-based agent routing
- routes 'spec' labeled beads to prowl agent
- routes non-spec beads to default scrapper agent
- routes beads with no labels to default scrapper agent
- handles multiple labels including spec

Finished in 0.00412 seconds
28 examples, 0 failures
```

### CLI Verification
Created test beads with labels and verified routing:
- Bead with "spec" label routes to prowl
- Bead with other labels routes to scrapper
- Bead with no labels routes to scrapper

## File Structure
- `src/braids/orch_runner.clj` - Added determine-worker-agent function
- `src/braids/ready_io.clj` - Fixed label extraction
- `test/braids/orch_runner_test.clj` - Added routing tests

This enables specialized agents like "prowl" for spec-related work while keeping general tasks on "scrapper".