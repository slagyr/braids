Feature: Worker session tracking

  Background:
    Given an orchestrator with session tracking enabled

  Scenario: Generate deterministic session ID from bead ID
    Given a bead with ID "braids-test-123"
    When generating session ID for the bead
    Then the session ID should be "braids-braids-test-123-worker"

  Scenario: Same bead generates same session ID consistently
    Given a bead with ID "braids-consistent-456"
    When generating session ID multiple times
    Then all generated IDs should be identical

  Scenario: Different beads generate different session IDs
    Given beads with IDs "braids-a-001" and "braids-b-002"
    When generating session IDs for each
    Then the session IDs should be different

  Scenario: Prevent duplicate spawning with session tracking
    Given a worker session "braids-active-worker" is already active
    When attempting to spawn another worker for the same bead
    Then spawning should be prevented with reason "session-already-active"

  Scenario: Allow spawning when session is not active
    Given no active session for bead "braids-new-bead"
    When attempting to spawn worker for the bead
    Then spawning should proceed normally

  Scenario: Session tracking handles missing bead data
    Given a session exists but bead data is missing
    When checking session validity
    Then the session should be marked for cleanup

  Scenario: Session ID collision detection
    Given two different beads generate the same session ID
    When checking for collisions
    Then a warning should be logged and collision handled