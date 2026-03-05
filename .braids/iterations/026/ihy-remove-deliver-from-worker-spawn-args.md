# braids-ihy: Remove --deliver from worker spawn args

## Summary
Added test to verify --deliver flag is not included in worker spawn args, preventing channel spam and routing issues.

## Changes Made
- Added test in test/braids/orch_runner_test.clj to ensure --deliver is not present in build-worker-args output.

## Verification
- bb test: All specs passed, including new test confirming --deliver not in args.
- CLI test: braids orch --dry-run runs without --deliver in spawn commands.