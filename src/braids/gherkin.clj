(ns braids.gherkin
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private step-keywords #{"Given" "When" "Then" "And" "But"})

(defn- step-keyword? [trimmed]
  (some #(str/starts-with? trimmed (str % " ")) step-keywords))

(defn- strip-keyword [trimmed]
  (let [space-idx (str/index-of trimmed " ")]
    (when space-idx
      (subs trimmed (inc space-idx)))))

(defn- phase-for-keyword [trimmed]
  (cond
    (str/starts-with? trimmed "Given ") :givens
    (str/starts-with? trimmed "When ")  :whens
    (str/starts-with? trimmed "Then ")  :thens
    :else                               nil))

(defn- add-step [scenario phase text]
  (update scenario phase (fnil conj []) text))

(defn- process-step [state trimmed]
  (let [phase (phase-for-keyword trimmed)]
    (if phase
      (-> state
          (assoc :current-phase phase)
          (update :scenario add-step phase (strip-keyword trimmed)))
      ;; And/But — append to current phase
      (let [text (strip-keyword trimmed)]
        (update state :scenario add-step (:current-phase state) text)))))

(defn- parse-scenario-lines [lines]
  (let [result (reduce process-step
                       {:scenario {:givens [] :whens [] :thens []}
                        :current-phase nil}
                       lines)]
    (:scenario result)))

(defn- tag-line? [trimmed]
  (str/starts-with? trimmed "@"))

(defn- has-wip-tag? [trimmed]
  (some #(= "@wip" %) (str/split trimmed #"\s+")))

(defn- parse-sections
  "Splits feature lines into sections: feature line, description, background, and scenarios."
  [lines]
  (loop [lines lines
         state :start
         wip-pending false
         result {:feature-line nil
                 :description-lines []
                 :background-lines []
                 :scenarios []}]
    (if (empty? lines)
      ;; Finalize
      result
      (let [line (first lines)
            trimmed (str/trim line)
            rest-lines (rest lines)]
        (cond
          ;; Feature line
          (and (= state :start) (str/starts-with? trimmed "Feature:"))
          (recur rest-lines :description false
                 (assoc result :feature-line trimmed))

          ;; Skip blank lines in description
          (and (= state :description) (str/blank? trimmed))
          (recur rest-lines :description false result)

          ;; Background section start
          (str/starts-with? trimmed "Background:")
          (recur rest-lines :background false result)

          ;; Tag line — set wip flag for next scenario
          (tag-line? trimmed)
          (recur rest-lines state (has-wip-tag? trimmed) result)

          ;; Scenario start
          (str/starts-with? trimmed "Scenario:")
          (let [title (str/trim (subs trimmed 9))
                scenario-entry {:title title :lines [] :wip wip-pending}]
            (recur rest-lines :scenario false
                   (update result :scenarios conj scenario-entry)))

          ;; Step line in background
          (and (= state :background) (step-keyword? trimmed))
          (recur rest-lines :background false
                 (update result :background-lines conj trimmed))

          ;; Step line in scenario
          (and (= state :scenario) (step-keyword? trimmed))
          (let [scenarios (:scenarios result)
                current (peek scenarios)
                updated (update current :lines conj trimmed)]
            (recur rest-lines :scenario false
                   (assoc result :scenarios
                          (conj (pop scenarios) updated))))

          ;; Description text (non-blank, non-keyword lines before Background/Scenario)
          (and (= state :description) (seq trimmed))
          (recur rest-lines :description false
                 (update result :description-lines conj trimmed))

          ;; Skip anything else
          :else
          (recur rest-lines state wip-pending result))))))

(defn parse-feature
  "Parse a Gherkin feature string into an EDN IR map."
  [text]
  (let [lines (str/split-lines text)
        sections (parse-sections lines)
        feature-name (str/trim (subs (:feature-line sections) 8))
        description-lines (:description-lines sections)
        background-lines (:background-lines sections)
        scenarios (mapv (fn [{:keys [title lines wip]}]
                          (let [parsed (parse-scenario-lines lines)]
                            (cond-> (assoc parsed :scenario title)
                              wip (assoc :wip true))))
                        (:scenarios sections))]
    (cond-> {:feature feature-name
             :scenarios scenarios}
      (seq description-lines)
      (assoc :description (str/join "\n" description-lines))

      (seq background-lines)
      (assoc :background {:givens (mapv strip-keyword background-lines)}))))

(defn parse-feature-file
  "Parse a .feature file into an EDN IR map with :source."
  [path]
  (let [content (slurp path)
        filename (last (str/split path #"/"))]
    (assoc (parse-feature content) :source filename)))

(defn parse-features-dir
  "Parse all .feature files in a directory. Returns a seq of IR maps."
  [dir-path]
  (let [dir (io/file dir-path)]
    (->> (.listFiles dir)
         (filter #(str/ends-with? (.getName %) ".feature"))
         (sort-by #(.getName %))
         (mapv #(parse-feature-file (.getPath %))))))

(defn write-edn
  "Write an IR map to an .edn file."
  [path data]
  (spit path (pr-str data)))

(defn parse-features!
  "Parse all .feature files in source-dir and write .edn files to output-dir."
  [source-dir output-dir]
  (let [results (parse-features-dir source-dir)]
    (io/make-parents (io/file output-dir "dummy"))
    (doseq [result results]
      (let [base-name (str/replace (:source result) #"\.feature$" ".edn")
            edn-path (str output-dir "/" base-name)]
        (println (str "Parsing " (:source result) " -> " edn-path))
        (write-edn edn-path result)
        (println (str "  " (count (:scenarios result)) " scenarios parsed"))))))
