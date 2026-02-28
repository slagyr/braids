# braids-et3: CLI zombie detection and output

## Summary

Added `sessionId` threading through the zombie detection pipeline so the orchestrator agent can identify and kill zombie sessions.

## Changes

### `src/braids/orch.clj`
- **`detect-zombies`**: Now accepts optional `:session-id` in session maps and includes it in zombie entries when present
- **`format-orch-run-json`**: Zombie entries now explicitly formatted with `sessionId` (camelCase) when available

### `src/braids/orch_io.clj`
- **`load-sessions-from-stores`**: Extracts `sessionId` from session store JSON as `:session-id`
- **`parse-session-labels`**: Extracts `sessionId` from JSON session objects

### Tests
- 4 new tests covering session-id pass-through and JSON output formatting

## Output Shape

```json
{
  "action": "spawn|idle",
  "zombies": [{"slug":"...","bead":"...","label":"...","reason":"...","sessionId":"..."}],
  "disable_cron": true|false
}
```

`sessionId` only present when source provides it.
