Feature: Background steps
  Background:
    Given a fresh context

  Scenario: First scenario uses background
    When the first action runs
    Then the context was initialized

  Scenario: Second scenario also uses background
    When the second action runs
    Then the context was initialized
