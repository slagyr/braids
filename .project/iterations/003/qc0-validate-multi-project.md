# Validate Multi-Project Support with Concurrent Active Projects (projects-skill-qc0)

## Summary

Validated that the projects skill correctly supports multiple concurrent active projects. The current registry has 3 active projects (`projects-skill`, `zane-setup`, `wealth`), each with distinct configurations. The orchestrator and worker architecture handles them correctly.

## Validation Results

### ✅ Registry Scanning
- 3 projects registered, all with `active` status
- Orchestrator iterates all active projects each cycle

### ✅ Active Iteration Filtering
- `projects-skill`: iteration 003 is `active` → workers spawned ✓
- `zane-setup`: iteration 001 is `planning` → correctly skipped ✓
- `wealth`: iteration 001 is `complete` → correctly skipped ✓
- Only projects with active iterations get workers, preventing wasted spawns

### ✅ Format Tolerance Across Projects
- `projects-skill/PROJECT.md`: standard format with bold markers
- `zane-setup/PROJECT.md`: includes legacy `Budget` field (unknown field, correctly ignored)
- `wealth/PROJECT.md`: uses different formatting style (no `**` bold markers), missing `MaxWorkers` and `Autonomy` fields (defaults apply: MaxWorkers=1, Autonomy=full)
- All three parse correctly under the format tolerance rules in SKILL.md

### ✅ Independent Configuration
Each project has distinct settings that are respected independently:
- **Autonomy**: projects-skill=full, zane-setup=ask-first, wealth=unspecified(default:full)
- **Channel**: each project has its own notification channel
- **Notifications**: zane-setup has bead-start=off; others default to all-on
- **Guardrails**: each project has unique constraints (e.g., wealth confidentiality rules)

### ✅ Session Label Namespacing
Worker sessions use `project:<slug>:<bead-id>` labels:
- `project:projects-skill:projects-skill-qc0`
- `project:zane-setup:zane-setup-l3w`
- `project:wealth:wealth-xxx`

This ensures the orchestrator counts workers per-project independently, so MaxWorkers caps don't interfere across projects.

### ✅ Concurrency Independence
MaxWorkers is checked per-project. One project reaching its cap doesn't block others. The orchestrator processes each project in sequence and spawns workers up to each project's individual limit.

### ✅ Zombie Detection Isolation
Zombie detection uses the session label prefix `project:<slug>` to scope detection per-project. A zombie in one project doesn't affect another project's worker count.

## Potential Issues Noted

1. **No concurrent active iteration support within a project** — the orchestrator finds "one with Status: active" per project. If multiple iterations are active, behavior is undefined. Current projects don't hit this, but the orchestrator.md could clarify (pick the lowest-numbered active iteration).

2. **Wealth PROJECT.md format divergence** — uses a non-standard format (no bold markers, has custom fields like `Bead Cap`, `Iteration Rules`). This works due to format tolerance, but is an example of organic drift that migration could normalize.

## Conclusion

Multi-project support works correctly. The architecture cleanly separates concerns through per-project config, session label namespacing, and independent concurrency tracking. No code changes needed.
