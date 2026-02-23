# Add 'planning' status for iterations (braids-isc)

## Summary
Added `planning` as an iteration status in SKILL.md, with documentation explaining that workers must only work `active` iterations.

## Details
- Updated ITERATION.md format: `Status: planning | active | complete`
- Added status descriptions clarifying that `planning` means stories are still being defined and workers must not pick up tasks, `active` means ready for work, and `complete` means immutable
- Existing worker language ("active iteration") already naturally excludes planning/complete iterations
