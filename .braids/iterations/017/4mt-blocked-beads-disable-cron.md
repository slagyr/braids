# braids-4mt: Blocked beads should not keep orchestrator alive

## Summary

Fixed the disable-cron logic so blocked beads don't keep the orchestrator polling. Only workable (non-blocked) open beads keep the cron alive.

## Changes

### `src/braids/orch.clj`
- Fixed `has-open-beads` check to filter out blocked beads using `remove`
- Fixed `str/lower-case` → `name clojure.string/lower-case` (the previous attempt had a broken namespace reference)

### `spec/braids/orch_spec.clj`
- Updated existing test: blocked-only beads now expect `disable-cron true` (was incorrectly `false`)
- Added new test: mix of blocked and open beads correctly returns `disable-cron false`

## Verification

```
$ bb test
459 examples, 10 failures, 855 assertions
```

All 10 failures are pre-existing integration tests. No orch test failures.

Key test cases:
- All open beads blocked → disable-cron: true ✓
- Mix of blocked + open beads → disable-cron: false ✓
- No open beads → disable-cron: true ✓ (existing)
- Backward compat (no open-beads param) → disable-cron: false ✓ (existing)
