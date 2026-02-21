# Pretty-print EDN files (projects-skill-965)

## Summary

Added `braids.edn-format` namespace with an `edn-format` function that pretty-prints EDN data structures with proper indentation using `clojure.pprint`. Updated all EDN serialization points to use it instead of `pr-str`.

## Changes

### New files
- `src/braids/edn_format.clj` — `edn-format` function using `clojure.pprint/pprint` with 80-char right margin
- `spec/braids/edn_format_spec.clj` — 7 specs covering formatting, round-tripping, vectors, nested maps, empty maps, trailing newline

### Updated serializers (pr-str → edn-format)
- `src/braids/registry.clj` — `registry->edn-string`
- `src/braids/project_config.clj` — `project-config->edn-string`
- `src/braids/config.clj` — `serialize-config`
- `src/braids/iteration.clj` — `iteration->edn-string`
- `src/braids/new_io.clj` — inline config.edn, iteration.edn, and registry writes
- `src/braids/init_io.clj` — registry creation

### Result
All .edn files written by braids will now be pretty-printed with proper indentation. Existing files are reformatted on next write. No `pr-str` calls remain in `src/`.

## Tests
- 7 new specs, all passing
- Full suite: 394 examples, 13 failures (all pre-existing), 735 assertions
