Feature: Project lifecycle

  The braids system initializes its directory structure and registry,
  then lets users create new projects with validated configuration.
  Init checks prerequisites and plans actions; new project creation
  validates inputs and builds config with sensible defaults.

  Scenario: Init checks bd availability
    Given bd is not available
    And no registry exists
    When checking prerequisites
    Then prerequisites should fail with "bd (beads) is not installed"

  Scenario: Init fails when registry exists without force
    Given bd is available
    And a registry already exists
    And force is not set
    When checking prerequisites
    Then prerequisites should fail with "braids is already initialized"

  Scenario: Init allows reinit with force flag
    Given bd is available
    And a registry already exists
    And force is set
    When checking prerequisites
    Then prerequisites should pass

  Scenario: Init plans directory creation for fresh install
    Given braids dir does not exist
    And braids home does not exist
    When planning init
    Then the plan should include "create-braids-dir"
    And the plan should include "create-braids-home"
    And the plan should include "create-registry"
    And the plan should include "save-config"

  Scenario: Init skips existing directories in plan
    Given braids dir already exists
    And braids home already exists
    When planning init
    Then the plan should not include "create-braids-dir"
    And the plan should not include "create-braids-home"
    And the plan should include "create-registry"
    And the plan should include "save-config"

  Scenario: New project validates slug format
    Given a new project with slug "Bad Slug"
    And name "My Project"
    And goal "Build something"
    When validating new project params
    Then validation should fail with "Invalid slug"

  Scenario: New project rejects duplicate slug
    Given a registry with project "my-project"
    And a new registry entry with slug "my-project"
    When adding the entry to the registry
    Then it should fail with "already exists"

  Scenario: New project builds config with defaults
    Given a new project with name "My Project"
    When building the project config
    Then the config status should be "active"
    And the config priority should be "normal"
    And the config autonomy should be "full"
    And the config max-workers should be 1
    And the config worker-timeout should be 1800
