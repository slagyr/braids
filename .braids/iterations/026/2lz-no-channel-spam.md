# braids-2lz: Workers must use NO_REPLY for everything except defined notification events

## Problem

Workers were posting intermediate progress/thinking updates to the Discord channel — step-by-step commentary like "## Reviewing...", "## Reading config...", etc. This creates noisy channel spam that drowns out actual notifications.

## Changes

### 1. `~/.openclaw/skills/braids/references/worker.md`

Added a **"Channel Discipline"** subsection under "### 4. Do the Work" that:
- Enumerates the **only** allowed notification events: `bead-start`, `bead-complete`, `blocker`, `question`, `iteration-complete`, `no-ready-beads`
- Explicitly prohibits intermediate progress updates, step-by-step commentary, thinking-out-loud messages, and status updates between start and complete
- States the principle: "Work silently"

### 2. `~/.openclaw/agents/scrapper/agent/AGENTS.md`

Added a new rule to scrapper's Rules section:
- **"No channel spam"** — reinforces that only defined notification events should be sent to the channel

## Rationale

The worker.md already had a Notifications Reference section listing the events, but it never explicitly said "don't send anything else." The gap was that workers (following their general agent training and SOUL.md/AGENTS.md personality) would narrate their work process to the channel. The fix makes the constraint explicit in both the shared worker protocol and the agent-specific config.

## Verification

Both files are documentation/instruction files (not code), so there are no automated tests to run. Verification is behavioral — the next worker session using these instructions should produce only `bead-start` and `bead-complete` notifications (plus blocker/question if applicable), with no intermediate commentary.

```
$ cat ~/.openclaw/skills/braids/references/worker.md | grep -A 20 "Channel Discipline"
#### Channel Discipline

**Only send messages to the Channel for defined notification events.** These are:
- `bead-start` — when you claim the bead (step 2)
- `bead-complete` — when you close the bead (step 6)
- `blocker` — when you hit a blocker and stop work
- `question` — when you need customer input
- `iteration-complete` — when the iteration finishes (step 7)
- `no-ready-beads` — when no unblocked beads remain

**Do NOT post any other messages to the Channel.** Specifically:
- No intermediate progress updates ("Reviewing code...", "Running tests...")
- No step-by-step commentary ("## Reading config...", "## Checking dependencies...")
- No thinking-out-loud messages
- No status updates between start and complete
```

```
$ grep "channel spam" ~/.openclaw/agents/scrapper/agent/AGENTS.md
- **No channel spam.** Only send messages to the channel for defined notification events: `bead-start`, `bead-complete`, `blocker`, `question`, `iteration-complete`, `no-ready-beads`. No progress updates, no thinking out loud, no intermediate commentary. Work silently.
```

## Note

Pre-existing parse error in `src/braids/orch.clj` (line 283, mismatched bracket) prevents `bb test` from running. This is unrelated to this bead — no code was changed, only documentation files.
