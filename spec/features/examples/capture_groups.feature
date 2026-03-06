Feature: Regex capture groups
  Scenario: Numeric capture group
    Given 5 items in the cart
    Then the cart has 5 items

  Scenario: String capture group
    Given a user named "Alice"
    Then the greeting is "Hello, Alice"
