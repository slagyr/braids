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

### 5. Generate config.edn with Real Content

Write `.braids/config.edn` using the information gathered — **not** a template with TODOs. Example:

```clojure
{:name "<Project Name>"
 :status :active
 :priority :<from step 2>
 :autonomy :<from step 2>
 :checkin :<from step 2>
 :channel "<channel id from step 3>"
 :max-workers <from step 2>
 :worker-timeout 3600
 :notifications {:iteration-start true
                 :bead-start true
                 :bead-complete true
                 :iteration-complete true
                 :no-ready-beads true
                 :question true
                 :blocker true}
 :notification-mentions {:iteration-complete ["<@user-id>"]
                         :question ["<@user-id>"]
                         :blocker ["<@user-id>"]}}
```

Mentions support multiple values per event (vector of strings). Omit `:notification-mentions` if no mentions are needed.

### 6. Set Up AGENTS.md

Write the project's `AGENTS.md` with goal, guardrails, and the standard worker entry point:

```bash
cp ~/.openclaw/skills/braids/references/agents-template.md "$PROJECT_DIR/AGENTS.md"
```

Then add the **Goal** and **Guardrails** sections from what the human provided. These are prose — they belong in AGENTS.md, not config.edn.

If the template doesn't exist, write the standard AGENTS.md inline (see `references/agents-template.md` for the canonical version).

### 7. Initialize Iteration 001

```bash
mkdir -p "$PROJECT_DIR/.braids/iterations/001"
```

Write `.braids/iterations/001/ITERATION.md`:

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

Then update `.braids/iterations/001/ITERATION.md` to reference them in the Stories section.

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
- The `.braids/config.edn` content
- The channel (if created)
- The registry entry
- Any seeded stories

Ask if anything needs adjustment. Make changes, amend the commit if needed.

## Notes

- The `Channel` field can be left blank if the human doesn't want notifications yet — the system degrades gracefully.
- If `BRAIDS_HOME` is non-standard, ask the human for the path.
- Slug validation: must match `^[a-z0-9]([a-z0-9-]*[a-z0-9])?$` (lowercase, digits, hyphens, no leading/trailing hyphens).
