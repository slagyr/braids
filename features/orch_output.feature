Feature: Orchestrator tick output

  The orchestrator prints a mode banner, a summary of each active
  project and its open beads, and log lines for spawns, idles, and
  zombies. Ready, in-progress, and blocked beads are shown with
  status icons. Closed beads are excluded.

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

  Scenario: project with no active iteration
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | gamma | active | normal   | 1           |                  | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                          |
      | gamma  active  (no iteration)  workers:0/1 |

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

  Scenario: project with no open beads shows none
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                                                  |
      | alpha  active  iteration 001  workers:0/1  beads: (none) |

  Scenario: idle tick appends decision line
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression                  |
      | → idle: no-ready-beads |

  Scenario: projects are ordered by priority
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | low   | active | low      | 1           | 001              | 0              |
      | high  | active | high     | 1           | 001              | 0              |
      | norm  | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output has "high" before "norm"
    And the output has "norm" before "low"

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

  @wip
  Scenario: dry-run mode shows DRY-RUN header and footer
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks in dry-run mode
    Then the first line matches "-- DRY-RUN started at"
    And the last line matches "-- DRY-RUN completed at"

  @wip
  Scenario: live-run mode shows LIVE-RUN header and footer
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks in confirmed mode
    Then the first line matches "-- LIVE-RUN started at"
    And the last line matches "-- LIVE-RUN completed at"

  Scenario: spawn log prints full worker command
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers | path            | worker-agent | worker-timeout | channel |
      | alpha | active | normal   | 1           | 001              | 0              | /projects/alpha | scrapper     | 1800           | #alpha  |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
    When the orchestrator ticks
    Then the output contains a line matching
      "spawn cmd: openclaw agent --message .+ --session-id braids-alpha-aa1-worker --thinking high --timeout 1800 --agent scrapper"

  @wip
  Scenario: spawn log shows worker count and bead IDs
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers | path            |
      | alpha | active | normal   | 2           | 001              | 0              | /projects/alpha |
    And project "alpha" has beads:
      | id        | title  | status |
      | alpha-aa1 | Task 1 | ready  |
      | alpha-aa2 | Task 2 | ready  |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression            |
      | Spawning 2 worker(s) |
      | → bead=alpha-aa1     |
      | → bead=alpha-aa2     |

  @wip
  Scenario: idle log shows reason
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression              |
      | Idle: no-ready-beads |

  @wip
  Scenario: zombie log shows zombie count and reasons
    Given configured projects:
      | slug  | status | priority | max-workers | active-iteration | active-workers |
      | alpha | active | normal   | 1           | 001              | 0              |
    And zombie sessions:
      | bead      | reason      |
      | alpha-aa1 | bead-closed |
      | alpha-aa2 | timeout     |
    When the orchestrator ticks
    Then the output contains lines matching
      | expression              |
      | Found 2 zombie(s)      |
      | zombie: alpha-aa1 reason=bead-closed |
      | zombie: alpha-aa2 reason=timeout     |
