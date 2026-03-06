(ns braids.gherkin
  (:require [clojure.string :as str]))

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

(defn- split-scenarios [lines]
  (let [scenario-lines (drop-while #(not (str/starts-with? (str/trim %) "Scenario:")) lines)]
    (when (seq scenario-lines)
      (let [groups (reduce (fn [acc line]
                             (if (str/starts-with? (str/trim line) "Scenario:")
                               (conj acc [line])
                               (update acc (dec (count acc)) conj line)))
                           [] scenario-lines)]
        (for [[scenario-line & step-lines] groups]
          {:title (str/trim (subs (str/trim scenario-line) 9))
           :steps (parse-steps (or step-lines []))})))))

(defn parse-feature [text]
  (let [lines (str/split-lines text)
        feature-line (first (filter #(str/starts-with? (str/trim %) "Feature:") lines))
        feature (str/trim (subs (str/trim feature-line) 8))
        has-background? (some #(str/starts-with? (str/trim %) "Background:") lines)
        background-lines (when has-background?
                           (take-while #(not (str/starts-with? (str/trim %) "Scenario:"))
                                       (rest (drop-while #(not (str/starts-with? (str/trim %) "Background:")) lines))))
        background-steps (when has-background? (vec (parse-steps (or background-lines []))))
        scenarios (vec (or (split-scenarios lines) []))]
    (cond-> {:feature feature :scenarios scenarios}
      has-background? (assoc :background background-steps))))

(defn match-step [step step-defs]
  (let [full-text (str (:keyword step) " " (:text step))]
    (if-let [f (get step-defs full-text)]
      f
      (first (for [[pattern f] step-defs
                   :when (instance? java.util.regex.Pattern pattern)
                   :let [m (re-matches pattern full-text)]
                   :when m]
               (let [groups (if (string? m) [] (vec (rest m)))]
                 (fn [] (apply f groups))))))))

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