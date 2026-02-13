# Projects Skill

Autonomous background project management for OpenClaw agents. Enables long-running projects with iterative delivery, parallel sub-agent execution, and structured check-ins.

## Installation

**Automated setup:** Have your agent follow [`references/init.md`](projects/references/init.md) — it walks through every step: symlink the skill, verify `bd`, create `PROJECTS_HOME`, set up the orchestrator cron, and optionally scaffold your first project.

**Manual setup:** See below.

1. Symlink the skill into OpenClaw's skills folder:
   ```
   ln -s /path/to/projects-skill/projects ~/.openclaw/skills/projects
   ```

2. Install [beads](https://github.com/openclaw/beads) (`bd`) for task tracking:
   ```
   bd --version
   ```

3. Create the projects directory and registry:
   ```
   mkdir -p ~/Projects
   cat > ~/Projects/registry.md << 'EOF'
   # Projects

   | Slug | Status | Priority | Path |
   |------|--------|----------|------|
   EOF
   ```

4. Set up the orchestrator cron job (see SKILL.md §Cron Integration)

## Requirements

- **OpenClaw** with cron and sessions support
- **beads** (`bd`) CLI for task tracking
- **git** for version control
- **Discord** channel (optional, for check-in notifications)

## Usage

### Create a Project

```bash
mkdir -p ~/projects/my-project
cd ~/projects/my-project
git init
bd init
```

Then write `PROJECT.md`, create `iterations/001/ITERATION.md`, add stories with `bd create`, and register in `~/projects/registry.md`.

See `SKILL.md` for the full workflow.

## License

Part of the OpenClaw skill ecosystem.
