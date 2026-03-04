# braids-2lz: Enforce NO_REPLY Except for Defined Notification Events

## Summary
Updated worker.md to enforce that workers use NO_REPLY for all output except defined notification events (bead-start, bead-complete, blocker, question, iteration-complete, no-ready-beads). This prevents channel spam from intermediate messages.

## Changes Made
- Edited ~/.openclaw/skills/braids/references/worker.md in the "Channel Discipline" section to add enforcement language.

## Verification
- Read CONTRACTS.md: Does not exist or contain relevant instructions.
- No code changes required; documentation update only.
- bb test passed (no regressions).