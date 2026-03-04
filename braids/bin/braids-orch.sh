#!/usr/bin/env bash
# braids-orch.sh — Shell-based orchestrator for braids.
# Replaces the LLM agent orchestrator with a simple script that:
#   1. Runs `braids orch-tick` to get spawn decisions
#   2. For each spawn, fires `openclaw agent` in the background
#   3. Handles idle/disable-cron
#   4. Exits immediately
#
# Usage: braids-orch.sh [--dry-run] [--verbose]
# Environment: BRAIDS_ORCH_LOG (default: /tmp/braids-orch.log)

set -euo pipefail

DRY_RUN=false
VERBOSE=false
LOG_FILE="${BRAIDS_ORCH_LOG:-/tmp/braids-orch.log}"

for arg in "$@"; do
  case "$arg" in
    --dry-run)  DRY_RUN=true ;;
    --verbose)  VERBOSE=true ;;
    *)          echo "Unknown arg: $arg" >&2; exit 1 ;;
  esac
done

log() {
  local ts
  ts=$(date '+%Y-%m-%dT%H:%M:%S')
  echo "[$ts] $*" >> "$LOG_FILE"
  if $VERBOSE; then echo "[$ts] $*" >&2; fi
}

# 1. Run orch-tick and capture JSON (stdout) and debug (stderr)
TICK_JSON=$(braids orch-tick 2>/dev/null)
if [ -z "$TICK_JSON" ]; then
  log "ERROR: braids orch-tick returned empty output"
  exit 1
fi

ACTION=$(echo "$TICK_JSON" | jq -r '.action')
log "orch-tick action=$ACTION"

# 2. Handle zombies (if any)
ZOMBIE_COUNT=$(echo "$TICK_JSON" | jq -r '.zombies // [] | length')
if [ "$ZOMBIE_COUNT" -gt 0 ]; then
  log "Found $ZOMBIE_COUNT zombie(s) — logging only (no CLI kill available)"
  echo "$TICK_JSON" | jq -r '.zombies[] | "  zombie: \(.bead) reason=\(.reason) label=\(.label)"' | while read -r line; do
    log "$line"
  done
fi

# 3. Handle spawns
if [ "$ACTION" = "spawn" ]; then
  SPAWN_COUNT=$(echo "$TICK_JSON" | jq -r '.spawns | length')
  log "Spawning $SPAWN_COUNT worker(s)"

  echo "$TICK_JSON" | jq -c '.spawns[]' | while read -r spawn; do
    BEAD=$(echo "$spawn" | jq -r '.bead')
    PROJECT_PATH=$(echo "$spawn" | jq -r '.path')
    ITERATION=$(echo "$spawn" | jq -r '.iteration')
    CHANNEL=$(echo "$spawn" | jq -r '.channel')
    TIMEOUT=$(echo "$spawn" | jq -r '.runTimeoutSeconds')
    AGENT_ID=$(echo "$spawn" | jq -r '.agentId // empty')
    THINKING=$(echo "$spawn" | jq -r '.thinking // "low"')
    SESSION_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')

    TASK="You are a project worker for the braids skill. Read and follow ~/.openclaw/skills/braids/references/worker.md

Project: $PROJECT_PATH
Bead: $BEAD
Iteration: $ITERATION
Channel: $CHANNEL"

    log "Spawning worker: bead=$BEAD agent=${AGENT_ID:-default} session=$SESSION_ID"

    if $DRY_RUN; then
      log "DRY-RUN: would spawn openclaw agent for $BEAD"
    else
      AGENT_ARGS=()
      if [ -n "$AGENT_ID" ]; then
        AGENT_ARGS+=(--agent "$AGENT_ID")
      fi
      AGENT_ARGS+=(--message "$TASK")
      AGENT_ARGS+=(--session-id "$SESSION_ID")
      AGENT_ARGS+=(--thinking "$THINKING")
      AGENT_ARGS+=(--timeout "$TIMEOUT")
      AGENT_ARGS+=(--deliver)
      AGENT_ARGS+=(--reply-channel discord)
      AGENT_ARGS+=(--reply-to "$CHANNEL")

      # Fire and forget
      openclaw agent "${AGENT_ARGS[@]}" >> "$LOG_FILE" 2>&1 &
    fi
  done

  log "All workers spawned"
fi

# 4. Handle idle
if [ "$ACTION" = "idle" ]; then
  REASON=$(echo "$TICK_JSON" | jq -r '.reason')
  DISABLE_CRON=$(echo "$TICK_JSON" | jq -r '.disable_cron')
  log "Idle: reason=$REASON disable_cron=$DISABLE_CRON"

  if [ "$DISABLE_CRON" = "true" ]; then
    # Find the braids-orchestrator cron job and disable it
    CRON_ID=$(openclaw cron list --json 2>/dev/null | jq -r '.jobs[] | select(.name == "braids-orchestrator") | .id')
    if [ -n "$CRON_ID" ]; then
      log "Disabling cron job $CRON_ID"
      if ! $DRY_RUN; then
        openclaw cron disable "$CRON_ID" >> "$LOG_FILE" 2>&1 || log "WARN: failed to disable cron"
      else
        log "DRY-RUN: would disable cron $CRON_ID"
      fi
    else
      log "WARN: braids-orchestrator cron job not found"
    fi
  fi
fi

log "Orchestrator tick complete"
