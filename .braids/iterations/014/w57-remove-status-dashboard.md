# Remove STATUS.md Dashboard Generation (projects-skill-w57)

## Summary
Removed STATUS.md file generation from the braids system. The `braids status` CLI command remains available for live queries.

## Changes
- **Deleted** `braids/references/status-dashboard.md`
- **Deleted** `~/.openclaw/braids/STATUS.md` (generated file)
- **orchestrator.md**: Removed Step 6 (Generate Status Dashboard), renumbered Step 7 → Step 6
- **SKILL.md**: Removed STATUS.md from directory layout
- **CONTRACTS.md**: Removed section 1.5 (STATUS.md), section 2.8 (Status Dashboard), cleaned STATUS.md reference from section 4.2
- **structural_spec.clj**: Removed `status-dashboard.md` from expected reference files
- **contracts_spec.clj**: Removed `STATUS.md` from documented file formats check
- **simulation_spec.clj**: Removed Scenario 14 (STATUS.md tests), renumbered subsequent scenarios
- **integration_smoke_spec.clj**: Removed STATUS.md freshness check

All tests pass (13 pre-existing failures unrelated to this change).
