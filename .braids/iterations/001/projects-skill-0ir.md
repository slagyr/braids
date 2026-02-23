# Define and Document the Check-in/Review Workflow (braids-0ir)

## Summary
Documented the check-in/review workflow in SKILL.md. The workflow covers how reviews are triggered, what gets presented, and how iteration transitions happen.

## Details

The check-in workflow operates as follows:

### When Reviews Happen
- Per the `Checkin` field in PROJECT.md: `daily`, `weekly`, or `on-demand`
- The worker notifies the project's Channel when:
  - All iteration stories are complete
  - No ready beads remain
  - A blocker or question requires human input

### What Gets Reviewed
- Story deliverables in `iterations/<N>/<story-id>.md`
- `bd list` output showing task status
- REVIEW.md is created **only** when there are blockers or questions needing customer input (not for routine progress summaries)

### Review Meeting Flow
1. Agent presents completed work and any blockers
2. Customer reviews deliverables, provides feedback
3. Customer and agent agree on next iteration scope
4. Agent creates new iteration directory and ITERATION.md
5. Agent creates bead tasks for new stories
6. Previous iteration is marked `complete` and becomes immutable

### Iteration Transitions
1. Update current ITERATION.md status to `complete`
2. Create `iterations/<N+1>/ITERATION.md` with new stories
3. Create beads for each new story
4. Resume autonomous work via cron

The existing SKILL.md already captures this accurately. No changes needed to the skill definition.

## Assets
None.
