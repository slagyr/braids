Feature: Project status

  The status command builds a dashboard of all registered projects
  with enriched iteration, worker, and story data. It supports
  human-readable and JSON output formats, plus detailed per-project views.

  Scenario: Dashboard includes all projects with enriched data
    Given a registry with projects:
      | slug  | status | priority | path              |
      | alpha | active | high     | ~/Projects/alpha  |
      | beta  | paused | normal   | ~/Projects/beta   |
      | gamma | active | low      | ~/Projects/gamma  |
    And project configs:
      | slug  | max-workers |
      | alpha | 2           |
      | beta  | 1           |
      | gamma | 1           |
    And active iterations:
      | slug  | number | total | closed | percent |
      | alpha | 009    | 3     | 1      | 33      |
      | gamma | 002    | 2     | 2      | 100     |
    And active workers:
      | slug  | count |
      | alpha | 1     |
      | gamma | 0     |
    When building the dashboard
    Then the dashboard should have 3 projects
    And project "alpha" should have status "active"
    And project "alpha" should have iteration number "009"
    And project "alpha" should have workers 1 of 2
    And project "beta" should have status "paused"
    And project "beta" should have no iteration

  Scenario: Dashboard handles missing iterations
    Given a registry with projects:
      | slug | status | priority | path             |
      | proj | active | normal   | ~/Projects/proj  |
    And project configs:
      | slug | max-workers |
      | proj | 1           |
    And no active iterations
    And no active workers
    When building the dashboard
    Then project "proj" should have no iteration

  Scenario: Project detail shows iteration progress and stories
    Given a dashboard project "alpha" with:
      | status | active |
      | workers | 1 |
      | max-workers | 2 |
    And project "alpha" has iteration:
      | number | 009 |
      | total  | 3   |
      | closed | 1   |
      | percent | 33 |
    And project "alpha" has stories:
      | id    | title      | status      |
      | a-001 | Do thing   | closed      |
      | a-002 | Do other   | in_progress |
      | a-003 | Do last    | open        |
    When formatting project detail for "alpha"
    Then the output should contain "alpha"
    And the output should contain "1/3"
    And the output should contain "33%"
    And the output should contain "a-001"
    And the output should contain "Do thing"

  Scenario: Project detail shows no-iteration fallback
    Given a dashboard project "beta" with:
      | status | paused |
      | workers | 0 |
      | max-workers | 1 |
    And project "beta" has no iteration
    When formatting project detail for "beta"
    Then the output should contain "beta"
    And the output should contain "no active iteration"

  Scenario: Dashboard JSON output includes all project data
    Given a registry with projects:
      | slug  | status | priority | path              |
      | alpha | active | high     | ~/Projects/alpha  |
    And project configs:
      | slug  | max-workers |
      | alpha | 2           |
    And active iterations:
      | slug  | number | total | closed | percent |
      | alpha | 009    | 3     | 1      | 33      |
    And active workers:
      | slug  | count |
      | alpha | 1     |
    When building the dashboard
    And formatting the dashboard as JSON
    Then the JSON should contain 1 project
    And the JSON project "alpha" should have status "active"
    And the JSON project "alpha" should have iteration percent 33

  Scenario: Dashboard handles empty registry
    Given an empty registry
    When building the dashboard
    And formatting the dashboard
    Then the output should be "No projects registered."
