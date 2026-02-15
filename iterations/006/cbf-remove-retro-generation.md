# Remove RETRO.md Auto-Generation Feature

## Summary

Removed the RETRO.md auto-generation feature and the `.completing` lock file mechanism from the worker workflow and contracts. This simplifies iteration completion to: check no open beads → update ITERATION.md status to complete → notify channel → commit.

## Changes

### worker.md
- **Step 7** simplified: removed `.completing` lock file guard and RETRO.md generation reference. Now just checks if iteration is complete, updates ITERATION.md, notifies, and commits.
- **Step 8** removed entirely (was "Generate Iteration Retrospective")

### CONTRACTS.md
- **Section 1.5 (RETRO.md)** removed; STATUS.md and .orchestrator-state.json renumbered to 1.5 and 1.6
- **Section 3.7** simplified: no longer references `.completing` lock or RETRO.md generation
- **Section 4.5** updated: removed RETRO.md from immutable iteration files list

### Specs Updated
- **contracts_spec.clj**: Tests now verify RETRO.md and `.completing` are NOT referenced in CONTRACTS.md
- **simulation_spec.clj**: Scenario 11 repurposed to verify RETRO removal; Scenario 16 simplified to verify no `.completing` mechanism; Scenario 2 no longer requires RETRO.md in completed iterations
- **integration_smoke_spec.clj**: Removed RETRO.md existence check for completed iterations; removed RETRO.md from deliverable exclusion filters
- **structural_spec.clj**: Removed RETRO.md existence check for completed iterations

### Test Results
- 140 examples, 5 pre-existing failures (unrelated to this change), 276 assertions
- All new/modified tests pass
- Pre-existing failures are integration checks on older iterations (deliverable matching, registry validation)
