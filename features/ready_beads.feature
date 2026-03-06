Feature: Ready beads

  The ready command finds unblocked beads eligible for work. It filters
  to active projects, respects worker capacity, orders results by
  project priority, and formats output as a numbered list with colors.

  Scenario: Ready beads filters to active projects only
    Given a registry with projects:
      | slug  | status   | priority |
      | alpha | active   | normal   |
      | beta  | paused   | normal   |
    And project "alpha" has config with max-workers 1
    And project "beta" has config with max-workers 1
    And project "alpha" has ready beads:
      | id        | title   | priority |
      | alpha-aaa | Task A  | P1       |
    And project "beta" has ready beads:
      | id       | title   | priority |
      | beta-bbb | Task B  | P1       |
    And no active workers
    When computing ready beads
    Then the result should contain bead "alpha-aaa"
    And the result should not contain bead "beta-bbb"

  Scenario: Ready beads respects worker capacity
    Given a registry with projects:
      | slug | status | priority |
      | proj | active | normal   |
    And project "proj" has config with max-workers 1
    And project "proj" has ready beads:
      | id       | title   | priority |
      | proj-abc | Task A  | P1       |
    And project "proj" has 1 active worker
    When computing ready beads
    Then the result should be empty

  Scenario: Ready beads returns beads when under capacity
    Given a registry with projects:
      | slug | status | priority |
      | proj | active | normal   |
    And project "proj" has config with max-workers 3
    And project "proj" has ready beads:
      | id       | title   | priority |
      | proj-abc | Task A  | P0       |
      | proj-def | Task B  | P1       |
    And project "proj" has 2 active workers
    When computing ready beads
    Then the result should contain bead "proj-abc"
    And the result should contain bead "proj-def"

  Scenario: Ready beads orders by project priority
    Given a registry with projects:
      | slug | status | priority |
      | low  | active | low      |
      | high | active | high     |
      | norm | active | normal   |
    And project "low" has config with max-workers 1
    And project "high" has config with max-workers 1
    And project "norm" has config with max-workers 1
    And project "low" has ready beads:
      | id      | title    | priority |
      | low-aaa | Low task | P2       |
    And project "high" has ready beads:
      | id       | title     | priority |
      | high-bbb | High task | P0       |
    And project "norm" has ready beads:
      | id       | title     | priority |
      | norm-ccc | Norm task | P1       |
    And no active workers
    When computing ready beads
    Then the first result should be from project "high"
    And the second result should be from project "norm"
    And the third result should be from project "low"

  Scenario: Ready beads skips project paused in config
    Given a registry with projects:
      | slug | status | priority |
      | proj | active | normal   |
    And project "proj" has config with status "paused" and max-workers 1
    And project "proj" has ready beads:
      | id       | title   | priority |
      | proj-abc | Task A  | P1       |
    And no active workers
    When computing ready beads
    Then the result should be empty

  Scenario: Format ready output shows numbered list
    Given ready beads to format:
      | project | id       | title    | priority |
      | proj    | proj-abc | Do stuff | P0       |
    When formatting ready output
    Then the output should contain "proj-abc"
    And the output should contain "Do stuff"
    And the output should contain "proj"

  Scenario: Format ready output for empty beads
    Given no ready beads to format
    When formatting ready output
    Then the output should be "No ready beads."
