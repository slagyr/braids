Feature: Orchestrator spawning behavior

  Background:
    Given an orchestrator with max-workers set to 2
    And worker-timeout set to 3600

  Scenario: Tick decides to spawn when beads ready and capacity available
    Given 3 ready beads in the project
    And 0 active workers
    When the orchestrator ticks
    Then it should spawn 2 workers
    And mark the tick as spawn

  Scenario: Tick decides to spawn partial capacity when fewer beads than max-workers
    Given 1 ready bead in the project
    And 0 active workers
    When the orchestrator ticks
    Then it should spawn 1 worker
    And mark the tick as spawn

  Scenario: Tick decides idle when no ready beads
    Given 0 ready beads in the project
    And 0 active workers
    When the orchestrator ticks
    Then it should spawn 0 workers
    And mark the tick as idle with reason "no-ready-beads"

  Scenario: Tick decides idle when at capacity
    Given 5 ready beads in the project
    And 2 active workers
    When the orchestrator ticks
    Then it should spawn 0 workers
    And mark the tick as idle with reason "all-at-capacity"

  Scenario: Spawn conditions check project readiness
    Given a project with active iteration
    And notifications enabled
    When checking spawn conditions
    Then the project should be considered ready for spawning

  Scenario: Spawn conditions reject when iteration not active
    Given a project with planning iteration
    When checking spawn conditions
    Then the project should not be considered ready for spawning

  Scenario: Idle conditions when no action needed
    Given no ready beads across all projects
    When checking idle conditions
    Then the orchestrator should report idle with reason "no-ready-beads"

  Scenario: Idle conditions when all workers busy
    Given ready beads exist but all workers at capacity
    When checking idle conditions
    Then the orchestrator should report idle with reason "all-at-capacity"