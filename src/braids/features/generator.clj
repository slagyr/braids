(ns braids.features.generator
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [braids.features.steps.zombie-detection :as zombie-detection]
            [braids.features.steps.orch-output :as orch-output]
            [braids.features.steps.orch-spawning :as orch-spawning]
            [braids.features.steps.worker-session :as worker-session]
            [braids.features.steps.project-lifecycle :as project-lifecycle]
            [braids.features.steps.iteration :as iteration]
            [braids.features.steps.ready-beads :as ready-beads]
            [braids.features.steps.project-listing :as project-listing]
            [braids.features.steps.configuration :as configuration]
            [braids.features.steps.project-status :as project-status]
            [braids.features.steps.orch-runner :as orch-runner]))

(defn source->ns-name
  "Convert a feature source filename to a Clojure namespace name."
  [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str/replace #"_" "-")
      (->> (str "braids.features."))
      (str "-spec")))

;; --- Step composition: merge per-domain patterns and registries ---

(defn- flatten-patterns
  "Flatten a domain's {:given [...] :when [...] :then [...]} pattern map
   into a single vector of [regex handler] pairs."
  [domain-patterns]
  (into [] (concat (:given domain-patterns)
                   (:when domain-patterns)
                   (:then domain-patterns))))

(defn- merge-patterns
  "Merge multiple domain pattern maps into a single flat vector of [regex handler] pairs."
  [& domain-pattern-maps]
  (into [] (mapcat flatten-patterns domain-pattern-maps)))

(def ^:private step-patterns
  "Composed step patterns from all domain files.
   Order matters -- first match wins."
  (merge-patterns
    zombie-detection/step-patterns
    orch-output/step-patterns
    orch-spawning/step-patterns
    worker-session/step-patterns
    project-lifecycle/step-patterns
    iteration/step-patterns
    ready-beads/step-patterns
    project-listing/step-patterns
    configuration/step-patterns
    project-status/step-patterns
    orch-runner/step-patterns))

(def ^:private step-registry
  "Composed step registry from all domain files."
  (merge
    {:unrecognized {:text :text}}
    zombie-detection/step-registry
    orch-spawning/step-registry
    worker-session/step-registry
    project-lifecycle/step-registry
    iteration/step-registry
    ready-beads/step-registry
    project-listing/step-registry
    configuration/step-registry
    project-status/step-registry
    orch-runner/step-registry
    orch-output/step-registry))

;; --- Step classification: pattern-match step text into typed IR nodes ---

(defn classify-step
  "Pattern-match step text into a typed IR node map, or {:pattern :unrecognized :text text}."
  [text]
  (or (some (fn [[pattern handler]]
              (when-let [match (re-matches pattern text)]
                (handler match)))
            step-patterns)
      {:pattern :unrecognized :text text}))

(defn- classify-node
  "Classify a step node. If it already has a :pattern key, return as-is.
   If it only has :text (new parser format), classify the text and merge."
  [node]
  (if (:pattern node)
    node
    (let [classified (classify-step (:text node))]
      (-> (merge classified (select-keys node [:type :table]))
          (assoc :text (:text node))))))

(defn- classify-steps
  "Classify all step nodes in a steps vector."
  [steps]
  (mapv classify-node steps))

;; --- Step text and code: thin dispatchers over the registry ---

(defn step-text
  "Convert a typed IR node to a human-readable step text string."
  [{:keys [pattern] :as node}]
  (let [entry (get step-registry pattern)
        text-fn (:text entry)]
    (cond
      (nil? entry)    (str node)
      (= :text text-fn) (get node :text)
      :else           (text-fn node))))

(defn- generate-step-code
  "Generate executable Clojure code for a typed IR step, or nil to skip."
  [{:keys [pattern] :as node}]
  (when-let [code-fn (:code (get step-registry pattern))]
    (code-fn node)))

