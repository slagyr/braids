# Data format migration: registry.edn and project.edn schemas (braids-qle)

## Summary

Defined EDN schemas for `registry.edn` and `project.edn`, implemented parsers with validation, and built a markdown-to-EDN migration layer. The CLI can now read structured EDN data from day one, with a migration path from the existing markdown formats.

## What was built

### `src/braids/registry.clj`
- `parse-registry` — Reads registry.edn string into Clojure data
- `validate-registry` — Validates statuses (active/paused/blocked), priorities (high/normal/low), slug presence, and slug uniqueness
- `registry->edn-string` — Serializes registry back to EDN

### `src/braids/project_config.clj`
- `parse-project-config` — Reads project.edn with defaults applied (max-workers=1, worker-timeout=3600, checkin=on-demand, all notifications=true)
- `validate-project-config` — Validates status, priority, autonomy, max-workers positivity, name presence
- `project-config->edn-string` — Serializes config back to EDN

### `src/braids/migration.clj`
- `parse-registry-md` — Parses markdown registry table into the EDN registry format
- `parse-project-md` — Extracts config fields from PROJECT.md markdown format, including notifications table with mention directives
- Both produce the same data structures as the EDN parsers, enabling seamless migration

## EDN Schemas

### registry.edn
```clojure
{:projects [{:slug "my-project"
             :status :active        ; :active | :paused | :blocked
             :priority :normal      ; :high | :normal | :low
             :path "~/Projects/my-project"}]}
```

### project.edn
```clojure
{:name "My Project"
 :status :active
 :priority :normal
 :autonomy :full              ; :full | :ask-first | :research-only
 :checkin :on-demand           ; :daily | :weekly | :on-demand
 :channel "1234567890"
 :max-workers 1
 :worker-timeout 3600
 :notifications {:iteration-start true
                 :bead-start true
                 :bead-complete true
                 :iteration-complete true
                 :no-ready-beads true
                 :question true
                 :blocker true}
 :notification-mentions {:blocker "<@user-id>"}}  ; optional
```

## Test Coverage

- 7 registry specs (parse, validate, round-trip)
- 9 project-config specs (parse with defaults, validate, notifications, round-trip)
- 5 migration specs (registry md parsing, project md parsing, notifications table extraction)

All 21 new specs passing. TDD throughout: RED → GREEN → commit.
