#!/usr/bin/env bash
# Tests for references/init.md — validate the setup guide exists and is consistent
# with the actual skill structure.

set -euo pipefail

SKILL_DIR="$(cd "$(dirname "$0")/../projects" && pwd)"
INIT_REF="$SKILL_DIR/references/init.md"

PASS=0
FAIL=0

pass() { ((PASS++)); echo "  ✓ $1"; }
fail() { ((FAIL++)); echo "  ✗ $1"; }

echo "▸ Init Reference (references/init.md)"

# File exists
if [ -f "$INIT_REF" ]; then
  pass "init.md exists"
else
  fail "init.md missing at $INIT_REF"
  echo ""
  echo "Results: $PASS passed, $FAIL failed"
  exit 1
fi

# Required sections
for section in "Install the Skill" "Verify beads" "Create PROJECTS_HOME" "Orchestrator Cron" "Verification"; do
  if grep -qi "$section" "$INIT_REF"; then
    pass "Contains section: $section"
  else
    fail "Missing section: $section"
  fi
done

# References projects-init script
if grep -q "projects-init" "$INIT_REF"; then
  pass "References projects-init script"
else
  fail "Should reference projects-init script"
fi

# References orchestrator.md
if grep -q "orchestrator.md" "$INIT_REF"; then
  pass "References orchestrator.md"
else
  fail "Should reference orchestrator.md"
fi

# SKILL.md points to init.md
if grep -q "init.md" "$SKILL_DIR/SKILL.md"; then
  pass "SKILL.md references init.md"
else
  fail "SKILL.md should reference init.md"
fi

# README.md points to init.md
README="$(cd "$(dirname "$0")/.." && pwd)/README.md"
if [ -f "$README" ] && grep -q "init.md" "$README"; then
  pass "README.md references init.md"
else
  fail "README.md should reference init.md"
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
