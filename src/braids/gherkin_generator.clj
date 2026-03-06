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
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/parsed-bead-id))"))}
   ;; Project lifecycle
   :bd-not-available      {:text (constantly "bd is not available")
                           :code (constantly "(h/set-bd-not-available)")}
   :bd-available          {:text (constantly "bd is available")
                           :code (constantly "(h/set-bd-available)")}
   :no-registry           {:text (constantly "no registry exists")
                           :code (constantly "(h/set-no-registry)")}
   :registry-exists       {:text (constantly "a registry already exists")
                           :code (constantly "(h/set-registry-exists)")}
   :force-not-set         {:text (constantly "force is not set")
                           :code (constantly "(h/set-force-not-set)")}
   :force-set             {:text (constantly "force is set")
                           :code (constantly "(h/set-force-set)")}
   :braids-dir-not-exists {:text (constantly "braids dir does not exist")
                           :code (constantly "(h/set-braids-dir-not-exists)")}
   :braids-dir-exists     {:text (constantly "braids dir already exists")
                           :code (constantly "(h/set-braids-dir-exists)")}
   :braids-home-not-exists {:text (constantly "braids home does not exist")
                            :code (constantly "(h/set-braids-home-not-exists)")}
   :braids-home-exists    {:text (constantly "braids home already exists")
                           :code (constantly "(h/set-braids-home-exists)")}
   :check-prerequisites   {:text (constantly "checking prerequisites")
                           :code (constantly "(h/check-prerequisites!)")}
   :plan-init             {:text (constantly "planning init")
                           :code (constantly "(h/plan-init!)")}
   :assert-prereq-fail    {:text (fn [{:keys [expected]}]              (str "prerequisites should fail with \"" expected "\""))
                           :code (fn [{:keys [expected]}]              (str "(should-not (empty? (h/prereq-errors)))\n(should (some #(clojure.string/includes? % \"" expected "\") (h/prereq-errors)))"))}
   :assert-prereq-pass    {:text (constantly "prerequisites should pass")
                           :code (constantly "(should (empty? (h/prereq-errors)))")}
   :assert-plan-include   {:text (fn [{:keys [action]}]               (str "the plan should include \"" action "\""))
                           :code (fn [{:keys [action]}]               (str "(should (some #{\"" action "\"} (h/plan-actions)))"))}
   :assert-plan-not-include {:text (fn [{:keys [action]}]             (str "the plan should not include \"" action "\""))
                             :code (fn [{:keys [action]}]             (str "(should-not (some #{\"" action "\"} (h/plan-actions)))"))}
   :new-project-slug      {:text (fn [{:keys [slug]}]                (str "a new project with slug \"" slug "\""))
                           :code (fn [{:keys [slug]}]                (str "(h/set-new-project-slug \"" slug "\")"))}
   :new-project-name      {:text (fn [{:keys [name]}]                (str "a new project with name \"" name "\""))
                           :code (fn [{:keys [name]}]                (str "(h/set-new-project-name \"" name "\")"))}
   :set-name              {:text (fn [{:keys [name]}]                (str "name \"" name "\""))
                           :code (fn [{:keys [name]}]                (str "(h/set-new-project-name \"" name "\")"))}
   :set-goal              {:text (fn [{:keys [goal]}]                (str "goal \"" goal "\""))
                           :code (fn [{:keys [goal]}]                (str "(h/set-new-project-goal \"" goal "\")"))}
   :registry-with-project {:text (fn [{:keys [slug]}]                (str "a registry with project \"" slug "\""))
                           :code (fn [{:keys [slug]}]                (str "(h/set-registry-with-project \"" slug "\")"))}
   :new-registry-entry    {:text (fn [{:keys [slug]}]                (str "a new registry entry with slug \"" slug "\""))
                           :code (fn [{:keys [slug]}]                (str "(h/set-new-registry-entry \"" slug "\")"))}
   :validate-new-project  {:text (constantly "validating new project params")
                           :code (constantly "(h/validate-new-project!)")}
   :add-to-registry       {:text (constantly "adding the entry to the registry")
                           :code (constantly "(h/add-to-registry!)")}
   :build-project-config  {:text (constantly "building the project config")
                           :code (constantly "(h/build-project-config!)")}
   :assert-validation-fail {:text (fn [{:keys [expected]}]            (str "validation should fail with \"" expected "\""))
                            :code (fn [{:keys [expected]}]            (str "(should-not (empty? (h/validation-errors)))\n(should (some #(clojure.string/includes? % \"" expected "\") (h/validation-errors)))"))}
   :assert-should-fail    {:text (fn [{:keys [expected]}]             (str "it should fail with \"" expected "\""))
                           :code (fn [{:keys [expected]}]             (str "(should (clojure.string/includes? (or (h/add-registry-error) \"\") \"" expected "\"))"))}
   :assert-config-value   {:text (fn [{:keys [key expected]}]         (str "the config " key " should be \"" expected "\""))
                           :code (fn [{:keys [key expected]}]         (str "(should= " (if (re-matches #"^:.*" expected) expected (str ":" expected)) " (:" key " (h/project-config)))"))}
   :assert-config-number  {:text (fn [{:keys [key expected]}]         (str "the config " key " should be " expected))
                            :code (fn [{:keys [key expected]}]         (str "(should= " expected " (:" key " (h/project-config)))"))}
   ;; Project listing
   :project-list-with-table {:text (constantly "a project list with the following projects:")
                             :code (fn [{:keys [table]}]
                                     (when table
                                       (let [{:keys [headers rows]} table]
                                         (str "(h/set-project-list-from-table\n"
                                              "  " (pr-str headers) "\n"
                                              "  " (pr-str rows) ")"))))}
   :empty-project-list    {:text (constantly "an empty project list")
                           :code (constantly "(h/set-empty-project-list)")}
   :format-list           {:text (constantly "formatting the project list")
                           :code (constantly "(h/format-list!)")}
   :format-list-json      {:text (constantly "formatting the project list as JSON")
                           :code (constantly "(h/format-list-json!)")}
   :assert-column-headers {:text (fn [{:keys [headers]}]              (str "the output should contain column headers"))
                           :code (fn [{:keys [headers]}]
                                   (let [header-strs (mapv second headers)]
                                     (str/join "\n" (map #(str "(should (clojure.string/includes? (h/list-output) \"" % "\"))") header-strs))))}
   :assert-output-contains-slug {:text (fn [{:keys [slug]}]           (str "the output should contain slug \"" slug "\""))
                                 :code (fn [{:keys [slug]}]           (str "(should (clojure.string/includes? (h/list-output) \"" slug "\"))"))}
   :assert-output-contains-iteration {:text (fn [{:keys [iteration]}] (str "the output should contain iteration \"" iteration "\""))
                                      :code (fn [{:keys [iteration]}] (str "(should (clojure.string/includes? (h/list-output) \"" iteration "\"))"))}
   :assert-output-contains-progress {:text (fn [{:keys [progress]}]   (str "the output should contain progress \"" progress "\""))
                                     :code (fn [{:keys [progress]}]   (str "(should (clojure.string/includes? (h/list-output) \"" progress "\"))"))}
   :assert-output-contains-workers {:text (fn [{:keys [workers]}]     (str "the output should contain workers \"" workers "\""))
                                    :code (fn [{:keys [workers]}]     (str "(should (clojure.string/includes? (h/list-output) \"" workers "\"))"))}
   :assert-dash-placeholder {:text (fn [{:keys [slug field]}]         (str "the line for \"" slug "\" should contain a dash for " field))
                             :code (fn [{:keys [slug]}]               (str "(should (h/line-contains-dash? \"" slug "\"))"))}
   :assert-output-equals  {:text (fn [{:keys [expected]}]             (str "the output should be \"" expected "\""))
                           :code (fn [{:keys [expected]}]             (str "(should= \"" expected "\" (h/list-output))"))}
   :assert-status-color   {:text (fn [{:keys [status color]}]        (str "\"" status "\" status should be colorized " color))
                           :code (fn [{:keys [status color]}]        (str "(should (h/colorized? (h/list-output) \"" status "\" \"" color "\"))"))}
   :assert-priority-color {:text (fn [{:keys [priority color]}]      (str "\"" priority "\" priority should be colorized " color))
                           :code (fn [{:keys [priority color]}]      (str "(should (h/colorized? (h/list-output) \"" priority "\" \"" color "\"))"))}
   :assert-progress-color {:text (fn [{:keys [percent color]}]       (str percent " percent progress should be colorized " color))
                           :code (fn [{:keys [percent color]}]       (str "(should (h/colorized? (h/list-output) \"" percent "%\" \"" color "\"))"))}
   :assert-json-project-exists {:text (fn [{:keys [slug]}]           (str "the JSON output should contain a project with slug \"" slug "\""))
                                :code (fn [{:keys [slug]}]           (str "(should (h/json-project \"" slug "\"))"))}
   :assert-json-project-string {:text (fn [{:keys [slug key expected]}] (str "the JSON project \"" slug "\" should have " key " \"" expected "\""))
                                :code (fn [{:keys [slug key expected]}] (str "(should= \"" expected "\" (get (h/json-project \"" slug "\") \"" key "\"))"))}
   :assert-json-project-number {:text (fn [{:keys [slug key expected]}] (str "the JSON project \"" slug "\" should have " key " " expected))
                                :code (fn [{:keys [slug key expected]}] (str "(should= " expected " (get (h/json-project \"" slug "\") \"" key "\"))"))}
   :assert-json-iteration-number {:text (fn [{:keys [slug number]}]  (str "the JSON project \"" slug "\" should have iteration number \"" number "\""))
                                  :code (fn [{:keys [slug number]}]  (str "(should= \"" number "\" (get-in (h/json-project \"" slug "\") [\"iteration\" \"number\"]))"))}
   })

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
