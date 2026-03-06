Feature: Orchestrator spawning behavior

  The orchestrator tick examines project state and decides whether
  to spawn workers or remain idle. It respects max-workers capacity,
  requires active iterations, and reports idle reasons.

  Background:
    Given a project "alpha" with max-workers 2
    And project "alpha" has an active iteration "003"

  Scenario: Spawn workers when beads ready and capacity available
    Given project "alpha" has 3 ready beads
    And project "alpha" has 0 active workers
    When the orchestrator ticks
    Then the action should be "spawn"
    And 2 workers should be spawned

  Scenario: Spawn fewer workers when fewer beads than capacity
    Given project "alpha" has 1 ready bead
    And project "alpha" has 0 active workers
    When the orchestrator ticks
    Then the action should be "spawn"
    And 1 worker should be spawned

  Scenario: Idle when no ready beads
    Given project "alpha" has 0 ready beads
    And project "alpha" has 0 active workers
    When the orchestrator ticks
    Then the action should be "idle"
    And the idle reason should be "no-ready-beads"

  Scenario: Idle when at capacity
    Given project "alpha" has 3 ready beads
    And project "alpha" has 2 active workers
    When the orchestrator ticks
    Then the action should be "idle"
    And the idle reason should be "all-at-capacity"

  Scenario: Idle when no active iterations
    Given a project "beta" with max-workers 1
    And project "beta" has no active iteration
    And project "beta" has 3 ready beads
    And project "beta" has 0 active workers
    When the orchestrator ticks for project "beta" only
    Then the action should be "idle"
    And the idle reason should be "no-active-iterations"

  Scenario: Spawn respects per-project capacity independently
    Given a project "beta" with max-workers 1
    And project "beta" has an active iteration "001"
    And project "alpha" has 2 ready beads
    And project "alpha" has 0 active workers
    And project "beta" has 1 ready bead
    And project "beta" has 0 active workers
    When the orchestrator ticks
    Then the action should be "spawn"
    And 3 workers should be spawned

  Scenario: Spawn includes correct label format
    Given project "alpha" has 1 ready bead with id "alpha-abc"
    And project "alpha" has 0 active workers
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawn label should be "project:alpha:alpha-abc"
