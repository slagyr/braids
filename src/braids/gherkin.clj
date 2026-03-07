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
      {:type :project-config :slug slug :worker-timeout (parse-long timeout)})]

   [#"^a session \"([^\"]+)\" with label \"([^\"]+)\"$"
    (fn [[_ session-id label]]
      {:type :session :session-id session-id :label label})]

   [#"^session \"([^\"]+)\" has status \"([^\"]+)\" and age (\d+) seconds$"
    (fn [[_ session-id status age]]
      {:type :session-status :session-id session-id :status status :age-seconds (parse-long age)})]

   [#"^bead \"([^\"]+)\" has status \"([^\"]+)\"$"
    (fn [[_ bead-id status]]
      {:type :bead-status :bead-id bead-id :status status})]

   [#"^bead \"([^\"]+)\" has no recorded status$"
    (fn [[_ bead-id]]
      {:type :bead-no-status :bead-id bead-id})]

   [#"^checking for zombies$"
    (fn [_] {:type :check-zombies})]

   [#"^session \"([^\"]+)\" should be a zombie with reason \"([^\"]+)\"$"
    (fn [[_ session-id reason]]
      {:type :assert-zombie :session-id session-id :reason reason})]

   [#"^no zombies should be detected$"
    (fn [_] {:type :assert-no-zombies})]

   ;; --- Orch spawning patterns ---

   [#"^a project \"([^\"]+)\" with max-workers (\d+)$"
    (fn [[_ slug max-w]]
      {:type :project-config :slug slug :max-workers (parse-long max-w)})]

   [#"^project \"([^\"]+)\" has an active iteration \"([^\"]+)\"$"
    (fn [[_ slug iteration]]
      {:type :active-iteration :slug slug :iteration iteration})]

   [#"^project \"([^\"]+)\" has no active iteration$"
    (fn [[_ slug]]
      {:type :no-active-iteration :slug slug})]

   [#"^project \"([^\"]+)\" has (\d+) ready beads? with id \"([^\"]+)\"$"
    (fn [[_ slug _count bead-id]]
      {:type :ready-bead-with-id :slug slug :bead-id bead-id})]

   [#"^project \"([^\"]+)\" has (\d+) ready beads?$"
    (fn [[_ slug count]]
      {:type :ready-beads :slug slug :count (parse-long count)})]

   [#"^project \"([^\"]+)\" has (\d+) active workers?$"
    (fn [[_ slug count]]
      {:type :active-workers :slug slug :count (parse-long count)})]

   [#"^the orchestrator ticks$"
    (fn [_] {:type :orch-tick})]

   [#"^the orchestrator ticks for project \"([^\"]+)\" only$"
    (fn [[_ slug]]
      {:type :orch-tick-project :slug slug})]

   [#"^the action should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-action :expected expected})]

   [#"^(\d+) workers? should be spawned$"
    (fn [[_ count]]
      {:type :assert-spawn-count :count (parse-long count)})]

   [#"^the idle reason should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-idle-reason :expected expected})]

   [#"^the spawn label should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-spawn-label :expected expected})]

   ;; --- Worker session tracking patterns ---

   [#"^a bead with id \"([^\"]+)\"$"
    (fn [[_ bead-id]]
      {:type :bead :bead-id bead-id})]

   [#"^another bead with id \"([^\"]+)\"$"
    (fn [[_ bead-id]]
      {:type :bead :bead-id bead-id})]

   [#"^a session ID \"([^\"]+)\"$"
    (fn [[_ session-id]]
      {:type :session-id-literal :session-id session-id})]

   [#"^generating the session ID twice$"
    (fn [_] {:type :generate-session-id-twice})]

   [#"^generating the session ID$"
    (fn [_] {:type :generate-session-id})]

   [#"^generating session IDs for both$"
    (fn [_] {:type :generate-session-ids-both})]

   [#"^parsing the session ID$"
    (fn [_] {:type :parse-session-id})]

   [#"^the session ID should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-session-id :expected expected})]

   [#"^both session IDs should be identical$"
    (fn [_] {:type :assert-ids-identical})]

   [#"^the session IDs should be different$"
    (fn [_] {:type :assert-ids-different})]

    [#"^the extracted bead ID should be \"([^\"]+)\"$"
     (fn [[_ expected]]
       {:type :assert-bead-id :expected expected})]

   ;; --- Project lifecycle patterns ---

   [#"^bd is not available$"
    (fn [_] {:type :bd-not-available})]

   [#"^bd is available$"
    (fn [_] {:type :bd-available})]

   [#"^no registry exists$"
    (fn [_] {:type :no-registry})]

   [#"^a registry already exists$"
    (fn [_] {:type :registry-exists})]

   [#"^force is not set$"
    (fn [_] {:type :force-not-set})]

   [#"^force is set$"
    (fn [_] {:type :force-set})]

   [#"^braids dir does not exist$"
    (fn [_] {:type :braids-dir-not-exists})]

   [#"^braids dir already exists$"
    (fn [_] {:type :braids-dir-exists})]

   [#"^braids home does not exist$"
    (fn [_] {:type :braids-home-not-exists})]

   [#"^braids home already exists$"
    (fn [_] {:type :braids-home-exists})]

   [#"^checking prerequisites$"
    (fn [_] {:type :check-prerequisites})]

   [#"^planning init$"
    (fn [_] {:type :plan-init})]

   [#"^prerequisites should fail with \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-prereq-fail :expected expected})]

   [#"^prerequisites should pass$"
    (fn [_] {:type :assert-prereq-pass})]

   [#"^the plan should include \"([^\"]+)\"$"
    (fn [[_ action]]
      {:type :assert-plan-include :action action})]

   [#"^the plan should not include \"([^\"]+)\"$"
    (fn [[_ action]]
      {:type :assert-plan-not-include :action action})]

   [#"^a new project with slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:type :new-project-slug :slug slug})]

   [#"^a new project with name \"([^\"]+)\"$"
    (fn [[_ name]]
      {:type :new-project-name :name name})]

   [#"^name \"([^\"]+)\"$"
    (fn [[_ name]]
      {:type :set-name :name name})]

   [#"^goal \"([^\"]+)\"$"
    (fn [[_ goal]]
      {:type :set-goal :goal goal})]

   [#"^a registry with project \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:type :registry-with-project :slug slug})]

   [#"^a new registry entry with slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:type :new-registry-entry :slug slug})]

   [#"^validating new project params$"
    (fn [_] {:type :validate-new-project})]

   [#"^adding the entry to the registry$"
    (fn [_] {:type :add-to-registry})]

   [#"^building the project config$"
    (fn [_] {:type :build-project-config})]

   [#"^validation should fail with \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-validation-fail :expected expected})]

   [#"^it should fail with \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-should-fail :expected expected})]

   [#"^the config (\S+) should be \"([^\"]+)\"$"
    (fn [[_ key expected]]
      {:type :assert-config-value :key key :expected expected})]

   [#"^the config (\S+) should be (\d+)$"
    (fn [[_ key expected]]
      {:type :assert-config-number :key key :expected (parse-long expected)})]

     ;; --- Iteration management step patterns ---

     [#"^iteration EDN with number \"([^\"]+)\" and status \"([^\"]+)\" and (\d+) stor(?:y|ies)$"
      (fn [[_ number status count]]
        {:type :iteration-edn :number number :status status :story-count (parse-long count)})]

     [#"^the EDN has no guardrails or notes$"
      (fn [_] {:type :edn-no-guardrails-or-notes})]

     [#"^an iteration with number \"([^\"]+)\" and status \"([^\"]+)\" and stories$"
      (fn [[_ number status]]
        {:type :iteration-with-status :number number :status status})]

     [#"^an iteration with no number$"
      (fn [_] {:type :iteration-no-number})]

     [#"^an iteration with stories \"([^\"]+)\" and \"([^\"]+)\"$"
      (fn [[_ id1 id2]]
        {:type :iteration-with-stories :story-ids [id1 id2]})]

     [#"^an iteration with story \"([^\"]+)\"$"
      (fn [[_ story-id]]
        {:type :iteration-with-story :story-id story-id})]

     [#"^bead \"([^\"]+)\" has status \"([^\"]+)\" and priority (\d+)$"
      (fn [[_ bead-id status priority]]
        {:type :iter-bead-status :bead-id bead-id :status status :priority (parse-long priority)})]

     [#"^no bead data exists$"
      (fn [_] {:type :no-bead-data})]

     [#"^annotated stories with (\d+) closed and (\d+) open out of (\d+) total$"
      (fn [[_ closed open total]]
        {:type :annotated-stories :closed (parse-long closed) :open (parse-long open) :total (parse-long total)})]

     [#"^an iteration with no stories$"
      (fn [_] {:type :iteration-no-stories})]

     [#"^an iteration \"([^\"]+)\" with status \"([^\"]+)\"$"
      (fn [[_ number status]]
        {:type :iteration-number-status :number number :status status})]

     [#"^a story \"([^\"]+)\" with status \"([^\"]+)\"$"
      (fn [[_ story-id status]]
        {:type :story-with-status :story-id story-id :status status})]

     [#"^completion stats of (\d+) closed out of (\d+)$"
      (fn [[_ closed total]]
        {:type :completion-stats :closed (parse-long closed) :total (parse-long total)})]

     [#"^parsing the iteration EDN$"
      (fn [_] {:type :parse-iteration-edn})]

     [#"^validating the iteration$"
      (fn [_] {:type :validate-iteration})]

     [#"^annotating stories with bead data$"
      (fn [_] {:type :annotate-stories})]

     [#"^calculating completion stats$"
      (fn [_] {:type :calculate-completion-stats})]

     [#"^formatting the iteration$"
      (fn [_] {:type :format-iteration})]

     [#"^formatting the iteration as JSON$"
      (fn [_] {:type :format-iteration-json})]

     [#"^the iteration number should be \"([^\"]+)\"$"
      (fn [[_ expected]]
        {:type :assert-iteration-number :expected expected})]

     [#"^the iteration status should be \"([^\"]+)\"$"
      (fn [[_ expected]]
        {:type :assert-iteration-status :expected expected})]

     [#"^the iteration guardrails should be empty$"
      (fn [_] {:type :assert-iteration-guardrails-empty})]

     [#"^the iteration notes should be empty$"
      (fn [_] {:type :assert-iteration-notes-empty})]

     [#"^story \"([^\"]+)\" should have status \"([^\"]+)\"$"
      (fn [[_ story-id expected]]
        {:type :assert-story-status :story-id story-id :expected expected})]

     [#"^the total should be (\d+)$"
      (fn [[_ expected]]
        {:type :assert-total :expected (parse-long expected)})]

     [#"^the closed count should be (\d+)$"
      (fn [[_ expected]]
        {:type :assert-closed-count :expected (parse-long expected)})]

     [#"^the completion percent should be (\d+)$"
      (fn [[_ expected]]
        {:type :assert-completion-percent :expected (parse-long expected)})]

     [#"^the JSON should contain \"([^\"]+)\"$"
      (fn [[_ expected]]
        {:type :assert-json-contains :expected expected})]

     ;; --- Ready beads step patterns ---

    [#"^a registry with projects:$"
     (fn [_] {:type :registry-with-projects-table})]

    [#"^project \"([^\"]+)\" has config with status \"([^\"]+)\" and max-workers (\d+)$"
     (fn [[_ slug status max-w]]
       {:type :project-config-status-and-max-workers :slug slug :status status :max-workers (parse-long max-w)})]

    [#"^project \"([^\"]+)\" has config with max-workers (\d+)$"
     (fn [[_ slug max-w]]
       {:type :project-config-max-workers :slug slug :max-workers (parse-long max-w)})]

    [#"^project \"([^\"]+)\" has ready beads:$"
     (fn [[_ slug]]
       {:type :project-ready-beads-table :slug slug})]

    [#"^no active workers$"
     (fn [_] {:type :no-active-workers})]

    [#"^computing ready beads$"
     (fn [_] {:type :compute-ready-beads})]

    [#"^the result should contain bead \"([^\"]+)\"$"
     (fn [[_ bead-id]]
       {:type :assert-result-contains-bead :bead-id bead-id})]

    [#"^the result should not contain bead \"([^\"]+)\"$"
     (fn [[_ bead-id]]
       {:type :assert-result-not-contains-bead :bead-id bead-id})]

    [#"^the result should be empty$"
     (fn [_] {:type :assert-result-empty})]

    [#"^the (first|second|third) result should be from project \"([^\"]+)\"$"
     (fn [[_ ordinal slug]]
       {:type :assert-nth-result-project
        :position (case ordinal "first" 1 "second" 2 "third" 3)
        :slug slug})]

    [#"^ready beads to format:$"
     (fn [_] {:type :ready-beads-to-format})]

    [#"^no ready beads to format$"
     (fn [_] {:type :no-ready-beads-to-format})]

    [#"^formatting ready output$"
     (fn [_] {:type :format-ready-output})]

    [#"^the output should contain \"([^\"]+)\"$"
     (fn [[_ expected]]
       {:type :assert-output-contains :expected expected})]

    ;; --- Project listing step patterns ---

    [#"^a project list with the following projects:$"
    (fn [_] {:type :project-list-with-table})]

   [#"^an empty project list$"
    (fn [_] {:type :empty-project-list})]

   [#"^formatting the project list as JSON$"
    (fn [_] {:type :format-list-json})]

   [#"^formatting the project list$"
    (fn [_] {:type :format-list})]

   [#"^the output should contain column headers (.+)$"
    (fn [[_ headers-str]]
      {:type :assert-column-headers :headers (re-seq #"\"([^\"]+)\"" headers-str)})]

   [#"^the output should contain slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:type :assert-output-contains-slug :slug slug})]

   [#"^the output should contain iteration \"([^\"]+)\"$"
    (fn [[_ iteration]]
      {:type :assert-output-contains-iteration :iteration iteration})]

   [#"^the output should contain progress \"([^\"]+)\"$"
    (fn [[_ progress]]
      {:type :assert-output-contains-progress :progress progress})]

   [#"^the output should contain workers \"([^\"]+)\"$"
    (fn [[_ workers]]
      {:type :assert-output-contains-workers :workers workers})]

   [#"^the line for \"([^\"]+)\" should contain a dash for (\S+)$"
    (fn [[_ slug field]]
      {:type :assert-dash-placeholder :slug slug :field field})]

   [#"^the output should be \"([^\"]+)\"$"
    (fn [[_ expected]]
      {:type :assert-output-equals :expected expected})]

   [#"^\"([^\"]+)\" status should be colorized (\w+)$"
    (fn [[_ status color]]
      {:type :assert-status-color :status status :color color})]

   [#"^\"([^\"]+)\" priority should be colorized (\w+)$"
    (fn [[_ priority color]]
      {:type :assert-priority-color :priority priority :color color})]

   [#"^(\d+) percent progress should be colorized (\w+)$"
    (fn [[_ percent color]]
      {:type :assert-progress-color :percent (parse-long percent) :color color})]

   [#"^the JSON output should contain a project with slug \"([^\"]+)\"$"
    (fn [[_ slug]]
      {:type :assert-json-project-exists :slug slug})]

   [#"^the JSON project \"([^\"]+)\" should have (\S+) \"([^\"]+)\"$"
    (fn [[_ slug key expected]]
      {:type :assert-json-project-string :slug slug :key key :expected expected})]

   [#"^the JSON project \"([^\"]+)\" should have (\S+) (\d+)$"
    (fn [[_ slug key expected]]
      {:type :assert-json-project-number :slug slug :key key :expected (parse-long expected)})]

   [#"^the JSON project \"([^\"]+)\" should have iteration number \"([^\"]+)\"$"
    (fn [[_ slug number]]
      {:type :assert-json-iteration-number :slug slug :number number})]])

(defn classify-step
  "Pattern-match step text into a typed IR node map, or {:type :unrecognized :text text}."
  [text]
  (or (some (fn [[pattern handler]]
              (when-let [match (re-matches pattern text)]
                (handler match)))
            step-patterns)
      {:type :unrecognized :text text}))

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

(defn- add-step [scenario phase ir-node]
  (update scenario phase (fnil conj []) ir-node))

(defn- process-step-entry [state entry]
  (if (table-line? entry)
    ;; Accumulate table lines for the most recent step
    (update state :pending-table (fnil conj []) entry)
    ;; It's a step line — first, finalize any pending table on previous step
    (let [state (if (and (:current-phase state) (seq (:pending-table state)))
                  (let [phase (:current-phase state)
                        steps (get-in state [:scenario phase])
                        last-step (peek steps)
                        updated-step (attach-table last-step (:pending-table state))
                        updated-steps (conj (pop steps) updated-step)]
                    (-> state
                        (assoc-in [:scenario phase] updated-steps)
                        (dissoc :pending-table)))
                  state)
          phase (phase-for-keyword entry)]
      (if phase
        (let [text (strip-keyword entry)]
          (-> state
              (assoc :current-phase phase)
              (update :scenario add-step phase (classify-step text))))
        ;; And/But — append to current phase
        (let [text (strip-keyword entry)]
          (update state :scenario add-step (:current-phase state) (classify-step text)))))))

(defn- finalize-pending-table
  "Attach any remaining pending table to the last step."
  [state]
  (if (and (:current-phase state) (seq (:pending-table state)))
    (let [phase (:current-phase state)
          steps (get-in state [:scenario phase])
          last-step (peek steps)
          updated-step (attach-table last-step (:pending-table state))
          updated-steps (conj (pop steps) updated-step)]
      (-> state
          (assoc-in [:scenario phase] updated-steps)
          (dissoc :pending-table)))
    state))

(defn- parse-scenario-lines [lines]
  (let [result (-> (reduce process-step-entry
                           {:scenario {:givens [] :whens [] :thens []}
                            :current-phase nil}
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
      (assoc :background {:givens (mapv (comp classify-step strip-keyword) background-lines)}))))

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
