(ns braids.gherkin
  (:require [clojure.string :as str]))

(defn parse-feature [text]
  (let [lines (str/split-lines text)
        feature-line (first (filter #(str/starts-with? % "Feature:") lines))
        feature (str/trim (subs feature-line 8))
        background-lines (take-while #(not (str/starts-with? % "Scenario:")) 
                                     (drop-while #(not (str/starts-with? % "Background:")) lines))
        background-steps (parse-steps (rest background-lines))
        scenario-blocks (partition-by #(str/starts-with? % "Scenario:") 
                                      (drop-while #(not (str/starts-with? % "Scenario:")) lines))
        scenarios (for [[scenario-line & step-lines] (filter #(str/starts-with? (first %) "Scenario:") scenario-blocks)]
                    {:title (str/trim (subs scenario-line 9))
                     :steps (parse-steps step-lines)})]
    {:feature feature
     :background background-steps
     :scenarios scenarios}))

(defn parse-steps [lines]
  (for [line lines
        :let [trimmed (str/trim line)]
        :when (and (seq trimmed) 
                   (not (str/starts-with? trimmed "Scenario:")) 
                   (not (str/starts-with? trimmed "Background:")) 
                   (not (str/starts-with? trimmed "Feature:")))]
    (let [[keyword & text-parts] (str/split trimmed #" " 2)
          text (str/join " " text-parts)]
      {:keyword keyword :text text})))

(defn match-step [step step-defs]
  (let [full-text (str (:keyword step) " " (:text step))]
    (if-let [defn (get step-defs full-text)]
      defn
      (first (for [[pattern defn] step-defs
                   :when (and (instance? java.util.regex.Pattern pattern)
                              (re-matches pattern full-text))]
               defn)))))

(defn run-feature [feature step-defs]
  (for [scenario (:scenarios feature)]
    (try
      (doseq [step (:steps scenario)]
        (if-let [step-fn (match-step step step-defs)]
          (step-fn)
          (throw (Exception. (str "No step definition for: " (:keyword step) " " (:text step))))))
      {:scenario (:title scenario) :status :passed :steps []}
      (catch Exception e
        {:scenario (:title scenario) :status :failed :steps []}))))