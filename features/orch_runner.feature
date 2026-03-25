Feature: Orchestrator runner

  The orchestrator runner converts tick results into openclaw agent
  spawn commands. It builds worker task messages from a template,
  constructs CLI arguments with session IDs and agent configuration,
  parses CLI flags, and formats log output for spawn, idle, and
  zombie events.

  Scenario: Build worker task message from template
    Given a spawn entry with path "~/Projects/test" and bead "test-abc"
    And iteration "001" and channel "12345"
    When building the worker task
    Then the task should contain "~/Projects/test"
    And the task should contain "test-abc"
    And the task should contain "001"
    And the task should contain "worker.md"

  Scenario: Build worker CLI args with sessions spawn
    Given a spawn entry with bead "proj-abc"
    And no custom worker agent
    When building the worker args
    Then the args should include "--task"
    And the args should include "--label"
    And the label should be "project:proj:proj-abc"
    And the args should include "--thinking"
    And the args should include "--timeout"
    And the args should not include "--agent"

  Scenario: Build args with custom agent
    Given a spawn entry with bead "proj-abc"
    And worker agent "scrapper"
    When building the worker args
    Then the args should include "--agent"
    And the agent value should be "scrapper"

  Scenario: Parse CLI args defaults to dry-run
    Given no CLI arguments
    When parsing CLI args
    Then dry-run should be true
    And verbose should be false

  Scenario: Parse --confirmed enables run
    Given CLI arguments "--confirmed"
    When parsing CLI args
    Then dry-run should be false

  Scenario: Parse unknown arg returns error
    Given CLI arguments "--bogus"
    When parsing CLI args
    Then parsing should return an error
    And the error should contain "--bogus"

  Scenario: Format spawn log
    Given a spawn tick result with 2 workers
    And beads "b1" and "b2"
    When formatting the spawn log
    Then the log should contain "2 worker"
    And the log should contain "b1"
    And the log should contain "b2"

  Scenario: spawn log shows multiple worker commands
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers | path            | worker-agent | worker-timeout | channel |
      | alpha | active | normal   | 2           | 001              | 0              | /projects/alpha | scrapper     | 1800           | #alpha  |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
      | alpha-aa2 | Task 2 | ready  |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression           |
      | Spawning 2 worker(s) |
    And the output contains a line matching
      | expression |
      | aa1 .+ --thinking high --timeout 1800 --label project:alpha:alpha-aa1 --agent scrapper |
      | aa2 .+ --thinking high --timeout 1800 --label project:alpha:alpha-aa2 --agent scrapper |



  Scenario: Format zombie log
    Given 2 zombie sessions with reasons "bead-closed" and "timeout"
    When formatting the zombie log
    Then the log should contain "2 zombie"
    And the log should contain "bead-closed"
    And the log should contain "timeout"

