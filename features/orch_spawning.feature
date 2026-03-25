Feature: Orchestrator spawning behavior

  The orchestrator tick examines project state and decides whether
  to spawn workers or remain idle. It respects max-workers capacity,
  requires active iterations, and reports idle reasons. When spawning,
  it produces a list of spawn entries with all attributes needed to
  invoke the worker agents.

  Background:
    Given the harness is reset
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers | path            |
      | alpha | active | normal   | 2           | 003              | 0              | /projects/alpha |

  Scenario: Spawn includes all invocation attributes
    Given configured projects:
      | slug  | worker-timeout | worker-agent | worker-model | worker-thinking | channel |
      | alpha | 7200           | agent-abc    | opus         | high            | #alpha  |
    And project "alpha" has beads:
      | id        | title          | status |
      | alpha-aa1 | Implement auth | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      | iteration | channel | path            | label                  | worker-timeout | worker-thinking | worker-agent | worker-model |
      | alpha   | alpha-aa1 | 003       | #alpha  | /projects/alpha | project:alpha:alpha-aa1 | 7200           | high            | agent-abc    | opus         |

  Scenario: Spawn uses default config values when not specified
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      | iteration | channel | path            | label                  | worker-timeout | worker-thinking |
      | alpha   | alpha-aa1 | 003       |         | /projects/alpha | project:alpha:alpha-aa1 | 1800           | high            |

  Scenario: Spawn multiple workers up to capacity
    Given configured projects:
      | slug  | max-workers | active-iteration | active-workers |
      | alpha | 3           | 005              | 1              |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
      | alpha-aa2 | Task 2 | ready  |
      | alpha-aa3 | Task 3 | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      | label                  |
      | alpha   | alpha-aa1 | project:alpha:alpha-aa1 |
      | alpha   | alpha-aa2 | project:alpha:alpha-aa2 |
    And the spawns should not include bead "alpha-aa3"

  Scenario: Spawn across multiple projects respects per-project capacity
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers | path           |
      | alpha | active | high     | 1           | 003              | 0              | /projects/alpha |
      | beta  | active | normal   | 2           | 001              | 0              | /projects/beta |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    And project "beta" has beads:
      | id       | title  | status |
      | beta-bb1 | Task 1 | ready  |
      | beta-bb2 | Task 2 | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      | path            |
      | alpha   | alpha-aa1 | /projects/alpha |
      | beta    | beta-bb1  | /projects/beta  |
      | beta    | beta-bb2  | /projects/beta  |

  Scenario: Idle when no ready beads
    When the orchestrator ticks
    Then the action should be "idle"
    And the idle reason should be "no-ready-beads"

  Scenario: Idle when at capacity
    Given configured projects:
      | slug  | active-workers |
      | alpha | 2              |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the action should be "idle"
    And the idle reason should be "all-at-capacity"

  Scenario: Idle when no active iterations
    Given configured projects:
      | slug  | active-iteration |
      | alpha |                  |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the action should be "idle"
    And the idle reason should be "no-active-iterations"

  Scenario: Paused projects are not spawned
    Given configured projects:
      | slug | status | max-workers | active-iteration | active-workers | path           |
      | beta | paused | 1           | 001              | 0              | /projects/beta |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    And project "beta" has beads:
      | id       | title  | status |
      | beta-bb1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      |
      | alpha   | alpha-aa1 |
    And the spawns should not include bead "beta-bb1"

  Scenario: Only ready beads are spawned
    And project "alpha" has beads:
      | id        | title  | status      |
      | alpha-aa1 | Task 1 | ready       |
      | alpha-aa2 | Task 2 | in-progress |
      | alpha-aa3 | Task 3 | blocked     |
      | alpha-aa4 | Task 4 | closed      |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      |
      | alpha   | alpha-aa1 |
    And the spawns should not include bead "alpha-aa2"
    And the spawns should not include bead "alpha-aa3"
    And the spawns should not include bead "alpha-aa4"

  Scenario: Spawn omits worker-agent and worker-model when not configured
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And the spawns should include
      | project | bead      |
      | alpha   | alpha-aa1 |
    And the spawn for "alpha-aa1" should not have key "worker-agent"
    And the spawn for "alpha-aa1" should not have key "worker-model"

  Scenario: High priority projects spawn before lower priority
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers | path           |
      | low   | active | low      | 1           | 001              | 0              | /projects/low  |
      | high  | active | high     | 1           | 001              | 0              | /projects/high |
      | alpha | active | normal   | 1           | 003              | 0              | /projects/alpha |
    And project "high" has beads:
      | id      | title  | status |
      | high-h1 | Task 1 | ready  |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    And project "low" has beads:
      | id    | title  | status |
      | low-l1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the action should be "spawn"
    And spawn 1 should be for project "high"
    And spawn 2 should be for project "alpha"
    And spawn 3 should be for project "low"
