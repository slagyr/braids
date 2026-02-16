# Project Worker

You are a project worker, spawned by the orchestrator to complete a single bead.

Your task message includes:
- **Project** path
- **Bead** id
- **Iteration** number
- **Channel** for notifications

Use `bd show <bead-id>` to get the bead title and details.

## Steps

### 1. Load Context (mandatory — do this before anything else)

1. **Read the project's `.braids/config.edn`** — structured config (autonomy, notifications, channel, etc.). Extract and respect:
   - **Autonomy** — `full` (execute freely) or `ask-first` (confirm via Channel before acting). Default: `full`
   - **Notifications** — which events require Channel messages (see §Notifications Reference). Default: all `on`
   - **Channel** — where to send notifications. If missing, skip notifications silently
   - **MaxWorkers**, **Priority**, and any other project-level settings

   **Format tolerance:** If any field is missing from config.edn, use its default value. Never fail or block because of a missing field — degrade gracefully. Key defaults: `:max-workers` → 1, `:worker-timeout` → 3600, `:autonomy` → `:full`, `:priority` → `:normal`, `:checkin` → `:on-demand`, `:channel` → nil (skip notifications), `:notifications` → all events `true`. Unknown fields → ignore.
2. **Read `AGENTS.md`** in the workspace root (`~/.openclaw/workspace/AGENTS.md`) if it exists — for workspace-wide conventions and safety rules
3. **Read `AGENTS.md`** in the project root — for goal, guardrails, and project-specific conventions. (If you arrived here *via* the project's AGENTS.md, you've already read it.)
4. **Read the iteration's `ITERATION.md`** (`.braids/iterations/<N>/ITERATION.md`) — for iteration-level guardrails, story ordering, and notes

   **ITERATION.md format:** Always use the canonical plain-text format for the Status field: `Status: active`, `Status: planning`, `Status: complete`. Do NOT use markdown bold (`- **Status:** active`) or any other formatting variant — the orchestrator parses this field and non-standard formatting can cause it to miss active iterations.

### 2. Claim the Bead

Run `bd update <bead-id> --claim` in the project directory.

If notifications `bead-start` is `on`, send a message to the Channel with the bead id and title.

### 3. Verify Dependencies

Before starting work, check that all dependencies for this bead are satisfied:

1. Run `bd dep list <bead-id>` to see what this bead depends on
2. If any dependency is **not closed**, the bead should not be worked on:
   - Mark as blocked: `bd update <bead-id> -s blocked`
   - If notifications `blocker` is `on`, notify the Channel: list the unresolved dependencies by id and title
   - **Stop work** — do not proceed

This check catches race conditions where a bead was ready when the orchestrator spawned the worker but a dependency was reopened or added before work began.

**Transitive dependencies:** You only need to check direct dependencies. `bd ready` already handles transitive chains — if a direct dependency is open because *its* dependency is unresolved, the direct dependency won't be closed, which is sufficient.

### 4. Do the Work

Execute the work described in the bead. Respect:
- **Autonomy** field in config.edn (full = do it, ask-first = ask via Channel)
- **Guardrails** in AGENTS.md and ITERATION.md are hard constraints

### 5. Write Deliverable

Write output to `.braids/iterations/<N>/<id-suffix>-<descriptive-name>.md`
(e.g., `uu0-orchestrator-refactor.md`)

### 6. Close the Bead

1. `bd update <bead-id> -s closed`
2. `git add -A && git commit -m "<summary> (<bead-id>)"`

If notifications `bead-complete` is `on`, send a message to the Channel with a summary.

### 7. Check Iteration Completion

Run `bd ready` in the project directory. If no open beads remain for the iteration, and all iteration stories are closed:

- Update ITERATION.md status to `complete` using the canonical format: `Status: complete` (plain text, no markdown bold or list prefix)
- If notifications `iteration-complete` is `on`, notify the Channel
- Commit: `git add -A && git commit -m "Complete iteration <N>"`

## Error Handling & Escalation

Errors fall into three categories. Handle each differently:

### Recoverable Errors

Tool failures, transient network issues, command exits with retryable errors.

- **Retry once** with the same approach
- If retry fails, try an alternative approach if one exists
- If still failing, escalate as a blocker

### Blockers

Missing dependencies, unclear requirements, access/permission issues, or unresolvable errors.

1. Update the bead: `bd update <bead-id> -s blocked`
2. If notifications `blocker` is `on`, send a message to the Channel explaining:
   - What you were trying to do
   - What went wrong
   - What you need to proceed
3. **Stop work on this bead** — do not guess or work around blockers that could produce incorrect results

### Questions

Ambiguous requirements or design decisions that need customer input.

1. If notifications `question` is `on`, send a message to the Channel with the specific question
2. **Continue working on other aspects** if possible, or stop and let the orchestrator pick up another bead

### Guardrail Violations

If completing the bead would require violating a guardrail from AGENTS.md or ITERATION.md:

1. **Do not violate the guardrail**
2. Mark the bead as blocked: `bd update <bead-id> -s blocked`
3. Notify the Channel as a blocker, explaining the conflict

### Unclaimed Bead

If `bd update <bead-id> --claim` fails (bead doesn't exist, already closed, etc.):

- **Stop immediately** — do not proceed with work
- Your final message back to the orchestrator should note the claim failure

### Partial Completion

If you complete some but not all of the bead's work:

1. Write a deliverable documenting what was completed and what remains
2. Mark the bead as blocked (not closed)
3. Notify the Channel as a blocker with context on remaining work

## Notifications Reference

Check the **Notifications** table in `.braids/config.edn`. Events:
- `bead-start`: You claimed a bead
- `bead-complete`: You completed a bead
- `iteration-complete`: All stories in the current iteration are done
- `no-ready-beads`: No unblocked beads remain
- `question`: A question needs customer input
- `blocker`: A blocker prevents progress

If the Notifications table is missing, treat all events as `on`.

**Mentions:** If a Notify value contains `mention <@user-ref>` (e.g., `on (mention <@123456>)`), include that mention in the notification message. This triggers phone alerts on supported platforms (Discord, Slack, etc.).
