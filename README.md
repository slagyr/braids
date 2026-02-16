# Braids

Autonomous background project management for OpenClaw agents. Enables long-running projects with iterative delivery, parallel sub-agent execution, and structured check-ins.

## Install

### Homebrew (macOS)

```bash
brew install slagyr/tap/braids
```

This installs `braids` on your PATH along with its dependencies (`bb` and `bd`).

### Manual Install (all platforms)

For non-Mac users or if you prefer a manual setup:

```bash
bash <(curl -fsSL https://raw.githubusercontent.com/slagyr/braids/main/install.sh)
```

This clones the repo to `~/.openclaw/braids-skill` and symlinks the skill into OpenClaw. To install to a custom location, set `BRAIDS_INSTALL_DIR` first:

```bash
BRAIDS_INSTALL_DIR=~/my/path bash <(curl -fsSL https://raw.githubusercontent.com/slagyr/braids/main/install.sh)
```

**Prerequisites for manual install:** [Babashka](https://github.com/babashka/babashka) (`bb`) and [Beads](https://github.com/steveyegge/beads) (`bd`) must be installed separately.

### Post-Install Setup

Once installed, ask your agent to "set up braids" to complete first-time setup (verify dependencies, create braids home, configure the orchestrator cron, and scaffold your first project). See [`references/init.md`](braids/references/init.md) for details.

## Details

See [SKILL.md](braids/SKILL.md) for the full workflow, configuration, and reference docs.

## License

Part of the OpenClaw skill ecosystem.
