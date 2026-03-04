# braids-01z: Document the 'private project' pattern in SKILL.md

## Changes Made

1. Updated the config table to reflect the correct default for `:worker-thinking` (`"high"`, not `"low"`).

2. Added documentation for the 'private project' pattern in the "Advanced Configuration" section.

   The new text explains that for projects needing dedicated agents (e.g., proprietary models, isolated contexts, or sensitive data), you create a dedicated agent with the desired configuration and set `:worker-agent` in `config.edn` to route workers through it.

   This ensures consistent behavior, model access, and isolation.

## Testing

- Verified the SKILL.md file exists and is editable.
- Checked that the changes align with the project's TDD process (this is documentation, so no tests needed).
- Ensured the documentation is clear and actionable.