(defn- all-recognized?
  "Returns true if all steps in a scenario (and optional background) are recognized patterns."
  [scenario background]
  (let [all-steps (concat (:steps background) (:steps scenario))]
    (every? #(not= :unrecognized (:pattern %)) all-steps)))

;; --- NS form generation ---

(defn generate-ns-form
  "Generate the ns declaration string for a feature spec."
  ([source] (generate-ns-form source false))
  ([source needs-harness?]
   (if needs-harness?
     (str "(ns " (source->ns-name source) "\n"
          "  (:require [speclj.core :refer :all]\n"
          "            [braids.features.harness :as h]))")
     (str "(ns " (source->ns-name source) "\n"
          "  (:require [speclj.core :refer :all]))"))))

;; --- Step comments (for pending scenarios) ---

(defn- step-keyword-label
  "Convert a step :type to its Gherkin keyword label for comments."
  [step-type]
  (case step-type
    :given "Given"
    :when  "When"
    :then  "Then"
    :and   "And"
    :but   "But"
    "Given"))

(defn generate-step-comments
  "Generate step comments for a scenario, optionally including background."
  [scenario background]
  (let [bg-steps (:steps background)
        scenario-steps (:steps scenario)
        bg-comments (when (seq bg-steps)
                      (let [bg-lines (map (fn [s]
                                            (str ";; " (step-keyword-label (:type s)) " " (step-text s)))
                                          bg-steps)]
                        (str ";; Background:\n" (str/join "\n" bg-lines) "\n;;")))
        step-lines (map (fn [s]
                          (str ";; " (step-keyword-label (:type s)) " " (step-text s)))
                        scenario-steps)
        parts (remove nil? [bg-comments (when (seq step-lines) (str/join "\n" step-lines))])]
    (str/join "\n" parts)))

;; --- Scenario generation ---

(defn- generate-executable-body
  "Generate the executable code body for a fully recognized scenario."
  [scenario background]
  (let [all-code (concat ["(h/reset!)"]
                         (map generate-step-code (:steps background))
                         (map generate-step-code (:steps scenario)))
        code-lines (->> all-code
                        (remove nil?)
                        (mapcat #(str/split-lines %)))]
    (->> code-lines
         (map #(str "      " %))
         (str/join "\n"))))

(defn- generate-pending-body
  "Generate the pending body with step comments for an unrecognized scenario."
  [scenario background]
  (let [step-comments (generate-step-comments scenario background)
        indented-comments (->> (str/split-lines step-comments)
                               (map #(str "      " %))
                               (str/join "\n"))]
    (str indented-comments "\n"
         "      (pending \"not yet implemented\")")))

(defn generate-scenario
  "Generate a (context ...) block for a scenario.
   Classifies text-only nodes before generating code.
   If all steps are recognized, generates executable code.
   Otherwise generates a pending block with step comments."
  [scenario background]
  (let [classified-scenario (update scenario :steps classify-steps)
        classified-background (when background (update background :steps classify-steps))
        title (:scenario classified-scenario)
        executable? (all-recognized? classified-scenario classified-background)
        body (if executable?
               (generate-executable-body classified-scenario classified-background)
               (generate-pending-body classified-scenario classified-background))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         body "))")))

;; --- Full spec generation ---

(defn- needs-harness?
  "Returns true if any non-wip scenario in the IR has all recognized steps."
  [ir]
  (let [{:keys [scenarios background]} ir
        non-wip (remove :wip scenarios)
        classified-bg (when background (update background :steps classify-steps))]
    (some #(all-recognized? (update % :steps classify-steps) classified-bg) non-wip)))

(defn generate-spec
  "Generate a complete speclj spec file string from an IR map."
  [ir]
  (let [{:keys [source feature scenarios background]} ir
        non-wip (remove :wip scenarios)
        harness? (needs-harness? ir)
        ns-form (generate-ns-form source harness?)
        scenario-blocks (->> non-wip
                             (map #(generate-scenario % background))
                             (str/join "\n\n"))]
    (str ns-form "\n\n"
         "(describe \"" feature "\"\n\n"
         scenario-blocks ")\n")))

(defn- source->spec-filename
  "Convert a source filename to a spec output filename."
  [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_spec.clj")))

(defn generate-features!
  "Read .edn IR files from edn-dir and write generated spec files to output-dir."
  [edn-dir output-dir]
  (let [dir (io/file edn-dir)
        edn-files (->> (.listFiles dir)
                       (filter #(str/ends-with? (.getName %) ".edn"))
                       (sort-by #(.getName %)))]
    (io/make-parents (io/file output-dir "dummy"))
    (doseq [f edn-files]
      (let [ir (edn/read-string (slurp f))
            out-name (source->spec-filename (:source ir))
            out-path (str output-dir "/" out-name)
            spec-str (generate-spec ir)]
        (println (str "Generating " out-path " from " (.getName f)))
        (spit out-path spec-str)
        (println (str "  " (count (remove :wip (:scenarios ir))) " scenarios generated"))))))
