# braids-d005: Fix cron worker spawn — add `--session isolated`

## Problem

Workers spawned via `openclaw cron add --session-key agent:scrapper:braids-X-worker` still inherited the agent's main session context (`sessionKey: agent:scrapper:main`, `deliveryContext: {channel: discord}`). The `--session-key` flag alone does not create isolated sessions — it only sets the key within the existing session store, which still carries Discord channel context.

This caused workers to receive Discord channel delivery context, leading them to respond with `NO_REPLY` instead of executing their tasks silently.

## Fix

Added `"--session" "isolated"` to the `build-worker-args` function in `src/braids/orch_runner.clj`. The `--session isolated` flag tells OpenClaw to create a completely fresh session with no inherited channel context, ensuring workers operate in a clean environment.

### Source change (`src/braids/orch_runner.clj`)

Added `"--session" "isolated"` to the `base-args` vector in `build-worker-args`, between `--session-key` and `--at`:

```clojure
base-args ["cron" "add"
           "--name" (str "braids-" bead "-worker")
           "--message" task
           "--session-key" session-key
           "--session" "isolated"        ;; <-- NEW: fresh session, no channel context
           "--at" "+0s"
           "--delete-after-run"
           "--thinking" thinking
           "--timeout-seconds" timeout]
```

### Spec changes (`spec/braids/orch_runner_spec.clj`)

1. Added `--session` to the "includes required cron add args" test
2. Added new test: "includes --session isolated for fresh session without channel context" — verifies the flag is present and its value is `"isolated"`

## Verification

All `build-worker-args` specs pass (including the new one). No regressions introduced — the 4 pre-existing Gherkin generator failures are unchanged.
