Feature: Orchestrator tick output

  The orchestrator prints a summary of each active project and its
  open beads when it ticks. Ready, in-progress, and blocked beads
  are shown with status icons. Closed beads are excluded.

  @wip
  Scenario: ready, in-progress, and blocked beads are printed
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 2           | 005              | 0              |
      | beta  | active | normal   | 1           | 003              | 0              |
    And project "alpha" has beads:
      | id        | title   | status |
      | alpha-aa1 | Task A1 | ready  |
      | alpha-aa2 | Task A2 | closed |
    And project "beta" has beads:
      | id       | title   | status      |
      | beta-bb1 | Task B1 | ready       |
      | beta-bb2 | Task B2 | in-progress |
      | beta-bb3 | Task B3 | blocked     |
      | beta-bb4 | Task B4 | closed      |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                                     |
      | alpha  active  iteration 005  workers:0/2  beads:              |
      |     ○ aa1 Task A1              ready |
      | beta  active  iteration 003  workers:0/1  beads: |
      |     ○ bb1 Task B1              ready |
      |     ● bb2 Task B2              in-progress |
      |     ✗ bb3 Task B3              blocked |

    And the output does not contain
      | text |
      | aa2  |
      | bb4  |

  @wip
  Scenario: long bead titles are truncated to 20 characters
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    And project "alpha" has beads:
      | id        | title                              | status |
      | alpha-xx1 | Short                              | ready  |
      | alpha-xx2 | Exactly Twenty Chars               | ready  |
      | alpha-xx3 | This Title Is Way Too Long For Col | ready  |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                                     |
      |     ○ xx1 Short                ready |
      |     ○ xx2 Exactly Twenty Chars ready |
      |     ○ xx3 This Title Is Way... ready |

  @wip
  Scenario: project with no active iteration
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | gamma | active | normal   | 1           |                  | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                          |
      | gamma  active  (no iteration)  workers:0/1 |

  @wip
  Scenario: project with active workers shows worker count
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 3           | 002              | 2              |
    And project "alpha" has beads:
      | id        | title   | status |
      | alpha-aa1 | Task A1 | ready  |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                          |
      | alpha  active  iteration 002  workers:2/3  beads: |
      |     ○ aa1 Task A1              ready |

  @wip
  Scenario: project with no open beads shows none
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                                  |
      | alpha  active  iteration 001  workers:0/1  beads: (none) |

  @wip
  Scenario: idle tick appends decision line
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                  |
      | → idle: no-ready-beads |

  @wip
  Scenario: projects are ordered by priority
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | low   | active | low      | 1           | 001              | 0              |
      | high  | active | high     | 1           | 001              | 0              |
      | norm  | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output has "high" before "norm"
    And the output has "norm" before "low"

  @wip
  Scenario: paused projects are excluded from output
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
      | beta  | paused | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression |
      | alpha      |
    And the output does not contain
      | text |
      | beta |
