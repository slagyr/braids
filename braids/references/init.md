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

cat > ~/.openclaw/braids/registry.md << 'EOF'
# Projects

| Slug | Status | Priority | Path |
|------|--------|----------|------|
EOF
```

If using a custom `BRAIDS_HOME`, replace `~/Projects` with the desired path. The registry and orchestrator state always live in `~/.openclaw/braids/` regardless of `BRAIDS_HOME`.

### 4. Set Up Orchestrator Cron Job

Create the cron job that drives autonomous project work:

```json
{
  "schedule": { "kind": "every", "everyMs": 300000 },
  "payload": {
    "kind": "agentTurn",
    "message": "You are the projects orchestrator. Read and follow ~/.openclaw/skills/braids/references/orchestrator.md"
  },
  "sessionTarget": "isolated"
}
```

Use the OpenClaw cron tool to register this. The orchestrator runs every 5 minutes, checks for active projects, and spawns workers as needed. It automatically scales back polling when there's no work (see SKILL.md §Orchestrator Frequency Scaling).

### 5. (Optional) Create Your First Project

Follow [`project-creation.md`](project-creation.md) — an interactive guide that walks through gathering project info, scaffolding the directory, and generating real `.braids/config.edn` content.

## Verification

After setup, confirm everything is in place:

- [ ] `~/.openclaw/skills/braids/SKILL.md` exists (symlink works)
- [ ] `bd --version` succeeds
- [ ] `~/.openclaw/braids/registry.md` exists with the header row
- [ ] Orchestrator cron job is registered
- [ ] (If created) first project appears in `registry.md`

## What Happens Next

The orchestrator cron fires every 5 minutes. When it finds active projects with active iterations and ready beads, it spawns worker sessions to do the work. No manual intervention needed — just add stories and set iterations to `active`.
