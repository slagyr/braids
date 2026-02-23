# Iteration 005 Retrospective

## Summary
Iteration 005 focused on testing maturity and iteration lifecycle reliability. Migrated the entire test suite from bash to speclj on Babashka, fixed the race condition causing duplicate iteration-complete notifications, and ensured RETRO.md is always generated when iterations complete.

## Completed
| Bead | Title | Deliverable |
|------|-------|-------------|
| braids-9zf | Migrate all tests to speclj + Babashka | 9zf-migrate-tests-to-speclj.md |
| braids-um4 | Prevent duplicate iteration-complete notifications | um4-prevent-duplicate-iteration-complete.md |
| braids-rbl | Workers must generate RETRO.md before marking iteration complete | rbl-retro-generation-enforcement.md |

## Key Decisions
- `.completing` lock file chosen over ITERATION.md status check for race condition prevention — simpler and atomic
- Speclj-style framework implemented as a lightweight custom helper rather than pulling in the full speclj dependency (Babashka compatibility)
- Retroactively generated iteration 004 RETRO.md rather than leaving the gap

## Lessons Learned
- The race condition in iteration 004 was a two-part problem: duplicate notifications (um4) and missing RETRO.md (rbl) — splitting into separate beads worked well
- Bash test scripts were hard to maintain at scale; speclj specs are significantly more readable and composable
- Pre-existing test failures from other projects (wealth, zane-setup format mismatches) need attention in a future iteration

## Carry-Forward
- 45 pre-existing test failures from other projects' structural/integration specs need fixing (format mismatches in wealth PROJECT.md, missing @mention in zane-setup)
- Consider adding a spec that validates RETRO.md content structure (required sections) beyond just file existence
