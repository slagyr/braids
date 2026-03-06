Feature: Iteration management

  Iterations organize beads into delivery cycles with planning,
  active, and complete phases. The system parses iteration EDN
  with sensible defaults, validates structure, annotates stories
  with live bead data, and computes completion statistics.

  Scenario: Parse iteration EDN with defaults for missing fields
    Given iteration EDN with number "003" and status "active" and 1 story
    And the EDN has no guardrails or notes
    When parsing the iteration EDN
    Then the iteration number should be "003"
    And the iteration status should be "active"
    And the iteration guardrails should be empty
    And the iteration notes should be empty

  Scenario: Validate rejects invalid iteration status
    Given an iteration with number "001" and status "bogus" and stories
    When validating the iteration
    Then validation should fail with "Invalid status"

  Scenario: Validate rejects missing required fields
    Given an iteration with no number
    When validating the iteration
    Then validation should fail with "Missing :number"

  Scenario: Annotate stories with bead data
    Given an iteration with stories "proj-abc" and "proj-def"
    And bead "proj-abc" has status "open" and priority 1
    And bead "proj-def" has status "closed" and priority 2
    When annotating stories with bead data
    Then story "proj-abc" should have status "open"
    And story "proj-def" should have status "closed"

  Scenario: Annotate marks missing beads as unknown
    Given an iteration with story "proj-xyz"
    And no bead data exists
    When annotating stories with bead data
    Then story "proj-xyz" should have status "unknown"

  Scenario: Completion stats calculation
    Given annotated stories with 2 closed and 2 open out of 4 total
    When calculating completion stats
    Then the total should be 4
    And the closed count should be 2
    And the completion percent should be 50

  Scenario: Completion stats for empty iteration
    Given an iteration with no stories
    When calculating completion stats
    Then the total should be 0
    And the closed count should be 0
    And the completion percent should be 0

  Scenario: Format iteration with status icons
    Given an iteration "009" with status "active"
    And a story "proj-abc" with status "open"
    And a story "proj-def" with status "closed"
    And completion stats of 1 closed out of 2
    When formatting the iteration
    Then the output should contain "Iteration 009"
    And the output should contain "active"
    And the output should contain "50%"

  Scenario: Format iteration as JSON
    Given an iteration "001" with status "active"
    And a story "a" with status "open"
    And completion stats of 0 closed out of 1
    When formatting the iteration as JSON
    Then the JSON should contain "number"
    And the JSON should contain "stories"
    And the JSON should contain "percent"
