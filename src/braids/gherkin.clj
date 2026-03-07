(ns braids.gherkin
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [braids.edn-format :refer [edn-format]]))

;; --- Step classification: pattern-match step text into typed IR nodes ---

(def ^:private step-patterns
  "Ordered list of [regex handler-fn] pairs for classifying step text.
   First match wins. Handler receives regex match groups."
  [[#"^a project \"([^\"]+)\" with worker-timeout (\d+)$"
    (fn [[_ slug timeout]]
      {:pattern :project-config :slug slug :worker-timeout (parse-long timeout)})]

   [#"^a session \"([^\"]+)\" with label \"([^\"]+)\"$"
    (fn [[_ session-id label]]
      {:pattern :session :session-id session-id :label label})]

   [#"^session \"([^\"]+)\" has status \"([^\"]+)\" and age (\d+) seconds$"
    (fn [[_ session-id status age]]
      {:pattern :session-status :session-id session-id :status status :age-seconds (parse-long age)})]

   [#"^bead \"([^\"]+)\" has status \"([^\"]+)\"$"
    (fn [[_ bead-id status]]
      {:pattern :bead-status :bead-id bead-id :status status})]

   [#"^bead \"([^\"]+)\" has no recorded status$"
    (fn [[_ bead-id]]
      {:pattern :bead-no-status :bead-id bead-id})]

   [#"^checking for zombies$"
    (fn [_] {:pattern :check-zombies})]

   [#"^session \"([^\"]+)\" should be a zombie with reason \"([^\"]+)\"$"
    (fn [[_ session-id reason]]
      {:pattern :assert-zombie :session-id session-id :reason reason})]

   [#"^no zombies should be detected$"
    (fn [_] {:pattern :assert-no-zombies})]

   ;; --- Orch spawning patterns ---

   [#"^a project \"([^\"]+)\" with max-workers (\d+)$"
    (fn [[_ slug max-w]]
      {:pattern :project-config :slug slug :max-workers (parse-long max-w)})]

   [#"^project \"([^\"]+)\" has an active iteration \"([^\"]+)\"$"
    (fn [[_ slug iteration]]
      {:pattern :active-iteration :slug slug :iteration iteration})]

   [#"^project \"([^\"]+)\" has no active iteration$"
    (fn [[_ slug]]
      {:pattern :no-active-iteration :slug slug})]

   [#"^project \"([^\"]+)\" has (\d+) ready beads? with id \"([^\"]+)\"$"
    (fn [[_ slug _count bead-id]]
      {:pattern :ready-bead-with-id :slug slug :bead-id bead-id})]

   [#"^project \"([^\"]+)\" has (\d+) ready beads?$"
    (fn [[_ slug count]]
      {:pattern :ready-beads :slug slug :count (parse-long count)})]

   [#"^project \"([^\"]+)\" has (\d+) active workers?$"
    (fn [[_ slug count]]
      {:pattern :active-workers :slug slug :count (parse-long count)})]

   [#"^the orchestrator ticks$"
    (fn [_] {:pattern :orch-tick})]

   [#"^the orchestrator ticks for project \"([^\"]+)\" only$"
    (fn [[_ slug]]
      {:pattern :orch-tick-project :slug slug})]

   [#"^the action should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-action :expected expected})]

   [#"^(\d+) workers? should be spawned$"
    (fn [[_ count]]
      {:pattern :assert-spawn-count :count (parse-long count)})]

   [#"^the idle reason should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-idle-reason :expected expected})]

   [#"^the spawn label should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-spawn-label :expected expected})]

   ;; --- Worker session tracking patterns ---

   [#"^a bead with id \"([^\"]+)\"$"
    (fn [[_ bead-id]]
      {:pattern :bead :bead-id bead-id})]

   [#"^another bead with id \"([^\"]+)\"$"
    (fn [[_ bead-id]]
      {:pattern :bead :bead-id bead-id})]

   [#"^a session ID \"([^\"]+)\"$"
    (fn [[_ session-id]]
      {:pattern :session-id-literal :session-id session-id})]

   [#"^generating the session ID twice$"
    (fn [_] {:pattern :generate-session-id-twice})]

   [#"^generating the session ID$"
    (fn [_] {:pattern :generate-session-id})]

   [#"^generating session IDs for both$"
    (fn [_] {:pattern :generate-session-ids-both})]

   [#"^parsing the session ID$"
    (fn [_] {:pattern :parse-session-id})]

   [#"^the session ID should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-session-id :expected expected})]

   [#"^both session IDs should be identical$"
    (fn [_] {:pattern :assert-ids-identical})]

   [#"^the session IDs should be different$"
    (fn [_] {:pattern :assert-ids-different})]

    [#"^the extracted bead ID should be \"([^\"]+)\"$"
     (fn [[_ expected]]
       {:pattern :assert-bead-id :expected expected})]

   ;; --- Project lifecycle patterns ---

   [#"^bd is not available$"
    (fn [_] {:pattern :bd-not-available})]

   [#"^bd is available$"
    (fn [_] {:pattern :bd-available})]

   [#"^no registry exists$"
    (fn [_] {:pattern :no-registry})]

   [#"^a registry already exists$"
    (fn [_] {:pattern :registry-exists})]

   [#"^force is not set$"
    (fn [_] {:pattern :force-not-set})]

   [#"^force is set$"
    (fn [_] {:pattern :force-set})]

   [#"^braids dir does not exist$"
    (fn [_] {:pattern :braids-dir-not-exists})]

   [#"^braids dir already exists$"
    (fn [_] {:pattern :braids-dir-exists})]

   [#"^braids home does not exist$"
    (fn [_] {:pattern :braids-home-not-exists})]

   [#"^braids home already exists$"
    (fn [_] {:pattern :braids-home-exists})]

   [#"^checking prerequisites$"
    (fn [_] {:pattern :check-prerequisites})]

   [#"^planning init$"
    (fn [_] {:pattern :plan-init})]

   [#"^prerequisites should fail with \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-prereq-fail :expected expected})]

   [#"^prerequisites should pass$"
    (fn [_] {:pattern :assert-prereq-pass})]

   [#"^the plan should include \"([^\"]+)\"$"
    (fn [[_ action]]
      {:pattern :assert-plan-include :action action})]

   [#"^the plan should not include \"([^\"]+)\"$"
    (fn [[_ action]]
      {:pattern :assert-plan-not-include :action action})]

   [#"^a new project with slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:pattern :new-project-slug :slug slug})]

   [#"^a new project with name \"([^\"]+)\"$"
    (fn [[_ name]]
      {:pattern :new-project-name :name name})]

   [#"^name \"([^\"]+)\"$"
    (fn [[_ name]]
      {:pattern :set-name :name name})]

   [#"^goal \"([^\"]+)\"$"
    (fn [[_ goal]]
      {:pattern :set-goal :goal goal})]

   [#"^a registry with project \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:pattern :registry-with-project :slug slug})]

   [#"^a new registry entry with slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:pattern :new-registry-entry :slug slug})]

   [#"^validating new project params$"
    (fn [_] {:pattern :validate-new-project})]

   [#"^adding the entry to the registry$"
    (fn [_] {:pattern :add-to-registry})]

   [#"^building the project config$"
    (fn [_] {:pattern :build-project-config})]

   [#"^validation should fail with \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-validation-fail :expected expected})]

   [#"^it should fail with \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-should-fail :expected expected})]

   [#"^the config (\S+) should be \"([^\"]+)\"$"
    (fn [[_ key expected]]
      {:pattern :assert-config-value :key key :expected expected})]

   [#"^the config (\S+) should be (\d+)$"
    (fn [[_ key expected]]
      {:pattern :assert-config-number :key key :expected (parse-long expected)})]

     ;; --- Iteration management step patterns ---

     [#"^iteration EDN with number \"([^\"]+)\" and status \"([^\"]+)\" and (\d+) stor(?:y|ies)$"
      (fn [[_ number status count]]
        {:pattern :iteration-edn :number number :status status :story-count (parse-long count)})]

     [#"^the EDN has no guardrails or notes$"
      (fn [_] {:pattern :edn-no-guardrails-or-notes})]

     [#"^an iteration with number \"([^\"]+)\" and status \"([^\"]+)\" and stories$"
      (fn [[_ number status]]
        {:pattern :iteration-with-status :number number :status status})]

     [#"^an iteration with no number$"
      (fn [_] {:pattern :iteration-no-number})]

     [#"^an iteration with stories \"([^\"]+)\" and \"([^\"]+)\"$"
      (fn [[_ id1 id2]]
        {:pattern :iteration-with-stories :story-ids [id1 id2]})]

     [#"^an iteration with story \"([^\"]+)\"$"
      (fn [[_ story-id]]
        {:pattern :iteration-with-story :story-id story-id})]

     [#"^bead \"([^\"]+)\" has status \"([^\"]+)\" and priority (\d+)$"
      (fn [[_ bead-id status priority]]
        {:pattern :iter-bead-status :bead-id bead-id :status status :priority (parse-long priority)})]

     [#"^no bead data exists$"
      (fn [_] {:pattern :no-bead-data})]

     [#"^annotated stories with (\d+) closed and (\d+) open out of (\d+) total$"
      (fn [[_ closed open total]]
        {:pattern :annotated-stories :closed (parse-long closed) :open (parse-long open) :total (parse-long total)})]

     [#"^an iteration with no stories$"
      (fn [_] {:pattern :iteration-no-stories})]

     [#"^an iteration \"([^\"]+)\" with status \"([^\"]+)\"$"
      (fn [[_ number status]]
        {:pattern :iteration-number-status :number number :status status})]

     [#"^a story \"([^\"]+)\" with status \"([^\"]+)\"$"
      (fn [[_ story-id status]]
        {:pattern :story-with-status :story-id story-id :status status})]

     [#"^completion stats of (\d+) closed out of (\d+)$"
      (fn [[_ closed total]]
        {:pattern :completion-stats :closed (parse-long closed) :total (parse-long total)})]

     [#"^parsing the iteration EDN$"
      (fn [_] {:pattern :parse-iteration-edn})]

     [#"^validating the iteration$"
      (fn [_] {:pattern :validate-iteration})]

     [#"^annotating stories with bead data$"
      (fn [_] {:pattern :annotate-stories})]

     [#"^calculating completion stats$"
      (fn [_] {:pattern :calculate-completion-stats})]

     [#"^formatting the iteration$"
      (fn [_] {:pattern :format-iteration})]

     [#"^formatting the iteration as JSON$"
      (fn [_] {:pattern :format-iteration-json})]

     [#"^the iteration number should be \"([^\"]+)\"$"
      (fn [[_ expected]]
        {:pattern :assert-iteration-number :expected expected})]

     [#"^the iteration status should be \"([^\"]+)\"$"
      (fn [[_ expected]]
        {:pattern :assert-iteration-status :expected expected})]

     [#"^the iteration guardrails should be empty$"
      (fn [_] {:pattern :assert-iteration-guardrails-empty})]

     [#"^the iteration notes should be empty$"
      (fn [_] {:pattern :assert-iteration-notes-empty})]

     [#"^story \"([^\"]+)\" should have status \"([^\"]+)\"$"
      (fn [[_ story-id expected]]
        {:pattern :assert-story-status :story-id story-id :expected expected})]

     [#"^the total should be (\d+)$"
      (fn [[_ expected]]
        {:pattern :assert-total :expected (parse-long expected)})]

     [#"^the closed count should be (\d+)$"
      (fn [[_ expected]]
        {:pattern :assert-closed-count :expected (parse-long expected)})]

     [#"^the completion percent should be (\d+)$"
      (fn [[_ expected]]
        {:pattern :assert-completion-percent :expected (parse-long expected)})]

     [#"^the JSON should contain \"([^\"]+)\"$"
      (fn [[_ expected]]
        {:pattern :assert-json-contains :expected expected})]

     ;; --- Ready beads step patterns ---

    [#"^a registry with projects:$"
     (fn [_] {:pattern :registry-with-projects-table})]

    [#"^project \"([^\"]+)\" has config with status \"([^\"]+)\" and max-workers (\d+)$"
     (fn [[_ slug status max-w]]
       {:pattern :project-config-status-and-max-workers :slug slug :status status :max-workers (parse-long max-w)})]

    [#"^project \"([^\"]+)\" has config with max-workers (\d+)$"
     (fn [[_ slug max-w]]
       {:pattern :project-config-max-workers :slug slug :max-workers (parse-long max-w)})]

    [#"^project \"([^\"]+)\" has ready beads:$"
     (fn [[_ slug]]
       {:pattern :project-ready-beads-table :slug slug})]

    [#"^no active workers$"
     (fn [_] {:pattern :no-active-workers})]

    [#"^computing ready beads$"
     (fn [_] {:pattern :compute-ready-beads})]

    [#"^the result should contain bead \"([^\"]+)\"$"
     (fn [[_ bead-id]]
       {:pattern :assert-result-contains-bead :bead-id bead-id})]

    [#"^the result should not contain bead \"([^\"]+)\"$"
     (fn [[_ bead-id]]
       {:pattern :assert-result-not-contains-bead :bead-id bead-id})]

    [#"^the result should be empty$"
     (fn [_] {:pattern :assert-result-empty})]

    [#"^the (first|second|third) result should be from project \"([^\"]+)\"$"
     (fn [[_ ordinal slug]]
       {:pattern :assert-nth-result-project
        :position (case ordinal "first" 1 "second" 2 "third" 3)
        :slug slug})]

    [#"^ready beads to format:$"
     (fn [_] {:pattern :ready-beads-to-format})]

    [#"^no ready beads to format$"
     (fn [_] {:pattern :no-ready-beads-to-format})]

    [#"^formatting ready output$"
     (fn [_] {:pattern :format-ready-output})]

    [#"^the output should contain \"([^\"]+)\"$"
     (fn [[_ expected]]
       {:pattern :assert-output-contains :expected expected})]

    ;; --- Project listing step patterns ---

    [#"^a project list with the following projects:$"
    (fn [_] {:pattern :project-list-with-table})]

   [#"^an empty project list$"
    (fn [_] {:pattern :empty-project-list})]

   [#"^formatting the project list as JSON$"
    (fn [_] {:pattern :format-list-json})]

   [#"^formatting the project list$"
    (fn [_] {:pattern :format-list})]

   [#"^the output should contain column headers (.+)$"
    (fn [[_ headers-str]]
      {:pattern :assert-column-headers :headers (re-seq #"\"([^\"]+)\"" headers-str)})]

   [#"^the output should contain slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:pattern :assert-output-contains-slug :slug slug})]

   [#"^the output should contain iteration \"([^\"]+)\"$"
    (fn [[_ iteration]]
      {:pattern :assert-output-contains-iteration :iteration iteration})]

   [#"^the output should contain progress \"([^\"]+)\"$"
    (fn [[_ progress]]
      {:pattern :assert-output-contains-progress :progress progress})]

   [#"^the output should contain workers \"([^\"]+)\"$"
    (fn [[_ workers]]
      {:pattern :assert-output-contains-workers :workers workers})]

   [#"^the line for \"([^\"]+)\" should contain a dash for (\S+)$"
    (fn [[_ slug field]]
      {:pattern :assert-dash-placeholder :slug slug :field field})]

   [#"^the output should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-output-equals :expected expected})]

   [#"^\"([^\"]+)\" status should be colorized (\w+)$"
    (fn [[_ status color]]
      {:pattern :assert-status-color :status status :color color})]

   [#"^\"([^\"]+)\" priority should be colorized (\w+)$"
    (fn [[_ priority color]]
      {:pattern :assert-priority-color :priority priority :color color})]

   [#"^(\d+) percent progress should be colorized (\w+)$"
    (fn [[_ percent color]]
      {:pattern :assert-progress-color :percent (parse-long percent) :color color})]

   [#"^the JSON output should contain a project with slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:pattern :assert-json-project-exists :slug slug})]

   [#"^the JSON project \"([^\"]+)\" should have (\S+) \"([^\"]+)\"$"
    (fn [[_ slug key expected]]
      {:pattern :assert-json-project-string :slug slug :key key :expected expected})]

   [#"^the JSON project \"([^\"]+)\" should have (\S+) (\d+)$"
    (fn [[_ slug key expected]]
      {:pattern :assert-json-project-number :slug slug :key key :expected (parse-long expected)})]

   [#"^the JSON project \"([^\"]+)\" should have iteration number \"([^\"]+)\"$"
    (fn [[_ slug number]]
      {:pattern :assert-json-iteration-number :slug slug :number number})]

   ;; --- Configuration step patterns ---

   [#"^a config with values:$"
    (fn [_] {:pattern :config-with-values})]

   [#"^listing the config$"
    (fn [_] {:pattern :list-config})]

   [#"^getting config key \"([^\"]+)\"$"
    (fn [[_ key]]
      {:pattern :get-config-key :key key})]

   [#"^setting config key \"([^\"]+)\" to \"([^\"]+)\"$"
    (fn [[_ key value]]
      {:pattern :set-config-key :key key :value value})]

   [#"^an empty config string$"
    (fn [_] {:pattern :empty-config-string})]

   [#"^parsing the config$"
    (fn [_] {:pattern :parse-config})]

   [#"^requesting config help$"
    (fn [_] {:pattern :request-config-help})]

   [#"^the result should be ok with value \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-result-ok-with-value :expected expected})]

   [#"^the result should be an error$"
    (fn [_] {:pattern :assert-result-error})]

   [#"^the error message should contain \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:pattern :assert-error-message-contains :expected expected})]

   [#"^the config should have \"([^\"]+)\" set to \"([^\"]+)\"$"
    (fn [[_ key expected]]
      {:pattern :assert-config-has-value :key key :expected expected})]

   [#"^\"([^\"]+)\" should appear before \"([^\"]+)\" in the output$"
    (fn [[_ first-item second-item]]
      {:pattern :assert-appears-before :first first-item :second second-item})]])

(defn classify-step
  "Pattern-match step text into a typed IR node map, or {:pattern :unrecognized :text text}."
  [text]
  (or (some (fn [[pattern handler]]
              (when-let [match (re-matches pattern text)]
                (handler match)))
            step-patterns)
      {:pattern :unrecognized :text text}))

(def ^:private step-keywords #{"Given" "When" "Then" "And" "But"})

(defn- step-keyword? [trimmed]
  (some #(str/starts-with? trimmed (str % " ")) step-keywords))

(defn- strip-keyword [trimmed]
  (let [space-idx (str/index-of trimmed " ")]
    (when space-idx
      (subs trimmed (inc space-idx)))))

(defn- gherkin-type-for-keyword [trimmed]
  (cond
    (str/starts-with? trimmed "Given ") :given
    (str/starts-with? trimmed "When ")  :when
    (str/starts-with? trimmed "Then ")  :then
    (str/starts-with? trimmed "And ")   :and
    (str/starts-with? trimmed "But ")   :but
    :else                               nil))

(defn- parse-table-line
  "Parse a pipe-delimited table row into a vector of cell strings."
  [line]
  (->> (str/split line #"[|]" -1)
       rest                      ;; drop leading empty from first |
       butlast                   ;; drop trailing empty from last |
       (mapv str/trim)))

(defn- table-line? [trimmed]
  (str/starts-with? trimmed "|"))

(defn- attach-table
  "Attach parsed table data to an IR node, if table-lines are present."
  [ir-node table-lines]
  (if (seq table-lines)
    (let [parsed (mapv parse-table-line table-lines)]
      (assoc ir-node :table {:headers (first parsed)
                             :rows (vec (rest parsed))}))
    ir-node))

(defn- add-step [scenario ir-node]
  (update scenario :steps (fnil conj []) ir-node))

(defn- process-step-entry [state entry]
  (if (table-line? entry)
    ;; Accumulate table lines for the most recent step
    (update state :pending-table (fnil conj []) entry)
    ;; It's a step line — first, finalize any pending table on previous step
    (let [state (if (seq (:pending-table state))
                  (let [steps (get-in state [:scenario :steps])
                        last-step (peek steps)
                        updated-step (attach-table last-step (:pending-table state))
                        updated-steps (conj (pop steps) updated-step)]
                    (-> state
                        (assoc-in [:scenario :steps] updated-steps)
                        (dissoc :pending-table)))
                  state)
          gherkin-type (gherkin-type-for-keyword entry)]
      (when gherkin-type
        (let [text (strip-keyword entry)
              ir-node (-> (classify-step text)
                          (assoc :type gherkin-type))]
          (update state :scenario add-step ir-node))))))

(defn- finalize-pending-table
  "Attach any remaining pending table to the last step."
  [state]
  (if (seq (:pending-table state))
    (let [steps (get-in state [:scenario :steps])
          last-step (peek steps)
          updated-step (attach-table last-step (:pending-table state))
          updated-steps (conj (pop steps) updated-step)]
      (-> state
          (assoc-in [:scenario :steps] updated-steps)
          (dissoc :pending-table)))
    state))

(defn- parse-scenario-lines [lines]
  (let [result (-> (reduce process-step-entry
                           {:scenario {:steps []}}
                           lines)
                   finalize-pending-table)]
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

          ;; Table line in scenario (pipe-delimited row)
          (and (= state :scenario) (table-line? trimmed))
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
      (assoc :background {:steps (vec (map-indexed
                                        (fn [i line]
                                          (let [gherkin-type (gherkin-type-for-keyword line)
                                                text (strip-keyword line)]
                                            (-> (classify-step text)
                                                (assoc :type gherkin-type))))
                                        background-lines))}))))

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
  (spit path (edn-format data)))

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
