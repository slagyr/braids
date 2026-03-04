# braids-gs1: Orch output: show workers:N/M and list in-progress/blocked beads per project

## Summary
Updated orchestrator output format to include active workers / max workers and list only in-progress/blocked beads per project.

## Changes Made
- Modified `format-debug-output` to accept workers map and filter beads to in-progress/blocked
- Changed bead status icons: ○ open, ● in-progress, ✗ blocked
- Updated project line format to include workers:N/M and beads: followed by status lines

## Verification
- bb test: All specs passed (no regressions).
- CLI test: Ran braids orch --dry-run, confirmed new format shows workers count and only active beads.