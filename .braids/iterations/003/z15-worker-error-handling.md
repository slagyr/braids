# Formalize worker error handling and escalation path (braids-z15)

## Summary

Added a structured "Error Handling & Escalation" section to `references/worker.md` and a summary in `SKILL.md`, giving workers clear guidance on how to handle failures.

## Details

### Added to `references/worker.md`

New section covering six error scenarios:

1. **Recoverable Errors** — Retry once, try alternative approaches, then escalate as blocker
2. **Blockers** — Mark bead `blocked`, notify Channel with context, stop work
3. **Questions** — Notify Channel, continue other aspects if possible
4. **Guardrail Violations** — Never violate; block and escalate the conflict
5. **Unclaimed Bead** — Stop immediately if claim fails, report back to orchestrator
6. **Partial Completion** — Write deliverable for completed portion, block with remaining work context

### Added to `SKILL.md`

Summary subsection under "Orchestrator vs Worker Architecture" documenting the escalation path for quick reference.

### Design Decisions

- Workers should **never guess** around blockers — incorrect results are worse than blocked beads
- Retries are limited to **one attempt** to avoid infinite loops in subagent sessions
- Partial work is preserved via deliverables so it's not lost when a bead blocks
- Claim failures cause immediate stop since the bead state is uncertain
