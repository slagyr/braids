(ns braids.gherkin-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin :refer :all]
            [braids.gherkin-runner :as runner]
            [babashka.fs :as fs]))

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

(describe "compile-step-defs"
  (it "converts string keys to regex Patterns"
    (let [raw {"Given (\\d+) beads" (fn [n] n)}
          compiled (compile-step-defs raw)]
      (should= 1 (count compiled))
      (should (instance? java.util.regex.Pattern (ffirst compiled)))))

  (it "preserves Pattern keys unchanged"
    (let [p #"Given (.*)"
          raw {p (fn [x] x)}
          compiled (compile-step-defs raw)]
      (should= p (ffirst compiled))))

  (it "preserves function values"
    (let [f (fn [n] (str "got:" n))
          raw {"Given (\\d+) items" f}
          compiled (compile-step-defs raw)]
      (should= f (second (first compiled))))))

(describe "Step Matching"
  (it "matches exact string keys"
    (let [step-defs {"Given a user" (fn [] :user)
                     "When logged in" (fn [] :logged-in)
                     "Then success" (fn [] :success)}]
      (let [step {:keyword "Given" :text "a user"}]
        (should= :user ((match-step step step-defs))))
      (let [step {:keyword "When" :text "logged in"}]
        (should= :logged-in ((match-step step step-defs))))
      (let [step {:keyword "Then" :text "success"}]
        (should= :success ((match-step step step-defs))))))

  (it "matches regex Pattern keys and passes capture groups as args"
    (let [step-defs {#".* user (.*)" (fn [name] (str "user:" name))}]
      (let [step {:keyword "Given" :text "a user alice"}]
        (should= "user:alice" ((match-step step step-defs))))))

  (it "matches compiled string-regex keys with capture groups"
    (let [raw {"Given (\\d+) ready beads" (fn [n] (str "count:" n))}
          compiled (compile-step-defs raw)]
      (let [step {:keyword "Given" :text "3 ready beads"}]
        (should= "count:3" ((match-step step compiled))))))

  (it "returns nil for unmatched step"
    (let [step-defs {"Given x" (fn [] :x)}]
      (should-be-nil (match-step {:keyword "Given" :text "y"} step-defs)))))

(describe "Feature Runner"
  (it "runs scenarios successfully with per-step results"
    (let [feature {:feature "Test"
                    :scenarios [{:title "Pass"
                                 :steps [{:keyword "Given" :text "setup"} {:keyword "Then" :text "ok"}]}]}
          step-defs {"Given setup" (fn [] true) "Then ok" (fn [] true)}]
      (let [results (run-feature feature step-defs)]
        (should= :passed (:status (first results)))
        (should= "Pass" (:scenario (first results)))
        (should= [{:step "Given setup" :status :passed}
                   {:step "Then ok" :status :passed}]
                  (:steps (first results))))))

  (it "reports failed scenarios with per-step results"
    (let [feature {:feature "Test"
                    :scenarios [{:title "Fail"
                                 :steps [{:keyword "Given" :text "setup"} {:keyword "Then" :text "fail"}]}]}
          step-defs {"Given setup" (fn [] true) "Then fail" (fn [] (throw (Exception. "failed")))}]
      (let [results (run-feature feature step-defs)]
        (should= :failed (:status (first results)))
        (should= [{:step "Given setup" :status :passed}
                   {:step "Then fail" :status :failed :error "failed"}]
                  (:steps (first results))))))

  (it "eagerly executes all scenarios (not lazy)"
    (let [counter (atom 0)
          feature {:feature "Test"
                    :scenarios [{:title "S1" :steps [{:keyword "Given" :text "inc"}]}
                                {:title "S2" :steps [{:keyword "Given" :text "inc"}]}]}
          step-defs {"Given inc" (fn [] (swap! counter inc))}
          results (run-feature feature step-defs)]
      (should= 2 @counter)
      (should= 2 (count results))))

  (it "executes background steps before each scenario"
    (let [call-log (atom [])
          feature {:feature "With Background"
                   :background [{:keyword "Given" :text "common setup"}]
                   :scenarios [{:title "First scenario"
                                :steps [{:keyword "When" :text "action one"}]}
                               {:title "Second scenario"
                                :steps [{:keyword "When" :text "action two"}]}]}
          step-defs {"Given common setup" (fn [] (swap! call-log conj :bg))
                     "When action one"    (fn [] (swap! call-log conj :s1))
                     "When action two"    (fn [] (swap! call-log conj :s2))}
          results (run-feature feature step-defs)]
      (should= [:bg :s1 :bg :s2] @call-log)
      (should= :passed (:status (first results)))
      (should= :passed (:status (second results)))))

  (it "records step pass/fail results in scenario"
    (let [feature {:feature "Test"
                   :scenarios [{:title "Mixed"
                                :steps [{:keyword "Given" :text "ok step"}
                                        {:keyword "Then" :text "bad step"}]}]}
          step-defs {"Given ok step" (fn [] true)
                     "Then bad step" (fn [] (throw (Exception. "boom")))}
          results (run-feature feature step-defs)]
      (should= :failed (:status (first results)))
      (should= [{:step "Given ok step" :status :passed}
                 {:step "Then bad step" :status :failed :error "boom"}]
               (:steps (first results)))))

  (it "records all steps as passed when scenario succeeds"
    (let [feature {:feature "Test"
                   :scenarios [{:title "All pass"
                                :steps [{:keyword "Given" :text "step a"}
                                        {:keyword "When" :text "step b"}]}]}
          step-defs {"Given step a" (fn [] true)
                     "When step b"  (fn [] true)}
          results (run-feature feature step-defs)]
      (should= :passed (:status (first results)))
      (should= [{:step "Given step a" :status :passed}
                 {:step "When step b" :status :passed}]
               (:steps (first results))))))

(describe "Gherkin Runner"
  (it "run-features returns exit code 0 when all pass"
    (let [tmp (str (fs/create-temp-dir {:prefix "gherkin-run"}))]
      (fs/create-dirs (str tmp "/features"))
      (fs/create-dirs (str tmp "/step_defs"))
      (spit (str tmp "/features/pass.feature")
        "Feature: Pass\n  Scenario: OK\n    Given pass")
      (spit (str tmp "/step_defs/steps.clj")
        "{\"Given pass\" (fn [] true)}")
      (let [exit (runner/run-features (str tmp "/features") (str tmp "/step_defs"))]
        (should= 0 exit))))

  (it "run-features returns exit code 1 when a scenario fails"
    (let [tmp (str (fs/create-temp-dir {:prefix "gherkin-run"}))]
      (fs/create-dirs (str tmp "/features"))
      (fs/create-dirs (str tmp "/step_defs"))
      (spit (str tmp "/features/fail.feature")
        "Feature: Fail\n  Scenario: Bad\n    Given fail")
      (spit (str tmp "/step_defs/steps.clj")
        "{\"Given fail\" (fn [] (throw (Exception. \"boom\")))}")
      (let [exit (runner/run-features (str tmp "/features") (str tmp "/step_defs"))]
        (should= 1 exit))))

  (it "load-step-defs dereferences Vars from step def files"
    (let [tmp (str (fs/create-temp-dir {:prefix "gherkin-load"}))]
      (fs/create-dirs tmp)
      (spit (str tmp "/steps.clj")
        "(def step-defs {\"Given hello\" (fn [] :hello)})\n")
      (let [defs (runner/load-step-defs tmp)]
        (should (map? defs))
        ;; Keys are compiled to Patterns by compile-step-defs
        (should= 1 (count defs))
        (should (instance? java.util.regex.Pattern (ffirst defs))))))

  (it "load-step-defs handles plain map (no def wrapper)"
    (let [tmp (str (fs/create-temp-dir {:prefix "gherkin-load"}))]
      (fs/create-dirs tmp)
      (spit (str tmp "/steps.clj")
        "{\"Given world\" (fn [] :world)}")
      (let [defs (runner/load-step-defs tmp)]
        (should (map? defs))
        (should= 1 (count defs))
        (should (instance? java.util.regex.Pattern (ffirst defs)))))))