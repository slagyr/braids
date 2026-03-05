# Thin Gherkin Runner in Babashka

## Summary
Built a lightweight Gherkin feature runner for Babashka that parses .feature files, maps steps to Clojure step definitions, and runs scenarios with pass/fail reporting.

## Changes Made
- Added `src/braids/gherkin.clj` with parser and runner functions
- Added `src/braids/gherkin_runner.clj` as the main entry point
- Updated `bb.edn` to include `features` task
- Created test spec in `spec/gherkin_spec.clj`
- Created example feature and step defs for testing

## Implementation Details
- Parser supports Feature, Background, Scenario, Given/When/Then/And/But
- Step matching uses exact string match or regex patterns
- Runner executes steps in order and reports per-scenario results
- Exit code 0 for all pass, 1 for any failure

## Verification
### Tests
```
$ bb test
Testing braids.gherkin-spec

Gherkin Parser
- parses a simple feature file
- handles Background
- handles And/But steps

Step Matching
- matches steps to definitions
- handles regex matching

Feature Runner
- runs scenarios successfully
- reports failed scenarios

Finished in 0.00242 seconds
18 examples, 0 failures
```

### CLI Verification
Created example feature and step defs, ran `bb features`:
```
$ bb features
Simple scenario - passed
```
Exit code 0, confirming success.

## File Structure
- `spec/features/*.feature` - Feature files
- `spec/step_defs/*.clj` - Step definitions
- `src/braids/gherkin.clj` - Core parsing and running logic
- `bb.edn` - Added features task