Feature: Ready beads

  The orchestrator prints important information about the projects and beads used to determine actions taken.

  @wip
  Scenario: ready, in-progress, and blocked beads are printed
    Given configured projects:
      | slug  | status   | priority | max-workers |
      | alpha | active   | normal   | 1           |
      | beta  | active   | normal   | 1           |
    And project "alpha" has beads:
      | id        | title   | status |
      | alpha-aa1 | Task A1 | ready  |
      | alpha-aa2 | Task A2 | closed  |
    And project "beta" has beads:
      | id       | title    | status |
      | beta-bb1 | Task B1  | ready  |
      | beta-bb2 | Task B2  | in-progress  |
      | beta-bb3 | Task B3  | blocked  |
      | beta-bb4 | Task B4  | closed  |
    When the orchstrator ticks
    Then the output contains lines
      | line                          |
      | alpha <more information here> |
      |   <emoji> alpha-aa1 Task A1 something like this |
      | beta project header   |
      |   <emoji> beta-bb1 line |
      |   <emoji> beta-bb2 line |
      |   <emoji> beta-bb3 line |
    And the output should not contain lines matching regex
      |   alpha-aa2  |
      |   beta-bb4   |
