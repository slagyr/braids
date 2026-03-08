(ns braids.features.parser-spec
  (:require [speclj.core :refer :all]
            [braids.features.parser :as gherkin]))

(describe "Gherkin Parser"

  (context "parse-feature"

    (it "parses a minimal feature with one scenario"
      (let [text "Feature: Simple feature\n\n  Scenario: Basic test\n    Given a step\n    When another step\n    Then final step"
            result (gherkin/parse-feature text)]
        (should= "Simple feature" (:feature result))
        (should= 1 (count (:scenarios result)))
        (should= "Basic test" (-> result :scenarios first :scenario))
        (should= [{:type :given :text "a step"}
                  {:type :when  :text "another step"}
                  {:type :then  :text "final step"}]
                 (-> result :scenarios first :steps))))

    (it "strips Given/When/Then keywords from step text"
      (let [text "Feature: Keyword stripping\n\n  Scenario: Strip keywords\n    Given the first step\n    When the second step\n    Then the third step"
            result (gherkin/parse-feature text)]
        (should= [{:type :given :text "the first step"}
                  {:type :when  :text "the second step"}
                  {:type :then  :text "the third step"}]
                 (-> result :scenarios first :steps))))

    (it "appends And steps to the previous phase"
      (let [text "Feature: And steps\n\n  Scenario: And handling\n    Given first given\n    And second given\n    When first when\n    And second when\n    Then first then\n    And second then"
            result (gherkin/parse-feature text)]
        (should= [{:type :given :text "first given"}
                  {:type :and   :text "second given"}
                  {:type :when  :text "first when"}
                  {:type :and   :text "second when"}
                  {:type :then  :text "first then"}
                  {:type :and   :text "second then"}]
                 (-> result :scenarios first :steps))))

    (it "appends But steps to the previous phase"
      (let [text "Feature: But steps\n\n  Scenario: But handling\n    Given a condition\n    But not another condition\n    When an action\n    But not another action\n    Then a result\n    But not another result"
            result (gherkin/parse-feature text)]
        (should= [{:type :given :text "a condition"}
                  {:type :but   :text "not another condition"}
                  {:type :when  :text "an action"}
                  {:type :but   :text "not another action"}
                  {:type :then  :text "a result"}
                  {:type :but   :text "not another result"}]
                 (-> result :scenarios first :steps))))

    (it "parses background givens separately from scenarios"
      (let [text "Feature: Feature with background\n\n  Background:\n    Given a common setup\n    And another common setup\n\n  Scenario: First scenario\n    Given a specific given\n    When something happens\n    Then a result"
            result (gherkin/parse-feature text)]
        (should= {:steps [{:type :given :text "a common setup"}
                          {:type :and   :text "another common setup"}]}
                 (:background result))
        (should= [{:type :given :text "a specific given"}
                  {:type :when  :text "something happens"}
                  {:type :then  :text "a result"}]
                 (-> result :scenarios first :steps))))

    (it "does NOT merge background givens into scenarios"
      (let [text "Feature: Background separation\n\n  Background:\n    Given background step\n\n  Scenario: Test scenario\n    Given scenario step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should= {:steps [{:type :given :text "background step"}]} (:background result))
        (should= [{:type :given :text "scenario step"}
                  {:type :when  :text "action"}
                  {:type :then  :text "result"}]
                 (-> result :scenarios first :steps))))

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
        (should= [{:type :when :text "something happens"}
                  {:type :then :text "a result"}]
                 (-> result :scenarios first :steps))))

    (it "has no background key when feature has no background"
      (let [text "Feature: No background\n\n  Scenario: Simple\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should-not-contain :background result)))

    (it "attaches data table to the preceding step"
      (let [text (str "Feature: Table support\n\n"
                      "  Scenario: With table\n"
                      "    Given a list of items:\n"
                      "      | name  | value |\n"
                      "      | alpha | 1     |\n"
                      "      | beta  | 2     |\n"
                      "    When action\n"
                      "    Then result")
            result (gherkin/parse-feature text)
            first-step (-> result :scenarios first :steps first)]
        (should= {:headers ["name" "value"]
                  :rows [["alpha" "1"] ["beta" "2"]]}
                 (:table first-step))))

    (it "step without table has no :table key"
      (let [text "Feature: No table\n\n  Scenario: Simple\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)
            first-step (-> result :scenarios first :steps first)]
        (should-not-contain :table first-step))))

  (context "parse-feature-file"

    (it "includes source filename in the result"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= "orch_spawning.feature" (:source result))
        (should= "Orchestrator spawning behavior" (:feature result))))

    (it "parses orch_spawning.feature with raw step text"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= :given (-> result :background :steps first :type))
        (should= "configured projects:" (-> result :background :steps first :text))
        (should (-> result :background :steps first :table))
        (should= ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path"]
                 (-> result :background :steps first :table :headers))
        (should= 11 (count (:scenarios result)))
        (let [first-scenario (first (:scenarios result))]
          (should= "Spawn includes all invocation attributes" (:scenario first-scenario))
          (should= :given (-> first-scenario :steps first :type))
          (should= "configured projects:" (-> first-scenario :steps first :text))
          (should (-> first-scenario :steps first :table))
          (should= {:type :when :text "the orchestrator ticks"}
                   (nth (:steps first-scenario) 2))
          (should= {:type :then :text "the action should be \"spawn\""}
                   (nth (:steps first-scenario) 3)))))

    (it "parses worker_session_tracking.feature with raw text and @wip tags"
      (let [result (gherkin/parse-feature-file "features/worker_session_tracking.feature")]
        (should= "Worker session tracking" (:feature result))
        (should-be-nil (:background result))
        (should= 6 (count (:scenarios result)))
        (let [s (first (:scenarios result))]
          (should= "Generate deterministic session ID from bead ID" (:scenario s))
          (should= [{:type :given :text "a bead with id \"proj-abc\""}
                    {:type :when  :text "generating the session ID"}
                    {:type :then  :text "the session ID should be \"braids-proj-abc-worker\""}]
                   (:steps s)))
        (let [s (nth (:scenarios result) 3)]
          (should= "Session ID can be parsed back to bead ID" (:scenario s))
          (should= [{:type :given :text "a session ID \"braids-proj-abc-worker\""}
                    {:type :when  :text "parsing the session ID"}
                    {:type :then  :text "the extracted bead ID should be \"proj-abc\""}]
                   (:steps s)))
        (should-be-nil (:wip (nth (:scenarios result) 0)))
        (should-be-nil (:wip (nth (:scenarios result) 3)))
        (should= true (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))))

    (it "parses zombie_detection.feature with background and @wip"
      (let [result (gherkin/parse-feature-file "features/zombie_detection.feature")]
        (should= "Zombie detection" (:feature result))
        (should= {:steps [{:type :given :text "a project \"proj\" with worker-timeout 3600"}]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        (let [first-scenario (first (:scenarios result))]
          (should= [{:type :given :text "a session \"s1\" with label \"project:proj:proj-abc\""}
                    {:type :and   :text "session \"s1\" has status \"running\" and age 100 seconds"}
                    {:type :and   :text "bead \"proj-abc\" has status \"closed\""}]
                   (take 3 (:steps first-scenario)))
          (should= {:type :when :text "checking for zombies"}
                   (nth (:steps first-scenario) 3))
          (should= [{:type :then :text "session \"s1\" should be a zombie with reason \"bead-closed\""}]
                   (drop 4 (:steps first-scenario))))
        (should-be-nil (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))
        (should= true (:wip (nth (:scenarios result) 6)))))

    (it "preserves feature description text from files"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= "The orchestrator tick examines project state and decides whether\nto spawn workers or remain idle. It respects max-workers capacity,\nrequires active iterations, and reports idle reasons. When spawning,\nit produces a list of spawn entries with all attributes needed to\ninvoke the worker agents."
                 (:description result)))))

  (context "parse-features-dir"

    (it "parses all .feature files in a directory"
      (let [results (gherkin/parse-features-dir "features")]
        (should= 11 (count results))
        (should (every? :source results))
        (should (every? :feature results))
        (should (every? :scenarios results)))))

  (context "write-edn"

    (it "writes parsed feature to .edn file"
      (let [ir {:source "test.feature"
                :feature "Test"
                :scenarios [{:scenario "S1"
                             :steps [{:type :given :text "a"}
                                     {:type :when  :text "b"}
                                     {:type :then  :text "c"}]}]}
            tmp-file (str "/tmp/test-gherkin-" (System/currentTimeMillis) ".edn")]
        (gherkin/write-edn tmp-file ir)
        (let [content (slurp tmp-file)
              parsed (read-string content)]
          (should= ir parsed))
        (clojure.java.io/delete-file tmp-file true)))))
