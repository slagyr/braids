# Add configurable orchestrator channel to braids skill (braids-anx)

## Summary

Added support for a dedicated orchestrator channel, separate from per-project channels. The orchestrator can now post its own announcements (spawn decisions, idle events, zombie cleanups) to a dedicated channel instead of relying solely on cron delivery.

## Changes

### 1. Global config (`~/.openclaw/braids/config.edn`)
- Created the file with `:orchestrator-channel "1476813011925598343"` pointing to #orchestrator
- Added `:orchestrator-channel nil` to `config/defaults` in `src/braids/config.clj`

### 2. `orchestrator.md` updates
- Added "Orchestrator Channel" section explaining how to check for and use the channel
- Updated zombie cleanup step to mention posting summaries to orchestrator channel
- Updated idle/self-disable step to post idle reason to orchestrator channel
- Updated troubleshooting cron example to use `--deliver-to` for the orchestrator channel

### 3. `SKILL.md` documentation
- Added "Global Config" section documenting `~/.openclaw/braids/config.edn` format
- Documented `:orchestrator-channel` field with description and CLI usage
- Updated directory layout to show `config.edn` in `~/.openclaw/braids/`

### 4. Cron job delivery
- Already configured: the `braids-orchestrator` cron job delivers to `1476813011925598343` (#orchestrator)

## Verification

```
$ braids config list
braids-home = ~/Projects
orchestrator-channel = 1476813011925598343

$ braids config get orchestrator-channel
1476813011925598343

$ bb test (config specs)
All config-related specs pass. 10 pre-existing integration failures unchanged.
```

## Test Updates
- Updated `config_spec.clj` to expect `:orchestrator-channel nil` in parsed configs
- Updated `config_io_spec.clj` to expect the new default key in loaded configs
