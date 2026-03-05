Feature: Zombie detection

  Background:
    Given an orchestrator with worker-timeout set to 3600

  Scenario: Detect zombie when bead is closed but session still active
    Given a worker session "braids-test-123-worker" is active
    And the corresponding bead "braids-test-123" is closed
    When checking for zombies
    Then the session should be marked as zombie with reason "bead-closed"

  Scenario: Detect zombie when session exceeds timeout
    Given a worker session started 2 hours ago
    And the corresponding bead is still open
    When checking for zombies
    Then the session should be marked as zombie with reason "timeout"

  Scenario: Do not mark active worker as zombie
    Given a worker session "braids-active-worker" is active
    And the corresponding bead "braids-active" is in-progress
    When checking for zombies
    Then no zombies should be detected

  Scenario: Clean up zombie sessions
    Given zombies detected with sessions "zombie-1" and "zombie-2"
    When cleaning up zombies
    Then the sessions should be killed
    And a cleanup log should be generated

  Scenario: Multiple zombies from different projects
    Given zombies from project "braids" and "other-project"
    When checking for zombies across all projects
    Then all zombies should be detected regardless of project

  Scenario: Zombie detection handles missing session data
    Given a bead references a session that no longer exists
    When checking for zombies
    Then it should not crash and continue processing

  Scenario: Zombie cleanup reports success
    Given zombies were successfully cleaned up
    When generating cleanup report
    Then the report should list killed sessions and reasons