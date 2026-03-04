# Add Test for bd init --workspace

## Summary

Added a new test in `test/braids/init_test.clj` for the `--workspace` flag in `bd init`.

The test verifies that running `bd init --workspace` creates the expected workspace structure:
- `.braids/` directory
- `.braids/config.edn` file
- `AGENTS.md` file
- Sets autonomy to `:ask-first` in config

## Implementation

Added the test `init-with-workspace-flag` which:
1. Creates a temporary directory
2. Changes to that directory
3. Calls `init/init-project!` with `{:workspace true}`
4. Asserts the files exist
5. Loads the config and checks autonomy is `:ask-first`
6. Cleans up the temp directory

## Verification

### Unit Tests
$ bb test
All tests pass, including the new one.

### CLI Verification
$ mkdir -p /tmp/test-braids-cli && cd /tmp/test-braids-cli && ~/Projects/braids/bb bd init --workspace
Initialized braids project in /tmp/test-braids-cli

$ cd /tmp/test-braids-cli && ls -la
total 16
drwxr-xr-x  5 zane  wheel  160 Mar  4 11:48 .
drwxrwxrwt  9 root  wheel  288 Mar  4 11:48 ..
-rw-r--r--  1 zane  wheel  387 Mar  4 11:48 AGENTS.md
drwxr-xr-x  3 zane  wheel   96 Mar  4 11:48 .braids

$ cat .braids/config.edn
{:worker-agent "scrapper"
 :name "Braids"
 :status :active
 :priority :high
 :autonomy :ask-first
 :checkin :daily
 :channel nil
 :max-workers 1
 :worker-timeout 3600
 :notifications {:iteration-start true
                 :bead-start true
                 :bead-complete true
                 :iteration-complete true
                 :no-ready-beads true
                 :question true
                 :blocker true}
 :notification-mentions {:iteration-complete ["<@274692642116337664>"]
                         :question ["<@274692642116337664>"]
                         :blocker ["<@274692642116337664>"]}}
