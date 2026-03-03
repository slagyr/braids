# Deliverable: Orchestrator Fire-and-Forget Spawns (braids-9ne)

## Changes

### 1. orchestrator.md — Explicit fire-and-forget semantics
- Renamed step 3 to "Spawn Workers — Fire and Forget"
- Added bold instruction: fire ALL spawns back-to-back, use parallel tool calls if supported
- Added post-spawn instruction: exit IMMEDIATELY after spawns fired
- Renamed step 5 to "Done — Exit Immediately" with explicit "complete in under 60 seconds" target

### 2. SKILL.md & init.md — Cron timeout reduced to 120s
- Changed `timeoutSeconds` from 300 to 120 in both files
- Rationale: CLI runs in <5s, agent only needs to fire spawns; 2 minutes is generous

## Verification

```
$ grep -n "timeoutSeconds" braids/SKILL.md braids/references/init.md
braids/SKILL.md:316:    "timeoutSeconds": 120
braids/references/init.md:60:    "timeoutSeconds": 120

$ grep -c "Fire ALL spawns\|Exit Immediately\|fire-and-forget" braids/references/orchestrator.md
3
```

## Tests

Documentation-only changes — no code modified. Pre-existing test suite status unchanged (481 examples, 9 pre-existing failures unrelated to this change).
