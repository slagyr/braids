(ns braids.gherkin-runner
  (:require [braids.gherkin :as gherkin]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn load-step-defs []
  (let [step-dir (io/file "spec/step_defs")]
    (when (.exists step-dir)
      (apply merge
             (for [file (.listFiles step-dir)
                   :when (str/ends-with? (.getName file) ".clj")]
               (load-file (.getPath file)))))))

(defn run-features []
  (let [feature-dir (io/file "spec/features")
        step-defs (load-step-defs)]
    (if (.exists feature-dir)
      (let [results (for [file (.listFiles feature-dir)
                          :when (str/ends-with? (.getName file) ".feature")
                          :let [feature (gherkin/parse-feature (slurp file))
                                result (gherkin/run-feature feature step-defs)]]
                      result)]
        (doseq [result (flatten results)]
          (println (str (:scenario result) " - " (name (:status result)))))
        (let [all-passed? (every? #(= :passed (:status %)) (flatten results))]
          (System/exit (if all-passed? 0 1))))
      (do
        (println "No spec/features directory found")
        (System/exit 1)))))

(defn -main [& args]
  (run-features))