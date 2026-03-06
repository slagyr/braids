Feature: And/But keywords
  Scenario: Steps with And and But
    Given the system is ready
    And the user is logged in
    When the action is performed
    But the retry flag is set
    Then the result is successful
    And the audit log is updated
