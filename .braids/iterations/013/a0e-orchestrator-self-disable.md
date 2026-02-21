# Orchestrator Self-Disables When Idle (projects-skill-a0e)

## Summary

Replaced the orchestrator's frequency-scaling backoff system with a simpler on/off self-disable mechanism. When the orchestrator finds no work, it instructs the agent to delete the cron job entirely, achieving zero token usage during idle periods.

## Changes

### Core Logic (`src/braids/orch.clj`)
- All idle tick results now include `:disable-cron true` in the returned map
- Covers all three idle reasons: `no-active-iterations`, `no-ready-beads`, `all-at-capacity`
- Spawn results do not include the field

### Orchestrator Reference (`braids/references/orchestrator.md`)
- Added Step 5: "Self-Disable on Idle" — instructs agent to run `openclaw cron delete` when `disable_cron` is true
- Renumbered subsequent steps

### SKILL.md
- Replaced "Orchestrator Frequency Scaling" section with "Orchestrator Self-Disable"
- Removed `.orchestrator-state.json` from directory structure listing
- Documents that re-activation is manual (part of iteration activation)

### CONTRACTS.md
- Replaced §1.6 `.orchestrator-state.json` with §1.6 "Orchestrator Self-Disable"
- Removed `.orchestrator-state.json` from infrastructure file list in §4.2

### Tests
- **orch_spec.clj**: 4 new tests for `disable-cron` presence/absence
- **simulation_spec.clj**: Replaced Scenario 4 (Frequency Scaling) with Scenario 4 (Self-Disable) — tests tick behavior and CONTRACTS.md documentation
- **integration_smoke_spec.clj**: Replaced `.orchestrator-state.json` validation with `disable-cron` contract test
- **contracts_spec.clj**: Updated format documentation check and replaced backoff test with self-disable test

## Re-activation Flow

When the channel agent activates a new iteration, it should re-create the orchestrator cron job. This is documented in SKILL.md § Cron Integration.
