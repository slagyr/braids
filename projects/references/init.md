# Projects Skill — First-Time Setup

Follow this guide when a user asks to "set up the projects skill" or similar. This is a **one-time setup** — once complete, the orchestrator cron handles everything.

## Prerequisites

- **OpenClaw** running with cron and sessions support
- **git** installed

## Steps

### 1. Install the Skill

Symlink the skill directory into OpenClaw's skills folder:

```bash
ln -s ~/Projects/projects-skill/projects ~/.openclaw/skills/projects
```

Verify:
```bash
ls ~/.openclaw/skills/projects/SKILL.md
```

If the skill source is elsewhere, adjust the symlink target accordingly.

### 2. Verify beads (`bd`) is Installed

```bash
bd --version
```

If `bd` is not found, install it following the [beads documentation](https://github.com/nickthecook/bd). The projects skill requires `bd` for all task tracking.

### 3. Create PROJECTS_HOME and Registry

```bash
mkdir -p ~/Projects

cat > ~/Projects/registry.md << 'EOF'
# Projects

| Slug | Status | Priority | Path |
|------|--------|----------|------|
EOF
```

If using a custom `PROJECTS_HOME`, replace `~/Projects` with the desired path.

### 4. Set Up Orchestrator Cron Job

Create the cron job that drives autonomous project work:

```json
{
  "schedule": { "kind": "every", "everyMs": 300000 },
  "payload": {
    "kind": "agentTurn",
    "message": "You are the projects orchestrator. Read and follow ~/.openclaw/skills/projects/references/orchestrator.md"
  },
  "sessionTarget": "isolated"
}
```

Use the OpenClaw cron tool to register this. The orchestrator runs every 5 minutes, checks for active projects, and spawns workers as needed. It automatically scales back polling when there's no work (see SKILL.md §Orchestrator Frequency Scaling).

### 5. (Optional) Create Your First Project

Use the `projects-init` script:

```bash
~/.openclaw/skills/projects/bin/projects-init my-project
```

Then complete the setup:
1. Edit `~/Projects/my-project/PROJECT.md` — fill in Goal, Guardrails, Channel
2. Create a Discord/notification channel and set the `Channel` field
3. Add stories: `cd ~/Projects/my-project && bd create "Story title"`
4. Update `iterations/001/ITERATION.md` with story references
5. Set iteration status to `active` when ready for work

## Verification

After setup, confirm everything is in place:

- [ ] `~/.openclaw/skills/projects/SKILL.md` exists (symlink works)
- [ ] `bd --version` succeeds
- [ ] `~/Projects/registry.md` exists with the header row
- [ ] Orchestrator cron job is registered
- [ ] (If created) first project appears in `registry.md`

## What Happens Next

The orchestrator cron fires every 5 minutes. When it finds active projects with active iterations and ready beads, it spawns worker sessions to do the work. No manual intervention needed — just add stories and set iterations to `active`.
