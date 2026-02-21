# Clean up global braids directory (projects-skill-8mb)

## What was done

Removed all bd workspace files from `~/.openclaw/braids/`, keeping only `registry.edn` (braids state).

### Files removed
- `.jsonl.lock`, `.local_version`
- `.orchestrator-state.json`
- `beads.db`, `beads.db-shm`, `beads.db-wal`
- `daemon.lock`, `daemon.log`, `daemon.pid`
- `issues.jsonl`, `last-touched`, `metadata.json`, `sync-state.json`

### Files kept
- `registry.edn` — project registry (braids state)

### Directory
Kept as `braids` (no dot-prefix) at `~/.openclaw/braids/`.

## Notes
- No code changes required — this was a file cleanup task
- The directory name was already `braids` (not `.braids`), so no rename needed
