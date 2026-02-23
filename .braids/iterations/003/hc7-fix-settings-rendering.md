# Fix PROJECT.md settings rendering (braids-hc7)

## Summary
Converted PROJECT.md and ITERATION.md settings from plain `Key: value` lines to markdown bullet lists (`- **Key:** value`) so they render correctly as separate lines instead of collapsing into a single paragraph.

## Details
Plain line breaks in markdown are ignored during rendering, causing settings like Status, Priority, Autonomy, etc. to merge into one line. The fix converts all settings to bullet list items with bold keys.

### Files changed:
- `PROJECT.md` — 6 settings converted to list format
- `iterations/001/ITERATION.md` — Status field
- `iterations/002/ITERATION.md` — Status field
- `iterations/003/ITERATION.md` — Status field
- `projects/SKILL.md` — Both PROJECT.md and ITERATION.md templates updated
