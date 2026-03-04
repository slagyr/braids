# Update SKILL.md config.edn table

## Summary

Updated the config.edn table in SKILL.md to document the :worker-agent, :worker-model, and :worker-thinking fields.

## Implementation

Added rows to the config.edn table in SKILL.md:
- `:worker-agent`: Agent ID to spawn workers with (default "scrapper")
- `:worker-model`: Model override for worker agents (optional, default nil)
- `:worker-thinking`: Thinking level for worker agents (default :high)

## Verification

### Unit Tests
$ bb test
All tests pass.

### CLI Verification
Checked that the SKILL.md file has been updated with the new table entries.