Feature: Configuration

  The config command manages braids global settings stored in config.edn.
  It supports listing all keys, getting individual values, setting values,
  and applies defaults for missing keys during parsing.

  Scenario: Config list shows all keys sorted alphabetically
    Given a config with values:
      | key                    | value       |
      | braids-home            | ~/Projects  |
      | orchestrator-channel   |             |
      | env-path               |             |
      | bd-bin                 | bd          |
      | openclaw-bin           | openclaw    |
    When listing the config
    Then the output should contain "bd-bin = bd"
    And the output should contain "braids-home = ~/Projects"
    And the output should contain "openclaw-bin = openclaw"
    And "bd-bin" should appear before "braids-home" in the output
    And "braids-home" should appear before "openclaw-bin" in the output

  Scenario: Config get returns value for existing key
    Given a config with values:
      | key         | value         |
      | braids-home | /custom/path  |
    When getting config key "braids-home"
    Then the result should be ok with value "/custom/path"

  Scenario: Config get returns error for missing key
    Given a config with values:
      | key         | value       |
      | braids-home | ~/Projects  |
    When getting config key "nonexistent"
    Then the result should be an error
    And the error message should contain "nonexistent"
    And the error message should contain "not found"

  Scenario: Config set updates value
    Given a config with values:
      | key         | value       |
      | braids-home | ~/Projects  |
    When setting config key "braids-home" to "/new/path"
    Then the config should have "braids-home" set to "/new/path"

  Scenario: Config defaults applied on parse
    Given an empty config string
    When parsing the config
    Then the config should have "braids-home" set to "~/Projects"
    And the config should have "bd-bin" set to "bd"
    And the config should have "openclaw-bin" set to "openclaw"

  Scenario: Config help output
    When requesting config help
    Then the output should contain "Usage: braids config"
    And the output should contain "list"
    And the output should contain "get <key>"
    And the output should contain "set <key> <val>"
