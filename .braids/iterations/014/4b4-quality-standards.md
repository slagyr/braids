# Update AGENTS.md with Quality Standards (braids-4b4)

## Summary
Added three new sections to AGENTS.md establishing end-to-end verification requirements and quality standards for bead completion.

## Details

Added the following sections after Guardrails:

### Definition of Done
Redefines "done" to require four gates:
1. Unit tests pass (`bb test`)
2. CLI verification with real `bd` commands against a test project
3. Integration check for workflow-affecting changes
4. Documented verification — actual commands and output in deliverables (text, not screenshots)

### Acceptance Criteria Standards
Guidance for writing specific, verifiable acceptance criteria on beads. Includes examples of good criteria (exact expected behavior, edge cases, what not to break) vs. vague ones.

### Safe Testing Practices
Rules to prevent damage to real project data during testing:
- Use test projects in `/tmp` or scratch directories
- Read-only commands against real projects
- Set up/tear down isolated environments for state-modifying tests
