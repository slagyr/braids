# Braids Skill — First-Time Setup

Follow this guide when a user asks to "set up the braids skill" or similar. This is a **one-time setup** — once complete, the orchestrator cron handles everything.

## Prerequisites

- **OpenClaw** running with cron and sessions support
- **git** installed

## Steps

### 1. Install the Skill

Symlink the skill directory into OpenClaw's skills folder:

```bash
ln -s ~/Projects/projects-skill/braids ~/.openclaw/skills/braids
```

Verify:
```bash
ls ~/.openclaw/skills/braids/SKILL.md
```

If the skill source is elsewhere, adjust the symlink target accordingly.

### 2. Verify beads (`bd`) is Installed

```bash
bd --version
```

If `bd` is not found, install it following the [beads documentation](https://github.com/nickthecook/bd). The braids skill requires `bd` for all task tracking.

### 3. Create BRAIDS_HOME and State Directory

```bash
mkdir -p ~/Projects
mkdir -p ~/.openclaw/braids

cat > ~/.openclaw/braids/registry.edn << 'EOF'
{:projects []}
EOF
```

If using a custom `BRAIDS_HOME`, replace `~/Projects` with the desired path. The registry and orchestrator state always live in `~/.openclaw/braids/` regardless of `BRAIDS_HOME`.

### 4. Set Up Orchestrator Cron Job

Add to the system crontab (`crontab -e`):

```bash
*/5 * * * * /usr/local/bin/braids orch --confirmed >> /tmp/braids.log 2>&1
```

Or via one-liner:

```bash
(crontab -l 2>/dev/null; echo "*/5 * * * * /usr/local/bin/braids orch --confirmed >> /tmp/braids.log 2>&1") | crontab -
```

Verify it was added:

```bash
crontab -l
```

The orchestrator runs every 5 minutes, checks for active projects, and spawns workers as needed. Test with `braids orch` (dry-run by default) before enabling.

> **Note:** Use the system crontab, not OpenClaw cron. The braids orchestrator is a standalone CLI (`braids orch --confirmed`) and does not require OpenClaw's cron scheduler.

### 5. (Optional) Create Your First Project

Follow [`project-creation.md`](project-creation.md) — an interactive guide that walks through gathering project info, scaffolding the directory, and generating real `.braids/config.edn` content.

## Verification

After setup, confirm everything is in place:

- [ ] `~/.openclaw/skills/braids/SKILL.md` exists (symlink works)
- [ ] `bd --version` succeeds
- [ ] `~/.openclaw/braids/registry.edn` exists with `{:projects []}` content
- [ ] Orchestrator cron job is registered
- [ ] (If created) first project appears in `registry.edn`

## What Happens Next

The orchestrator cron fires every 5 minutes. When it finds active projects with active iterations and ready beads, it spawns worker sessions to do the work. No manual intervention needed — just add stories and set iterations to `active`.
