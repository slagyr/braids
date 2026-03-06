(ns braids.gherkin-pipeline
  (:require [braids.gherkin :as parser]
            [braids.gherkin-generator :as generator]))

(def default-features-dir "features")
(def default-edn-dir "features/edn")
(def default-generated-dir "features/generated")

(defn default-dirs
  "Return a map of default directory paths for the pipeline."
  []
  {:features-dir  default-features-dir
   :edn-dir       default-edn-dir
   :generated-dir default-generated-dir})

(defn run-pipeline!
  "Run the full feature pipeline: parse .feature -> .edn -> generated speclj specs."
  ([]
   (let [{:keys [features-dir edn-dir generated-dir]} (default-dirs)]
     (run-pipeline! features-dir edn-dir generated-dir)))
  ([features-dir edn-dir generated-dir]
   (parser/parse-features! features-dir edn-dir)
   (generator/generate-features! edn-dir generated-dir)))
