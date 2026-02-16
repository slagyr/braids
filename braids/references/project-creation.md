# Creating a New Project

A conversational guide for agents creating a new project. Follow these steps when a human says "create a new project" (or similar).

## Prerequisites

- The braids skill is installed (`~/.openclaw/skills/braids/SKILL.md` exists)
- `bd` and `git` are available
- `BRAIDS_HOME` is set up (default: `~/Projects`)
- `~/.openclaw/braids/registry.md` exists

## Steps

### 1. Gather Information from the Human

Ask for:
- **Project name/slug** — lowercase alphanumeric with hyphens (e.g., `my-project`). The slug becomes the directory name.
- **Goal** — what does the project aim to achieve? Get a real description, not a placeholder.
- **Guardrails** — any constraints or boundaries for the project.

### 2. Suggest Defaults, Confirm Settings

Propose these defaults and let the human override:

| Setting | Default | Description |
|---------|---------|-------------|
| Priority | normal | high / normal / low |
| Autonomy | full | full / ask-first / research-only |
| Checkin | on-demand | daily / weekly / on-demand |
| MaxWorkers | 1 | Max parallel worker sessions |
| WorkerTimeout | 3600 | Seconds before worker is killed |

Ask if they want a **Discord channel** created for notifications. If yes, ask for a category (or suggest one). If they already have a channel, get the id.

### 3. Create the Discord Channel (if applicable)

Use the `message` tool:
```
action: channel-create
name: project-<slug>  (or whatever the human prefers)
topic: <project goal summary>
parentId: <category id if provided>
```

Record the resulting channel id for the `Channel` field.

### 4. Scaffold the Project

Run these commands in sequence:

```bash
SLUG="<slug>"
BRAIDS_HOME="${BRAIDS_HOME:-$HOME/Projects}"
PROJECT_DIR="$BRAIDS_HOME/$SLUG"

# Verify slug isn't taken
test ! -d "$PROJECT_DIR" || echo "ERROR: directory already exists"
grep -q "| $SLUG |" "$HOME/.openclaw/braids/registry.md" && echo "ERROR: slug in registry"

# Create directory and init
mkdir -p "$PROJECT_DIR/.project"
cd "$PROJECT_DIR"
git init -q
bd init -q
```

### 5. Generate PROJECT.md with Real Content

Write `.project/PROJECT.md` using the information gathered — **not** a template with TODOs. Example:

```markdown
# <Project Name>

- **Status:** active
- **Priority:** <from step 2>
- **Autonomy:** <from step 2>
- **Checkin:** <from step 2>
- **Channel:** <channel id from step 3, or blank>
- **MaxWorkers:** <from step 2>

## Notifications

| Event | Notify |
|-------|--------|
| iteration-start | on |
| bead-start | on |
| bead-complete | on |
| iteration-complete | on |
| no-ready-beads | on |
| question | on |
| blocker | on |

## Goal

<Real goal text from the human — not a placeholder>

## Guardrails

<Real guardrails from the human — not a placeholder>
```

If the human wants mentions on critical events (iteration-complete, question, blocker), add `(mention <@user-id>)` to those rows.

### 6. Set Up AGENTS.md

Copy the skill template:

```bash
cp ~/.openclaw/skills/braids/references/agents-template.md "$PROJECT_DIR/AGENTS.md"
```

If the template doesn't exist, write the standard AGENTS.md inline (see `references/agents-template.md` for the canonical version).

### 7. Initialize Iteration 001

```bash
mkdir -p "$PROJECT_DIR/.project/iterations/001"
```

Write `.project/iterations/001/ITERATION.md`:

```markdown
# Iteration 001

- **Status:** planning

## Stories

## Guardrails

## Notes
```

### 8. Optionally Seed Stories

If the human has initial stories/tasks, create them:

```bash
cd "$PROJECT_DIR"
bd create "Story title"
# repeat for each story
```

Then update `.project/iterations/001/ITERATION.md` to reference them in the Stories section.

If the human wants to start work immediately, set the iteration status to `active`.

### 9. Add to Registry

Append to `~/.openclaw/braids/registry.md`:

```
| <slug> | active | <priority> | <path> |
```

### 10. Initial Commit

```bash
cd "$PROJECT_DIR"
git add -A
git commit -m "Initialize project: <slug>"
```

### 11. Review with the Human

Before pushing or declaring done, show the human:
- The `.project/PROJECT.md` content
- The channel (if created)
- The registry entry
- Any seeded stories

Ask if anything needs adjustment. Make changes, amend the commit if needed.

## Notes

- The `Channel` field can be left blank if the human doesn't want notifications yet — the system degrades gracefully.
- If `BRAIDS_HOME` is non-standard, ask the human for the path.
- Slug validation: must match `^[a-z0-9]([a-z0-9-]*[a-z0-9])?$` (lowercase, digits, hyphens, no leading/trailing hyphens).
