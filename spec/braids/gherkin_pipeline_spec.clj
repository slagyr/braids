(ns braids.gherkin-pipeline-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin-pipeline :as pipeline]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private tmp-root (str "/tmp/pipeline-test-" (System/currentTimeMillis)))
(def ^:private features-dir (str tmp-root "/features"))
(def ^:private edn-dir (str tmp-root "/edn"))
(def ^:private generated-dir (str tmp-root "/generated"))

(describe "Gherkin Pipeline"

  (before-all
    ;; Create a minimal .feature file for testing
    (io/make-parents (io/file features-dir "dummy"))
    (spit (str features-dir "/simple.feature")
          "Feature: Simple pipeline test\n\n  Scenario: It works\n    Given a thing\n    When it runs\n    Then it passes\n"))

  (after-all
    ;; Cleanup
    (doseq [dir [generated-dir edn-dir features-dir]]
      (when (.exists (io/file dir))
        (doseq [f (.listFiles (io/file dir))]
          (.delete f))
        (.delete (io/file dir))))
    (.delete (io/file tmp-root)))

  (describe "run-pipeline!"

    (it "parses .feature files to .edn"
      (with-out-str (pipeline/run-pipeline! features-dir edn-dir generated-dir))
      (should (.exists (io/file (str edn-dir "/simple.edn")))))

    (it "generates spec files from .edn"
      (with-out-str (pipeline/run-pipeline! features-dir edn-dir generated-dir))
      (should (.exists (io/file (str generated-dir "/simple_spec.clj")))))

    (it "generated spec contains correct namespace"
      (with-out-str (pipeline/run-pipeline! features-dir edn-dir generated-dir))
      (let [content (slurp (str generated-dir "/simple_spec.clj"))]
        (should-contain "(ns braids.features.simple-spec" content)))

    (it "generated spec contains the scenario"
      (with-out-str (pipeline/run-pipeline! features-dir edn-dir generated-dir))
      (let [content (slurp (str generated-dir "/simple_spec.clj"))]
        (should-contain "(context \"It works\"" content)
        (should-contain "(pending \"not yet implemented\")" content))))

  (describe "default-dirs"

    (it "returns a map with default directory paths"
      (let [dirs (pipeline/default-dirs)]
        (should= "features" (:features-dir dirs))
        (should= "features/edn" (:edn-dir dirs))
        (should= "features/generated" (:generated-dir dirs))))))
