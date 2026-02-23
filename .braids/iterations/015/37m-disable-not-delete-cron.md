# Deliverable: Orchestrator should disable not delete cron job (braids-37m)

## Summary

Updated all references to change orchestrator self-disable from "delete and recreate" to "disable/enable" the cron job, preserving the job definition.

## Changes

### `braids/references/orchestrator.md`
- Step 5: Changed `openclaw cron delete` → `openclaw cron disable <job-id>` with lookup instructions
- Re-activation now says `openclaw cron enable <job-id>` instead of recreating
- Troubleshooting section: Simplified to `openclaw cron rm braids-orchestrator` (by name) + recreate

### `braids/references/init.md`
- Cron JSON now includes `name`, `enabled`, `delivery`, and `timeoutSeconds` fields
- Added CLI equivalent command as alternative

### `braids/SKILL.md`
- Cron JSON updated with complete fields: `name`, `enabled`, `delivery`, `timeoutSeconds`
- Self-disable section: Changed from "deletes via `openclaw cron delete`" to "disables via `openclaw cron disable <job-id>`"
- Re-activation: Changed from "recreate" to `openclaw cron enable <job-id>`

### AC3: 'projects orchestrator' → 'braids orchestrator'
- Already fixed in a prior iteration; no instances of "projects orchestrator" found in init.md

## Verification

Tested disable/enable cycle with a real cron job:

```
$ openclaw cron add --name braids-test-disable --every 5m --session isolated --message "test" --timeout-seconds 60 --no-deliver --disabled
→ Created job 4380d4ef... enabled: false

$ openclaw cron enable 4380d4ef...
→ enabled: true, nextRunAtMs set

$ openclaw cron list | grep braids-test
→ Shows "idle" status with scheduled next run

$ openclaw cron disable 4380d4ef...
→ enabled: false, state cleared

$ openclaw cron rm braids-test-disable
→ Cleaned up test job
```

Note: `openclaw cron disable/enable` require the job UUID, not the name. The orchestrator must look up the ID via `openclaw cron list --json` first. `openclaw cron rm` does accept names.

## Test Suite

```
$ bb test
437 examples, 11 failures (all pre-existing), 828 assertions
```

All failures are pre-existing and unrelated to this change.
