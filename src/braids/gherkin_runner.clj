(ns braids.gherkin-runner
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

;; --- Legacy parser (old IR format for running features) ---

(defn- parse-steps [lines]
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

(defn parse-feature-legacy [text]
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

;; --- Step definition compilation and matching ---

(defn- compile-step-defs
  [step-defs]
  (into {}
    (map (fn [[k v]]
           (if (instance? java.util.regex.Pattern k)
             [k v]
             [(re-pattern k) v]))
         step-defs)))

(defn- match-step
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

;; --- Step execution ---

(defn- execute-step
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
  [feature step-defs]
  (let [bg-steps (or (:background feature) [])]
    (mapv (fn [scenario]
            (let [all-steps (into (vec bg-steps) (:steps scenario))
                  result (run-steps all-steps step-defs)]
              (assoc result :scenario (:title scenario))))
          (:scenarios feature))))

;; --- Loading step defs ---

(defn- deref-if-var
  [x]
  (if (var? x) @x x))

(defn load-step-defs
  ([] (load-step-defs "spec/step_defs"))
  ([step-dir-path]
   (let [step-dir (io/file step-dir-path)]
     (when (.exists step-dir)
       (->> (.listFiles step-dir)
            (filter #(str/ends-with? (.getName %) ".clj"))
            (map #(deref-if-var (load-file (.getPath %))))
            (apply merge)
            compile-step-defs)))))

;; --- Public API ---

(defn run-features
  ([] (run-features "features" "spec/step_defs"))
  ([feature-dir-path step-dir-path]
   (let [feature-dir (io/file feature-dir-path)
         step-defs (load-step-defs step-dir-path)]
     (if (.exists feature-dir)
       (let [results (->> (.listFiles feature-dir)
                          (filter #(str/ends-with? (.getName %) ".feature"))
                          (mapv (fn [file]
                                  (let [feature (parse-feature-legacy (slurp file))]
                                    (run-feature feature step-defs))))
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
