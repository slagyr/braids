# Workers Must Generate RETRO.md Before Marking Iteration Complete (projects-skill-rbl)

## Summary
Ensured RETRO.md generation is reliably enforced when iterations complete. The `.completing` lock (added in um4) prevents race conditions where multiple workers skip retro generation. Added contract tests for the lock and RETRO requirement, and retroactively generated the missing iteration 004 RETRO.md.

## Changes

### Contract Tests Added (`spec/contracts_spec.clj`)
- **Iteration completion lock (.completing):** Verifies CONTRACTS.md documents the `.completing` lock file mechanism
- **RETRO.md generation on iteration completion:** Verifies CONTRACTS.md documents RETRO.md generation requirement

### Missing RETRO.md Generated (`iterations/004/RETRO.md`)
- Retroactively created the retrospective for iteration 004 which was missed due to the race condition
- Documents all 14 completed stories plus 2 bonus deliverables
- Includes key decisions, lessons learned, and carry-forward items

### Root Cause
The race condition (now fixed by um4's `.completing` lock) meant that when multiple workers finished their last beads simultaneously, each saw the iteration as complete but none took exclusive responsibility for generating RETRO.md. The lock ensures exactly one worker handles completion duties.

## Verification
- `bb run test` — both new contract tests pass (✓)
- Structural test for `iterations/004 has RETRO.md` now passes (✓)
- Pre-existing failures (45) are from other projects' format mismatches, unrelated to this bead
