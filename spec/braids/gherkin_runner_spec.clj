(ns braids.gherkin-runner-spec
  "End-to-end tests for the gherkin runner using example features.
   These are the runner's own acceptance tests — they prove the runner works
   before we trust it with real project features."
  (:require [speclj.core :refer :all]
            [braids.gherkin-runner :as runner]))

(def examples-features "features/examples")
(def examples-step-defs "spec/step_defs/examples")

(defn- run-example-feature
  "Parse and run a single example feature file against example step defs."
  [filename]
  (let [text (slurp (str examples-features "/" filename))
        feature (runner/parse-feature-legacy text)
        step-defs (runner/load-step-defs examples-step-defs)]
    (runner/run-feature feature step-defs)))

(describe "Gherkin Runner (example features)"

  (describe "load-step-defs from examples"

    (it "loads step defs from examples directory"
      (let [defs (runner/load-step-defs examples-step-defs)]
        (should (map? defs))
        (should (pos? (count defs)))))

    (it "compiles all string keys to Patterns"
      (let [defs (runner/load-step-defs examples-step-defs)]
        (should (every? #(instance? java.util.regex.Pattern %) (keys defs))))))

  (describe "run-features integration"

    (it "returns exit code 0 when all example features pass"
      (let [exit (atom nil)]
        (with-out-str (reset! exit (runner/run-features examples-features examples-step-defs)))
        (should= 0 @exit))))

  (describe "basic.feature"

    (it "passes a simple scenario"
      (let [results (run-example-feature "basic.feature")]
        (should= 1 (count results))
        (should= :passed (:status (first results))))))

  (describe "background_steps.feature"

    (it "runs background before each scenario"
      (let [results (run-example-feature "background_steps.feature")]
        (should= 2 (count results))
        (should (every? #(= :passed (:status %)) results)))))

  (describe "capture_groups.feature"

    (it "passes with numeric and string capture groups"
      (let [results (run-example-feature "capture_groups.feature")]
        (should= 2 (count results))
        (should (every? #(= :passed (:status %)) results)))))

  (describe "and_but_keywords.feature"

    (it "handles And and But keywords in steps"
      (let [results (run-example-feature "and_but_keywords.feature")]
        (should= 1 (count results))
        (should= :passed (:status (first results)))))))
