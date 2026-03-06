(ns braids.gherkin-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin :as gherkin]))

(describe "Gherkin Parser"

  (describe "parse-feature"

    (it "parses a minimal feature with one scenario"
      (let [text "Feature: Simple feature\n\n  Scenario: Basic test\n    Given a step\n    When another step\n    Then final step"
            result (gherkin/parse-feature text)]
        (should= "Simple feature" (:feature result))
        (should= 1 (count (:scenarios result)))
        (should= "Basic test" (-> result :scenarios first :scenario))
        (should= ["a step"] (-> result :scenarios first :givens))
        (should= ["another step"] (-> result :scenarios first :whens))
        (should= ["final step"] (-> result :scenarios first :thens))))

    (it "strips Given/When/Then keywords from step text"
      (let [text "Feature: Keyword stripping\n\n  Scenario: Strip keywords\n    Given the first step\n    When the second step\n    Then the third step"
            result (gherkin/parse-feature text)]
        (should= ["the first step"] (-> result :scenarios first :givens))
        (should= ["the second step"] (-> result :scenarios first :whens))
        (should= ["the third step"] (-> result :scenarios first :thens))))

    (it "appends And steps to the previous phase"
      (let [text "Feature: And steps\n\n  Scenario: And handling\n    Given first given\n    And second given\n    When first when\n    And second when\n    Then first then\n    And second then"
            result (gherkin/parse-feature text)]
        (should= ["first given" "second given"] (-> result :scenarios first :givens))
        (should= ["first when" "second when"] (-> result :scenarios first :whens))
        (should= ["first then" "second then"] (-> result :scenarios first :thens))))

    (it "appends But steps to the previous phase"
      (let [text "Feature: But steps\n\n  Scenario: But handling\n    Given a condition\n    But not another condition\n    When an action\n    But not another action\n    Then a result\n    But not another result"
            result (gherkin/parse-feature text)]
        (should= ["a condition" "not another condition"] (-> result :scenarios first :givens))
        (should= ["an action" "not another action"] (-> result :scenarios first :whens))
        (should= ["a result" "not another result"] (-> result :scenarios first :thens))))

    (it "parses background givens separately from scenarios"
      (let [text "Feature: Feature with background\n\n  Background:\n    Given a common setup\n    And another common setup\n\n  Scenario: First scenario\n    Given a specific given\n    When something happens\n    Then a result"
            result (gherkin/parse-feature text)]
        (should= {:givens ["a common setup" "another common setup"]}
                 (:background result))
        (should= ["a specific given"] (-> result :scenarios first :givens))
        (should= ["something happens"] (-> result :scenarios first :whens))
        (should= ["a result"] (-> result :scenarios first :thens))))

    (it "does NOT merge background givens into scenarios"
      (let [text "Feature: Background separation\n\n  Background:\n    Given background step\n\n  Scenario: Test scenario\n    Given scenario step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should= {:givens ["background step"]} (:background result))
        (should= ["scenario step"] (-> result :scenarios first :givens))))

    (it "parses multiple scenarios"
      (let [text "Feature: Multiple scenarios\n\n  Scenario: First\n    Given step A\n    When action A\n    Then result A\n\n  Scenario: Second\n    Given step B\n    When action B\n    Then result B"
            result (gherkin/parse-feature text)]
        (should= 2 (count (:scenarios result)))
        (should= "First" (-> result :scenarios first :scenario))
        (should= "Second" (-> result :scenarios second :scenario))))

    (it "preserves feature description text"
      (let [text "Feature: Described feature\n\n  This is the description of the feature.\n  It can span multiple lines.\n\n  Scenario: A test\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should= "This is the description of the feature.\nIt can span multiple lines."
                 (:description result))))

    (it "marks @wip tagged scenarios with :wip true"
      (let [text "Feature: WIP feature\n\n  Scenario: Normal scenario\n    Given a step\n    When action\n    Then result\n\n  @wip\n  Scenario: WIP scenario\n    Given a wip step\n    When wip action\n    Then wip result"
            result (gherkin/parse-feature text)]
        (should-be-nil (:wip (-> result :scenarios first)))
        (should= true (:wip (-> result :scenarios second)))))

    (it "handles scenario with no given steps"
      (let [text "Feature: No givens\n\n  Scenario: When/then only\n    When something happens\n    Then a result"
            result (gherkin/parse-feature text)]
        (should= [] (-> result :scenarios first :givens))
        (should= ["something happens"] (-> result :scenarios first :whens))
        (should= ["a result"] (-> result :scenarios first :thens))))

    (it "has no background key when feature has no background"
      (let [text "Feature: No background\n\n  Scenario: Simple\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should-not-contain :background result))))

  (describe "parse-feature-file"

    (it "includes source filename in the result"
      (let [result (gherkin/parse-feature-file "spec/features/orch_spawning.feature")]
        (should= "orch_spawning.feature" (:source result))
        (should= "Orchestrator spawning behavior" (:feature result))))

    (it "parses orch_spawning.feature correctly"
      (let [result (gherkin/parse-feature-file "spec/features/orch_spawning.feature")]
        (should= {:givens ["a project \"alpha\" with max-workers 2"
                           "project \"alpha\" has an active iteration \"003\""]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        (let [first-scenario (first (:scenarios result))]
          (should= "Spawn workers when beads ready and capacity available" (:scenario first-scenario))
          (should= ["project \"alpha\" has 3 ready beads"
                    "project \"alpha\" has 0 active workers"]
                   (:givens first-scenario))
          (should= ["the orchestrator ticks"] (:whens first-scenario))
          (should= ["the action should be \"spawn\""
                    "2 workers should be spawned"]
                   (:thens first-scenario)))))

    (it "parses worker_session_tracking.feature with @wip tags"
      (let [result (gherkin/parse-feature-file "spec/features/worker_session_tracking.feature")]
        (should= "Worker session tracking" (:feature result))
        (should-be-nil (:background result))
        (should= 6 (count (:scenarios result)))
        ;; The last two scenarios have @wip tags
        (should-be-nil (:wip (nth (:scenarios result) 0)))
        (should-be-nil (:wip (nth (:scenarios result) 3)))
        (should= true (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))))

    (it "parses zombie_detection.feature with background and @wip"
      (let [result (gherkin/parse-feature-file "spec/features/zombie_detection.feature")]
        (should= "Zombie detection" (:feature result))
        (should= {:givens ["a project \"proj\" with worker-timeout 3600"]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        ;; Last two are @wip
        (should-be-nil (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))
        (should= true (:wip (nth (:scenarios result) 6)))))

    (it "preserves feature description text from files"
      (let [result (gherkin/parse-feature-file "spec/features/orch_spawning.feature")]
        (should= "The orchestrator tick examines project state and decides whether\nto spawn workers or remain idle. It respects max-workers capacity,\nrequires active iterations, and reports idle reasons."
                 (:description result)))))

  (describe "parse-features-dir"

    (it "parses all .feature files in a directory"
      (let [results (gherkin/parse-features-dir "spec/features")]
        (should= 3 (count results))
        (should (every? :source results))
        (should (every? :feature results))
        (should (every? :scenarios results)))))

  (describe "write-edn"

    (it "writes parsed feature to .edn file"
      (let [ir {:source "test.feature"
                :feature "Test"
                :scenarios [{:scenario "S1" :givens ["a"] :whens ["b"] :thens ["c"]}]}
            tmp-file (str "/tmp/test-gherkin-" (System/currentTimeMillis) ".edn")]
        (gherkin/write-edn tmp-file ir)
        (let [content (slurp tmp-file)
              parsed (read-string content)]
          (should= ir parsed))
        (clojure.java.io/delete-file tmp-file true)))))
