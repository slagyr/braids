Feature: Zombie detection

  The orchestrator detects zombie worker sessions — sessions that
  should no longer be running. Zombies are detected by examining
  session status, bead status, and session age against timeouts.

  Background:
    Given the harness is reset
    Given a project "proj" with worker-timeout 3600

  Scenario: Detect zombie when bead is closed but session still running
    Given a session "s1" with label "project:proj:proj-abc"
    And session "s1" has status "running" and age 100 seconds
    And bead "proj-abc" has status "closed"
    When checking for zombies
    Then session "s1" should be a zombie with reason "bead-closed"

  Scenario: Detect zombie when session exceeds timeout
    Given a session "s2" with label "project:proj:proj-def"
    And session "s2" has status "running" and age 7200 seconds
    And bead "proj-def" has status "open"
    When checking for zombies
    Then session "s2" should be a zombie with reason "timeout"

  Scenario: No zombie when session is within timeout and bead is open
    Given a session "s3" with label "project:proj:proj-ghi"
    And session "s3" has status "running" and age 100 seconds
    And bead "proj-ghi" has status "open"
    When checking for zombies
    Then no zombies should be detected

  Scenario: Detect zombie when session has ended
    Given a session "s4" with label "project:proj:proj-jkl"
    And session "s4" has status "completed" and age 50 seconds
    And bead "proj-jkl" has status "open"
    When checking for zombies
    Then session "s4" should be a zombie with reason "session-ended"

  Scenario: Missing bead status defaults to open
    Given a session "s5" with label "project:proj:proj-mno"
    And session "s5" has status "running" and age 100 seconds
    And bead "proj-mno" has no recorded status
    When checking for zombies
    Then no zombies should be detected

  @wip
  Scenario: Zombie cleanup kills sessions and reports results
    Given zombies have been detected
    When cleaning up zombies
    Then the zombie sessions should be killed
    And a cleanup report should list each killed session and its reason

  @wip
  Scenario: Zombie detection across multiple projects
    Given a project "other" with worker-timeout 1800
    And a session "s6" with label "project:proj:proj-p1"
    And session "s6" has status "running" and age 100 seconds
    And bead "proj-p1" has status "closed"
    And a session "s7" with label "project:other:other-q2"
    And session "s7" has status "running" and age 3600 seconds
    And bead "other-q2" has status "open"
    When checking for zombies
    Then session "s6" should be a zombie with reason "bead-closed"
    And session "s7" should be a zombie with reason "timeout"
