# Projects Skill

Status: active
Priority: high
Budget: unlimited
Autonomy: full
Checkin: daily
Channel: 1471680593497554998
MaxWorkers: 1

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

Build and refine the "projects" OpenClaw skill — an autonomous project management system that enables OpenClaw agents to work on multiple long-running projects in the background with iterative delivery, parallel sub-agent execution, and structured check-ins.

## Guardrails

- Changes to the skill definition go in `~/projects/projects-skill/SKILL.md` (symlinked from `~/.openclaw/skills/projects/`)
- Test workflow changes by using this project as the guinea pig
- Commit frequently with meaningful messages
- Ask before making changes that affect other skills or OpenClaw config
