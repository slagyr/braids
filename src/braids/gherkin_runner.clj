(ns braids.gherkin-runner
  (:require [braids.gherkin :as gherkin]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; Bug 1: load-file returns a Var when the file uses (def ...).
;; Dereference Vars to get the underlying map value.

(defn- deref-if-var
  "Dereference a Var to get its value; return non-Vars as-is."
  [x]
  (if (var? x) @x x))

(defn load-step-defs
  "Load step definition files from a directory.
   Each file should evaluate to a map (or def a map).
   String keys are compiled to regex Patterns.
   Returns a merged, compiled step-defs map."
  ([] (load-step-defs "spec/step_defs"))
  ([step-dir-path]
   (let [step-dir (io/file step-dir-path)]
     (when (.exists step-dir)
       (->> (.listFiles step-dir)
            (filter #(str/ends-with? (.getName %) ".clj"))
            (map #(deref-if-var (load-file (.getPath %))))
            (apply merge)
            gherkin/compile-step-defs)))))

;; Bug 4: Return exit code instead of calling System/exit.

(defn run-features
  "Run all .feature files from feature-dir against step defs from step-dir.
   Returns exit code: 0 if all pass, 1 if any fail or dirs missing."
  ([] (run-features "spec/features" "spec/step_defs"))
  ([feature-dir-path step-dir-path]
   (let [feature-dir (io/file feature-dir-path)
         step-defs (load-step-defs step-dir-path)]
     (if (.exists feature-dir)
       (let [results (->> (.listFiles feature-dir)
                          (filter #(str/ends-with? (.getName %) ".feature"))
                          (mapv (fn [file]
                                  (let [feature (gherkin/parse-feature (slurp file))]
                                    (gherkin/run-feature feature step-defs))))
                          flatten
                          vec)]
         (doseq [result results]
           (println (str (:scenario result) " - " (name (:status result)))))
         (if (every? #(= :passed (:status %)) results) 0 1))
       (do
         (println "No features directory found")
         1)))))

(defn -main [& args]
  (System/exit (run-features)))
