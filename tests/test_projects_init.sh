#!/usr/bin/env bash
# Tests for projects-init script
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INIT_SCRIPT="$SCRIPT_DIR/../projects/bin/projects-init"
TEST_HOME="$(mktemp -d)"
TEST_PROJECTS="$TEST_HOME/Projects"

PASS=0
FAIL=0

pass() { ((PASS++)); echo "  ✓ $1"; }
fail() { ((FAIL++)); echo "  ✗ $1"; }
check() {
  local desc="$1"; shift
  if "$@" >/dev/null 2>&1; then pass "$desc"; else fail "$desc"; fi
}

cleanup() { rm -rf "$TEST_HOME"; }
trap cleanup EXIT

# ─── Test: Basic project creation ────────────────────────────────────

echo "▸ Basic project creation"
output=$("$INIT_SCRIPT" test-project --projects-home "$TEST_PROJECTS" 2>&1)
check "Exit code 0" true
check "Project directory created" test -d "$TEST_PROJECTS/test-project"
check "Git repo initialized" test -d "$TEST_PROJECTS/test-project/.git"
check "bd initialized" test -d "$TEST_PROJECTS/test-project/.beads"
check "AGENTS.md created" test -f "$TEST_PROJECTS/test-project/AGENTS.md"
check "PROJECT.md created" test -f "$TEST_PROJECTS/test-project/PROJECT.md"
check "iterations/001 created" test -d "$TEST_PROJECTS/test-project/iterations/001"
check "ITERATION.md created" test -f "$TEST_PROJECTS/test-project/iterations/001/ITERATION.md"
check "registry.md created" test -f "$TEST_PROJECTS/registry.md"
check "Registry has entry" grep -q "| test-project |" "$TEST_PROJECTS/registry.md"

# ─── Test: PROJECT.md contents ───────────────────────────────────────

echo "▸ PROJECT.md contents"
PM="$TEST_PROJECTS/test-project/PROJECT.md"
check "Has Status field" grep -q 'Status:' "$PM"
check "Has Priority field" grep -q 'Priority:' "$PM"
check "Has Autonomy field" grep -q 'Autonomy:' "$PM"
check "Has Channel field" grep -q 'Channel:' "$PM"
check "Has MaxWorkers field" grep -q 'MaxWorkers:' "$PM"
check "Has Goal section" grep -q '## Goal' "$PM"
check "Has Guardrails section" grep -q '## Guardrails' "$PM"
check "Has Notifications table" grep -q '| Event | Notify |' "$PM"

# ─── Test: AGENTS.md contents ────────────────────────────────────────

echo "▸ AGENTS.md contents"
AM="$TEST_PROJECTS/test-project/AGENTS.md"
check "References worker.md" grep -q 'worker.md' "$AM"
check "References PROJECT.md" grep -q 'PROJECT.md' "$AM"
check "Has Quick Reference" grep -q 'Quick Reference' "$AM"

# ─── Test: ITERATION.md contents ─────────────────────────────────────

echo "▸ ITERATION.md contents"
IM="$TEST_PROJECTS/test-project/iterations/001/ITERATION.md"
check "Has planning status" grep -q 'planning' "$IM"
check "Has Stories section" grep -q '## Stories' "$IM"

# ─── Test: Registry format ───────────────────────────────────────────

echo "▸ Registry format"
check "Has table header" grep -q '| Slug | Status | Priority | Path |' "$TEST_PROJECTS/registry.md"
check "Entry has active status" grep -q "| test-project | active | normal |" "$TEST_PROJECTS/registry.md"

# ─── Test: Git commit ────────────────────────────────────────────────

echo "▸ Git commit"
commit_msg="$(cd "$TEST_PROJECTS/test-project" && git log --oneline -1)"
if echo "$commit_msg" | grep -q "Initialize project"; then
  pass "Has initial commit"
else
  fail "Has initial commit (got: $commit_msg)"
fi

# ─── Test: Duplicate prevention ──────────────────────────────────────

echo "▸ Duplicate prevention"
if "$INIT_SCRIPT" test-project --projects-home "$TEST_PROJECTS" 2>&1; then
  fail "Should reject duplicate slug"
else
  pass "Rejects duplicate slug"
fi

# ─── Test: Second project appends to registry ────────────────────────

echo "▸ Second project"
"$INIT_SCRIPT" another-project --projects-home "$TEST_PROJECTS" >/dev/null 2>&1
check "Second project directory exists" test -d "$TEST_PROJECTS/another-project"
check "Registry has both entries" test "$(grep -c '| .* | active |' "$TEST_PROJECTS/registry.md")" -eq 2

# ─── Test: Invalid slug ─────────────────────────────────────────────

echo "▸ Invalid slug"
if "$INIT_SCRIPT" "BAD SLUG" --projects-home "$TEST_PROJECTS" 2>&1; then
  fail "Should reject invalid slug"
else
  pass "Rejects invalid slug"
fi

# ─── Test: No arguments ─────────────────────────────────────────────

echo "▸ No arguments"
if "$INIT_SCRIPT" 2>&1; then
  fail "Should fail with no arguments"
else
  pass "Fails with no arguments"
fi

# ─── Summary ─────────────────────────────────────────────────────────

echo ""
echo "==============================="
echo "projects-init Tests: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
