# Add Configurable Notification Table to PROJECT.md (braids-9bd)

## Summary
Added a per-project notifications table to PROJECT.md format allowing each notification event to be toggled on/off.

## Details
- Added Notifications table to PROJECT.md template in SKILL.md with 7 event types (iteration-start, bead-start, bead-complete, iteration-complete, no-ready-beads, question, blocker)
- Added reference documentation explaining each event type
- All events default to `on` if the table is missing
- Updated worker.md to check the Notifications table before sending messages
- Added the Notifications table to this project's own PROJECT.md with all events enabled
