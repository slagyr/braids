Feature: Worker session tracking

  Worker sessions use deterministic IDs derived from bead IDs.
  This enables the orchestrator to track, deduplicate, and
  manage worker sessions without external state.

  Scenario: Generate deterministic session ID from bead ID
    Given a bead with id "proj-abc"
    When generating the session ID
    Then the session ID should be "braids-proj-abc-worker"

  Scenario: Same bead always generates same session ID
    Given a bead with id "proj-xyz"
    When generating the session ID twice
    Then both session IDs should be identical

  Scenario: Different beads generate different session IDs
    Given a bead with id "proj-aaa"
    And another bead with id "proj-bbb"
    When generating session IDs for both
    Then the session IDs should be different

  Scenario: Session ID can be parsed back to bead ID
    Given a session ID "braids-proj-abc-worker"
    When parsing the session ID
    Then the extracted bead ID should be "proj-abc"

  Scenario: Prevent duplicate spawning when session already active
    Given a bead with id "proj-dup"
    And a session "braids-proj-dup-worker" is already active
    When the orchestrator considers spawning for bead "proj-dup"
    Then spawning should be prevented with reason "session-already-active"

  Scenario: Session with missing bead data is marked for cleanup
    Given a session with id "braids-proj-gone-worker"
    And no bead exists with id "proj-gone"
    When checking session validity
    Then the session should be flagged for cleanup
