(ns braids.gherkin-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin :refer :all]))

(describe "Gherkin Parser"
  (it "parses a simple feature file"
    (let [feature-text "Feature: Test feature
  Scenario: Simple scenario
    Given a step
    When another step
    Then final step"]
      (let [parsed (parse-feature feature-text)]
        (should= {:feature "Test feature"
                  :scenarios [{:title "Simple scenario"
                               :steps [{:keyword "Given" :text "a step"}
                                       {:keyword "When" :text "another step"}
                                       {:keyword "Then" :text "final step"}]}]}
                 parsed))))

  (it "handles Background"
    (let [feature-text "Feature: Feature with background
  Background:
    Given common setup
  Scenario: Scenario with background
    When something happens
    Then result"]
      (let [parsed (parse-feature feature-text)]
        (should= {:feature "Feature with background"
                  :background [{:keyword "Given" :text "common setup"}]
                  :scenarios [{:title "Scenario with background"
                               :steps [{:keyword "When" :text "something happens"}
                                       {:keyword "Then" :text "result"}]}]}
                 parsed))))

  (it "handles And/But steps"
    (let [feature-text "Feature: And/But steps
  Scenario: And/But scenario
    Given first step
    And second step
    When action
    But exception
    Then result"]
      (let [parsed (parse-feature feature-text)]
        (should= {:feature "And/But steps"
                  :scenarios [{:title "And/But scenario"
                               :steps [{:keyword "Given" :text "first step"}
                                       {:keyword "And" :text "second step"}
                                       {:keyword "When" :text "action"}
                                       {:keyword "But" :text "exception"}
                                       {:keyword "Then" :text "result"}]}]}
                 parsed)))))

(describe "Step Matching"
  (it "matches steps to definitions"
    (let [step-defs {"Given a user" (fn [] :user)
                     "When logged in" (fn [] :logged-in)
                     "Then success" (fn [] :success)}]
      (let [step {:keyword "Given" :text "a user"}]
        (should= :user ((match-step step step-defs))))
      (let [step {:keyword "When" :text "logged in"}]
        (should= :logged-in ((match-step step step-defs))))
      (let [step {:keyword "Then" :text "success"}]
        (should= :success ((match-step step step-defs))))))

  (it "handles regex matching"
    (let [step-defs {#".* user (.*)" (fn [name] (str "user:" name))}]
      (let [step {:keyword "Given" :text "a user alice"}]
        (should= "user:alice" ((match-step step step-defs)))))))

(describe "Feature Runner"
  (it "runs scenarios successfully"
    (let [feature {:feature "Test"
                   :scenarios [{:title "Pass"
                                :steps [{:keyword "Given" :text "setup"} {:keyword "Then" :text "ok"}]}]}
          step-defs {"Given setup" (fn [] true) "Then ok" (fn [] true)}]
      (let [results (run-feature feature step-defs)]
        (should= [{:scenario "Pass" :status :passed :steps []}] results))))

  (it "reports failed scenarios"
    (let [feature {:feature "Test"
                   :scenarios [{:title "Fail"
                                :steps [{:keyword "Given" :text "setup"} {:keyword "Then" :text "fail"}]}]}
          step-defs {"Given setup" (fn [] true) "Then fail" (fn [] (throw (Exception. "failed")))}]
      (let [results (run-feature feature step-defs)]
        (should= [{:scenario "Fail" :status :failed :steps []}] results)))))