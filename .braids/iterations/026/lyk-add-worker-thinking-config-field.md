# Add :worker-thinking config field

## Summary

Added :worker-thinking field to project config, defaulting to :high. Updated orch_runner.clj to use this config field instead of hardcoded 'low'.

## Implementation

- In src/braids/init.clj, added :worker-thinking :high to the base project config map.

- In src/braids/orch_runner.clj, modified build-worker-args to accept config as first parameter, and changed the thinking default from 'low' to (:worker-thinking config 'high')

- In src/braids/orch_runner_io.clj, modified the call to load the project config and pass it to build-worker-args.

- Added tests in test/braids/orch_runner_test.clj for the new behavior.

- Updated test/braids/init_test.clj to verify the config field is set.

## Verification

### Unit Tests
$ bb test
All tests pass.

### CLI Verification
$ mkdir -p /tmp/test-braids-normal && cd /tmp/test-braids-normal && ~/Projects/braids/bb bd init
Initialized braids project in /tmp/test-braids-normal

$ cd /tmp/test-braids-normal && cat .braids/config.edn | grep worker-thinking
:worker-thinking :high