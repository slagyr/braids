# Orchestrator: fire all spawns without waiting, exit immediately after (braids-9ne)

## Summary
Updated orchestrator.md to explicitly instruct the orchestrator to fire all sessions_spawn calls back-to-back without waiting for responses, and to exit immediately after firing spawns and killing zombies. This prevents timeouts caused by waiting for tool call overhead. The cron timeout is already configured to 120 seconds in the skill.

## Details
Modified the "Spawn Workers — Fire and Forget" section in braids/references/orchestrator.md to add a CRITICAL note emphasizing that the orchestrator must not wait for the sessions_spawn tool call response before firing the next one, as tool calls have overhead and waiting would cause timeouts. All spawns must be fired in rapid succession without blocking on any tool call.

The orchestrator's job is complete the moment it fires the spawns — it should not linger.

## Verification
- Reviewed the updated orchestrator.md to confirm the explicit instructions.
- Confirmed the cron job timeout is set to 120 seconds in the skill configuration.

## Assets
None.
