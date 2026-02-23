# Add guardrail: channel agent must not modify project files directly — beads only (braids-tw1)

## Summary

Added a guardrail enforcing that the channel/main session agent must not directly edit project files. All file changes must go through beads assigned to workers.

## Changes

### PROJECT.md
- Added guardrail: "Channel agent — beads only" to the Guardrails section

### CONTRACTS.md
- Added §4.7 "Channel Agent — Beads Only" invariant documenting the rule and the channel agent's permitted actions (create beads, plan iterations, activate iterations, review deliverables, answer questions)

### SKILL.md
- Added "Channel Agent Convention" subsection under "Channel for Check-ins" documenting the restriction and rationale

### spec/contracts_spec.clj
- Added "Channel Agent Guardrail" test context with two specs:
  - Verifies CONTRACTS.md contains the channel-agent restriction
  - Verifies SKILL.md documents the channel convention
