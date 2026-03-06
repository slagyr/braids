(ns braids.gherkin-generator
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn source->ns-name
  "Convert a feature source filename to a Clojure namespace name."
  [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str/replace #"_" "-")
      (->> (str "braids.features."))
      (str "-spec")))

;; --- Step text: convert IR node back to readable text for comments ---

(defn step-text
  "Convert a typed IR node to a human-readable step text string."
  [{:keys [type] :as node}]
  (case type
    :unrecognized       (:text node)
    ;; Zombie detection
    :session            (str "a session \"" (:session-id node) "\" with label \"" (:label node) "\"")
    :session-status     (str "session \"" (:session-id node) "\" has status \"" (:status node) "\" and age " (:age-seconds node) " seconds")
    :bead-status        (str "bead \"" (:bead-id node) "\" has status \"" (:status node) "\"")
    :bead-no-status     (str "bead \"" (:bead-id node) "\" has no recorded status")
    :check-zombies      "checking for zombies"
    :assert-zombie      (str "session \"" (:session-id node) "\" should be a zombie with reason \"" (:reason node) "\"")
    :assert-no-zombies  "no zombies should be detected"
    ;; Orch spawning (shared :project-config handles both worker-timeout and max-workers)
    :project-config     (cond
                          (:worker-timeout node) (str "a project \"" (:slug node) "\" with worker-timeout " (:worker-timeout node))
                          (:max-workers node)    (str "a project \"" (:slug node) "\" with max-workers " (:max-workers node))
                          :else                  (str "a project \"" (:slug node) "\""))
    :active-iteration   (str "project \"" (:slug node) "\" has an active iteration \"" (:iteration node) "\"")
    :no-active-iteration (str "project \"" (:slug node) "\" has no active iteration")
    :ready-beads        (str "project \"" (:slug node) "\" has " (:count node) " ready beads")
    :ready-bead-with-id (str "project \"" (:slug node) "\" has 1 ready bead with id \"" (:bead-id node) "\"")
    :active-workers     (str "project \"" (:slug node) "\" has " (:count node) " active workers")
    :orch-tick          "the orchestrator ticks"
    :orch-tick-project  (str "the orchestrator ticks for project \"" (:slug node) "\" only")
    :assert-action      (str "the action should be \"" (:expected node) "\"")
    :assert-spawn-count (str (:count node) " workers should be spawned")
    :assert-idle-reason (str "the idle reason should be \"" (:expected node) "\"")
    :assert-spawn-label (str "the spawn label should be \"" (:expected node) "\"")
    (str node)))

;; --- Code generation: emit executable Clojure code per step type ---

(defn- generate-step-code
  "Generate a single line of executable Clojure code for a typed IR step.
   Returns nil for types that require no action (e.g., :bead-no-status)."
  [{:keys [type] :as node}]
  (case type
    ;; Zombie detection
    :session            (str "(h/add-session \"" (:session-id node) "\" {:label \"" (:label node) "\"})")
    :session-status     (str "(h/set-session-status \"" (:session-id node) "\" \"" (:status node) "\" " (:age-seconds node) ")")
    :bead-status        (str "(h/set-bead-status \"" (:bead-id node) "\" \"" (:status node) "\")")
    :bead-no-status     nil
    :check-zombies      "(h/check-zombies!)"
    :assert-zombie      (str "(should (h/zombie? \"" (:session-id node) "\"))\n"
                             "(should= \"" (:reason node) "\" (h/zombie-reason \"" (:session-id node) "\"))")
    :assert-no-zombies  "(should= [] (h/zombies))"
    ;; Orch spawning (shared :project-config handles both domains)
    :project-config     (cond
                          (:worker-timeout node) (str "(h/add-project-config \"" (:slug node) "\" {:worker-timeout " (:worker-timeout node) "})")
                          (:max-workers node)    (str "(h/add-project \"" (:slug node) "\" {:max-workers " (:max-workers node) "})")
                          :else                  nil)
    :active-iteration   (str "(h/set-active-iteration \"" (:slug node) "\" \"" (:iteration node) "\")")
    :no-active-iteration (str "(h/remove-iteration \"" (:slug node) "\")")
    :ready-beads        (str "(h/set-ready-beads \"" (:slug node) "\" " (:count node) ")")
    :ready-bead-with-id (str "(h/set-ready-bead-with-id \"" (:slug node) "\" \"" (:bead-id node) "\")")
    :active-workers     (str "(h/set-active-workers \"" (:slug node) "\" " (:count node) ")")
    :orch-tick          "(h/orch-tick!)"
    :orch-tick-project  (str "(h/orch-tick-project! \"" (:slug node) "\")")
    :assert-action      (str "(should= \"" (:expected node) "\" (h/tick-action))")
    :assert-spawn-count (str "(should= " (:count node) " (h/spawn-count))")
    :assert-idle-reason (str "(should= \"" (:expected node) "\" (h/idle-reason))")
    :assert-spawn-label (str "(should= \"" (:expected node) "\" (h/spawn-label))")
    nil))

(defn- all-recognized?
  "Returns true if all steps in a scenario (and optional background) are recognized types."
  [scenario background]
  (let [all-steps (concat (:givens background)
                          (:givens scenario)
                          (:whens scenario)
                          (:thens scenario))]
    (every? #(not= :unrecognized (:type %)) all-steps)))

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

(defn- format-steps
  "Format a sequence of IR step nodes as comments with Given/When/Then prefixes."
  [keyword steps]
  (when (seq steps)
    (let [texts (map step-text steps)
          first-line (str ";; " keyword " " (first texts))
          rest-lines (map #(str ";; And " %) (rest texts))]
      (str/join "\n" (cons first-line rest-lines)))))

(defn generate-step-comments
  "Generate step comments for a scenario, optionally including background."
  [scenario background]
  (let [bg-comments (when background
                      (let [bg-header ";; Background:"
                            bg-steps (format-steps "Given" (:givens background))]
                        (str bg-header "\n" bg-steps "\n;;")))
        given-comments (format-steps "Given" (:givens scenario))
        when-comments (format-steps "When" (:whens scenario))
        then-comments (format-steps "Then" (:thens scenario))
        parts (remove nil? [bg-comments given-comments when-comments then-comments])]
    (str/join "\n" parts)))

;; --- Scenario generation ---

(defn- generate-executable-body
  "Generate the executable code body for a fully recognized scenario."
  [scenario background]
  (let [all-steps (concat [[:reset "(h/reset!)"]]
                          (map (fn [s] [:bg (generate-step-code s)]) (:givens background))
                          (map (fn [s] [:given (generate-step-code s)]) (:givens scenario))
                          (map (fn [s] [:when (generate-step-code s)]) (:whens scenario))
                          (map (fn [s] [:then (generate-step-code s)]) (:thens scenario)))
        code-lines (->> all-steps
                        (map second)
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
   If all steps are recognized, generates executable code.
   Otherwise generates a pending block with step comments."
  [scenario background]
  (let [title (:scenario scenario)
        executable? (all-recognized? scenario background)
        body (if executable?
               (generate-executable-body scenario background)
               (generate-pending-body scenario background))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         body "))")))

;; --- Full spec generation ---

(defn- needs-harness?
  "Returns true if any non-wip scenario in the IR has all recognized steps."
  [ir]
  (let [{:keys [scenarios background]} ir
        non-wip (remove :wip scenarios)]
    (some #(all-recognized? % background) non-wip)))

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
