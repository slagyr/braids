# Braids

Autonomous background project management for OpenClaw agents. Enables long-running projects with iterative delivery, parallel sub-agent execution, and structured check-ins.

## Install

```bash
bash <(curl -fsSL https://raw.githubusercontent.com/slagyr/project-skill/main/install.sh)
```

This clones the repo to `~/.openclaw/braids-skill` and symlinks the skill into OpenClaw. To install to a custom location, set `BRAIDS_INSTALL_DIR` first:

```bash
BRAIDS_INSTALL_DIR=~/my/path bash <(curl -fsSL https://raw.githubusercontent.com/slagyr/project-skill/main/install.sh)
```

Once installed, ask your agent to "set up braids" to complete first-time setup (verify dependencies, create braids home, configure the orchestrator cron, and scaffold your first project). See [`references/init.md`](braids/references/init.md) for details.

## Details

See [SKILL.md](braids/SKILL.md) for the full workflow, configuration, and reference docs.

## License

Part of the OpenClaw skill ecosystem.
