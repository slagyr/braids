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

;; --- Step registry: data-driven dispatch for text and code generation ---
;; Each IR :type maps to {:text (fn [node] ...) :code (fn [node] ...)}
;; :text produces human-readable step text for comments
;; :code produces executable Clojure code (or nil to skip)

(defn- project-config-text [{:keys [slug worker-timeout max-workers]}]
  (cond
    worker-timeout (str "a project \"" slug "\" with worker-timeout " worker-timeout)
    max-workers    (str "a project \"" slug "\" with max-workers " max-workers)
    :else          (str "a project \"" slug "\"")))

(defn- project-config-code [{:keys [slug worker-timeout max-workers]}]
  (cond
    worker-timeout (str "(h/add-project-config \"" slug "\" {:worker-timeout " worker-timeout "})")
    max-workers    (str "(h/add-project \"" slug "\" {:max-workers " max-workers "})")
    :else          nil))

(def ^:private step-registry
  {:unrecognized       {:text :text}
   ;; Zombie detection
   :session            {:text (fn [{:keys [session-id label]}]       (str "a session \"" session-id "\" with label \"" label "\""))
                        :code (fn [{:keys [session-id label]}]       (str "(h/add-session \"" session-id "\" {:label \"" label "\"})"))}
   :session-status     {:text (fn [{:keys [session-id status age-seconds]}] (str "session \"" session-id "\" has status \"" status "\" and age " age-seconds " seconds"))
                        :code (fn [{:keys [session-id status age-seconds]}] (str "(h/set-session-status \"" session-id "\" \"" status "\" " age-seconds ")"))}
   :bead-status        {:text (fn [{:keys [bead-id status]}]         (str "bead \"" bead-id "\" has status \"" status "\""))
                        :code (fn [{:keys [bead-id status]}]         (str "(h/set-bead-status \"" bead-id "\" \"" status "\")"))}
   :bead-no-status     {:text (fn [{:keys [bead-id]}]               (str "bead \"" bead-id "\" has no recorded status"))
                        :code (constantly nil)}
   :check-zombies      {:text (constantly "checking for zombies")
                        :code (constantly "(h/check-zombies!)")}
   :assert-zombie      {:text (fn [{:keys [session-id reason]}]     (str "session \"" session-id "\" should be a zombie with reason \"" reason "\""))
                        :code (fn [{:keys [session-id reason]}]     (str "(should (h/zombie? \"" session-id "\"))\n(should= \"" reason "\" (h/zombie-reason \"" session-id "\"))"))}
   :assert-no-zombies  {:text (constantly "no zombies should be detected")
                        :code (constantly "(should= [] (h/zombies))")}
   ;; Orch spawning
   :project-config     {:text project-config-text
                        :code project-config-code}
   :active-iteration   {:text (fn [{:keys [slug iteration]}]        (str "project \"" slug "\" has an active iteration \"" iteration "\""))
                        :code (fn [{:keys [slug iteration]}]        (str "(h/set-active-iteration \"" slug "\" \"" iteration "\")"))}
   :no-active-iteration {:text (fn [{:keys [slug]}]                 (str "project \"" slug "\" has no active iteration"))
                         :code (fn [{:keys [slug]}]                 (str "(h/remove-iteration \"" slug "\")"))}
   :ready-beads        {:text (fn [{:keys [slug count]}]            (str "project \"" slug "\" has " count " ready beads"))
                        :code (fn [{:keys [slug count]}]            (str "(h/set-ready-beads \"" slug "\" " count ")"))}
   :ready-bead-with-id {:text (fn [{:keys [slug bead-id]}]          (str "project \"" slug "\" has 1 ready bead with id \"" bead-id "\""))
                        :code (fn [{:keys [slug bead-id]}]          (str "(h/set-ready-bead-with-id \"" slug "\" \"" bead-id "\")"))}
   :active-workers     {:text (fn [{:keys [slug count]}]            (str "project \"" slug "\" has " count " active workers"))
                        :code (fn [{:keys [slug count]}]            (str "(h/set-active-workers \"" slug "\" " count ")"))}
   :orch-tick          {:text (constantly "the orchestrator ticks")
                        :code (constantly "(h/orch-tick!)")}
   :orch-tick-project  {:text (fn [{:keys [slug]}]                  (str "the orchestrator ticks for project \"" slug "\" only"))
                        :code (fn [{:keys [slug]}]                  (str "(h/orch-tick-project! \"" slug "\")"))}
   :assert-action      {:text (fn [{:keys [expected]}]              (str "the action should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/tick-action))"))}
   :assert-spawn-count {:text (fn [{:keys [count]}]                 (str count " workers should be spawned"))
                        :code (fn [{:keys [count]}]                 (str "(should= " count " (h/spawn-count))"))}
   :assert-idle-reason {:text (fn [{:keys [expected]}]              (str "the idle reason should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/idle-reason))"))}
   :assert-spawn-label {:text (fn [{:keys [expected]}]              (str "the spawn label should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/spawn-label))"))}
   ;; Worker session tracking
   :bead               {:text (fn [{:keys [bead-id]}]               (str "a bead with id \"" bead-id "\""))
                        :code (fn [{:keys [bead-id]}]               (str "(h/set-bead-id \"" bead-id "\")"))}
   :session-id-literal {:text (fn [{:keys [session-id]}]            (str "a session ID \"" session-id "\""))
                        :code (fn [{:keys [session-id]}]            (str "(h/set-session-id-literal \"" session-id "\")"))}
   :generate-session-id       {:text (constantly "generating the session ID")
                               :code (constantly "(h/generate-session-id!)")}
   :generate-session-id-twice {:text (constantly "generating the session ID twice")
                               :code (constantly "(h/generate-session-id-twice!)")}
   :generate-session-ids-both {:text (constantly "generating session IDs for both")
                               :code (constantly "(h/generate-session-ids-both!)")}
   :parse-session-id          {:text (constantly "parsing the session ID")
                               :code (constantly "(h/parse-session-id!)")}
   :assert-session-id  {:text (fn [{:keys [expected]}]              (str "the session ID should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/session-id-result))"))}
   :assert-ids-identical {:text (constantly "both session IDs should be identical")
                          :code (constantly "(should (h/session-ids-identical?))")}
   :assert-ids-different {:text (constantly "the session IDs should be different")
                          :code (constantly "(should (h/session-ids-different?))")}
   :assert-bead-id     {:text (fn [{:keys [expected]}]              (str "the extracted bead ID should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/parsed-bead-id))"))}})

;; --- Step text and code: thin dispatchers over the registry ---

(defn step-text
  "Convert a typed IR node to a human-readable step text string."
  [{:keys [type] :as node}]
  (let [entry (get step-registry type)
        text-fn (:text entry)]
    (cond
      (nil? entry)    (str node)
      (= :text text-fn) (get node :text)
      :else           (text-fn node))))

(defn- generate-step-code
  "Generate executable Clojure code for a typed IR step, or nil to skip."
  [{:keys [type] :as node}]
  (when-let [code-fn (:code (get step-registry type))]
    (code-fn node)))

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
