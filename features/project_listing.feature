Feature: Project listing

  The list command formats registered projects into a table or JSON.
  It displays status, priority, iteration progress, and worker counts
  with ANSI color coding and dash placeholders for missing data.

  Scenario: List shows projects with all columns populated
    Given a project list with the following projects:
      | slug  | status | priority | iteration | closed | total | percent | workers | max-workers | path              |
      | alpha | active | high     | 009       | 1      | 3     | 33      | 1       | 2           | ~/Projects/alpha  |
      | gamma | active | low      | 002       | 2      | 2     | 100     | 0       | 1           | ~/Projects/gamma  |
    When formatting the project list
    Then the output should contain column headers "SLUG" "STATUS" "PRIORITY" "ITERATION" "PROGRESS" "WORKERS" "PATH"
    And the output should contain slug "alpha"
    And the output should contain slug "gamma"
    And the output should contain iteration "009"
    And the output should contain progress "1/3 (33%)"
    And the output should contain progress "2/2 (100%)"
    And the output should contain workers "1/2"
    And the output should contain workers "0/1"

  Scenario: List shows dash placeholders for missing data
    Given a project list with the following projects:
      | slug | status | priority | iteration | closed | total | percent | workers | max-workers | path             |
      | beta | paused | normal   |           |        |       |         | 0       | 1           | ~/Projects/beta  |
    When formatting the project list
    Then the line for "beta" should contain a dash for iteration
    And the line for "beta" should contain a dash for progress

  Scenario: List handles empty registry
    Given an empty project list
    When formatting the project list
    Then the output should be "No projects registered."

  Scenario: List colorizes status and priority
    Given a project list with the following projects:
      | slug  | status | priority | iteration | closed | total | percent | workers | max-workers | path              |
      | alpha | active | high     | 009       | 1      | 3     | 33      | 1       | 2           | ~/Projects/alpha  |
      | beta  | paused | normal   |           |        |       |         | 0       | 1           | ~/Projects/beta   |
      | gamma | active | low      | 002       | 2      | 2     | 100     | 0       | 1           | ~/Projects/gamma  |
    When formatting the project list
    Then "active" status should be colorized green
    And "paused" status should be colorized yellow
    And "high" priority should be colorized red
    And "low" priority should be colorized yellow
    And 100 percent progress should be colorized green

  Scenario: List JSON output includes all project data
    Given a project list with the following projects:
      | slug  | status | priority | iteration | closed | total | percent | workers | max-workers | path              |
      | alpha | active | high     | 009       | 1      | 3     | 33      | 1       | 2           | ~/Projects/alpha  |
    When formatting the project list as JSON
    Then the JSON output should contain a project with slug "alpha"
    And the JSON project "alpha" should have status "active"
    And the JSON project "alpha" should have priority "high"
    And the JSON project "alpha" should have iteration number "009"
    And the JSON project "alpha" should have workers 1
    And the JSON project "alpha" should have max_workers 2
