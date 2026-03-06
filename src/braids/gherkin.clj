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

;; Bug 2: Compile string keys to regex Patterns for cucumber-style step defs.
;; String keys like "Given (\\d+) beads" are compiled to Pattern.
;; Pattern keys pass through unchanged.

(defn compile-step-defs
  "Compile step definition keys: strings become regex Patterns, Patterns pass through."
  [step-defs]
  (into {}
    (map (fn [[k v]]
           (if (instance? java.util.regex.Pattern k)
             [k v]
             [(re-pattern k) v]))
         step-defs)))

(defn match-step
  "Match a parsed step against step definitions.
   Tries exact string match first, then regex Pattern match.
   For Pattern matches, capture groups are passed as args to the step fn."
  [step step-defs]
  (let [full-text (str (:keyword step) " " (:text step))]
    (if-let [f (get step-defs full-text)]
      f
      (first (for [[pattern f] step-defs
                   :when (instance? java.util.regex.Pattern pattern)
                   :let [m (re-matches pattern full-text)]
                   :when m]
               (let [groups (if (string? m) [] (vec (rest m)))]
                 (fn [] (apply f groups))))))))

(defn- execute-step
  "Execute a single step. Returns {:step name :status :passed/:failed}."
  [step step-defs]
  (let [step-name (str (:keyword step) " " (:text step))]
    (if-let [step-fn (match-step step step-defs)]
      (try
        (step-fn)
        {:step step-name :status :passed}
        (catch Exception e
          {:step step-name :status :failed :error (.getMessage e)}))
      {:step step-name :status :failed :error (str "No step definition for: " step-name)})))

(defn- run-steps
  "Run a sequence of steps, recording pass/fail for each.
   Stops on first failure. Returns {:status :passed/:failed, :steps [...]}."
  [steps step-defs]
  (let [result (reduce (fn [results step]
                         (let [step-result (execute-step step step-defs)]
                           (if (= :failed (:status step-result))
                             (reduced (conj results step-result))
                             (conj results step-result))))
                       [] steps)
        failed? (= :failed (:status (peek result)))]
    {:status (if failed? :failed :passed)
     :steps result}))

(defn run-feature
  "Run all scenarios in a feature against step definitions.
   Prepends background steps to each scenario. Returns a vector of result maps."
  [feature step-defs]
  (let [bg-steps (or (:background feature) [])]
    (mapv (fn [scenario]
            (let [all-steps (into (vec bg-steps) (:steps scenario))
                  result (run-steps all-steps step-defs)]
              (assoc result :scenario (:title scenario))))
          (:scenarios feature))))
