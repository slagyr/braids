#!/usr/bin/env bash
set -euo pipefail

REPO="https://github.com/slagyr/braids.git"
SKILL_DIR="$HOME/.openclaw/skills/braids"
INSTALL_DIR="${BRAIDS_INSTALL_DIR:-$HOME/.openclaw/braids-skill}"

echo "Installing braids skill..."

# Clone or update
if [ -d "$INSTALL_DIR" ]; then
  echo "Updating existing installation..."
  git -C "$INSTALL_DIR" pull --ff-only
else
  git clone "$REPO" "$INSTALL_DIR"
fi

# Symlink
mkdir -p "$(dirname "$SKILL_DIR")"
if [ -L "$SKILL_DIR" ] || [ -e "$SKILL_DIR" ]; then
  echo "Removing existing symlink/directory at $SKILL_DIR"
  rm -rf "$SKILL_DIR"
fi
ln -s "$INSTALL_DIR/braids" "$SKILL_DIR"

echo "✅ Braids skill installed. Symlinked $SKILL_DIR → $INSTALL_DIR/braids"
echo ""
echo "Next: Ask your agent to 'set up braids' to complete first-time setup."
