#!/usr/bin/env bash
# Tests for CONTRACTS.md — validate that the contracts document exists,
# covers all required sections, and is consistent with SKILL.md/worker.md/orchestrator.md.

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONTRACTS="$PROJECT_ROOT/CONTRACTS.md"
SKILL="$HOME/.openclaw/skills/projects/SKILL.md"
WORKER="$HOME/.openclaw/skills/projects/references/worker.md"
ORCHESTRATOR="$HOME/.openclaw/skills/projects/references/orchestrator.md"

PASS=0
FAIL=0

check() {
  local desc="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "  ✓ $desc"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $desc"
    FAIL=$((FAIL + 1))
  fi
}

# Disable errexit so grep failures don't kill the script
set +e

echo "=== CONTRACTS.md Tests ==="

# --- Existence ---
check "CONTRACTS.md exists" test -f "$CONTRACTS"

# --- Required sections ---
check "Section: File Format Contracts" grep -q "## 1\. File Format Contracts" "$CONTRACTS"
check "Section: Orchestrator Invariants" grep -q "## 2\. Orchestrator Invariants" "$CONTRACTS"
check "Section: Worker Invariants" grep -q "## 3\. Worker Invariants" "$CONTRACTS"
check "Section: Cross-Cutting Invariants" grep -q "## 4\. Cross-Cutting Invariants" "$CONTRACTS"

# --- File format subsections ---
for fmt in "registry.md" "PROJECT.md" "ITERATION.md" "RETRO.md" "STATUS.md" ".orchestrator-state.json"; do
  check "Documents format: $fmt" grep -q "$fmt" "$CONTRACTS"
done
check "Documents deliverable format" grep -q "Deliverable" "$CONTRACTS"

# --- Key defaults documented (must match SKILL.md) ---
check "Default MaxWorkers=1" grep -q "MaxWorkers.*1" "$CONTRACTS"
check "Default WorkerTimeout=1800" grep -q "WorkerTimeout.*1800" "$CONTRACTS"
check "Default Autonomy=full" grep -q "Autonomy.*full" "$CONTRACTS"
check "Default Priority=normal" grep -q "Priority.*normal" "$CONTRACTS"

# --- Orchestrator invariants ---
check "No direct work invariant" grep -q "never.*performs bead work\|never.*perform bead work\|never.*do.*bead work" "$CONTRACTS"
check "Concurrency enforcement" grep -q "MaxWorkers" "$CONTRACTS"
check "Zombie detection documented" grep -q "[Zz]ombie" "$CONTRACTS"
check "Session label convention" grep -q "project:<slug>:<bead-id>" "$CONTRACTS"
check "Frequency scaling backoff values" grep -q "30min\|30 min" "$CONTRACTS"

# --- Worker invariants ---
check "Claim before work" grep -q "claim.*before\|Claim.*Before" "$CONTRACTS"
check "Dependency verification" grep -q "[Dd]ependenc" "$CONTRACTS"
check "Deliverable required" grep -q "[Dd]eliverable.*[Rr]equired\|produces a deliverable" "$CONTRACTS"
check "Git commit on completion" grep -q "git commit" "$CONTRACTS"
check "Format tolerance" grep -q "[Ff]ormat [Tt]olerance" "$CONTRACTS"
check "Notification discipline" grep -q "[Nn]otification" "$CONTRACTS"

# --- Cross-cutting ---
check "Path convention (~)" grep -q "home directory" "$CONTRACTS"
check "Immutable completed iterations" grep -q "[Ii]mmutable" "$CONTRACTS"
check "Git as transport" grep -q "git push" "$CONTRACTS"

# --- Consistency: all notification events from SKILL.md are in CONTRACTS ---
for event in "iteration-start" "bead-start" "bead-complete" "iteration-complete" "no-ready-beads" "question" "blocker"; do
  check "Notification event '$event' documented" grep -q "$event" "$CONTRACTS"
done

# --- Consistency: valid statuses match SKILL.md ---
check "Registry statuses: active, paused, blocked" grep -q "active.*paused.*blocked" "$CONTRACTS"
check "Iteration statuses: planning, active, complete" grep -q "planning.*active.*complete" "$CONTRACTS"

# --- Summary ---
echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
