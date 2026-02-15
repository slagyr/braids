# Cron Should Prioritize Iteration Story Order, Then Bead Priority (projects-skill-h98)

## Summary
Updated SKILL.md's "Working a Project" section to explicitly state that tasks are worked in ITERATION.md story order first, then by bead priority for non-iteration tasks.

## Details
The previous documentation just said "run `bd ready` to find unblocked tasks" without specifying ordering. Updated to:
1. Read ITERATION.md for the ordered story list
2. Cross-reference with `bd ready` output
3. Work iteration stories in their listed order
4. Then work remaining ready beads by priority (P0 > P1 > P2)

Also updated the workflow to mention committing after each story and notifying on completion/blockers.

## Assets
None.
