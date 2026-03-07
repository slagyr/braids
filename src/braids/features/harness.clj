(ns braids.features.harness
  "Test harness for generated feature specs. Manages mutable test state
   and provides helpers that generated code calls."
  (:require [braids.orch :as orch]
            [braids.orch-runner :as orch-runner]
            [braids.init :as init]
            [braids.new :as new-proj]
            [braids.list :as list]
            [braids.ready :as ready]
            [braids.iteration :as iteration]
            [braids.config :as config]
            [braids.status :as status]
            [cheshire.core :as json]
            [clojure.string :as str]))

;; --- Mutable test state ---

(def ^:private state (atom nil))

(defn reset!
  "Reset all test state. Call before each scenario."
  []
  (clojure.core/reset! state {:sessions {}
                               :configs {}
                               :bead-statuses {}
                               :zombies []
                               :registry {:projects []}
                               :iterations {}
                               :beads {}
                               :workers {}
                               :tick-result nil
                               :bead-ids []
                               :session-id-literal nil
                               :session-id-result nil
                               :session-ids []
                               :parsed-bead-id nil
                               ;; Project lifecycle state
                               :prereq-opts {}
                               :prereq-result nil
                               :plan-opts {}
                               :plan-result nil
                               :new-project-params {}
                               :validation-result nil
                               :new-entry nil
                               :add-registry-result nil
                               :project-config-result nil
                               ;; Project listing state
                               :list-projects []
                               :list-output nil
                               :list-json-output nil
                               ;; Generic output (shared across features)
                               :output nil
                               ;; Iteration management state
                               :iter-edn-str nil
                               :iter-parsed nil
                               :iter-data nil
                               :iter-stories []
                               :iter-beads []
                               :iter-annotated nil
                               :iter-stats nil
                               :iter-format-data nil
                               :iter-json-output nil
                               ;; Ready beads state
                               :ready-registry {:projects []}
                               :ready-configs {}
                               :ready-beads {}
                               :ready-result nil
                               :ready-format-beads nil
                               :ready-output nil
                               ;; Configuration state
                               :config-map nil
                               :config-edn-str nil
                               :config-result nil
                               ;; Project status / dashboard state
                               :status-registry {:projects []}
                               :status-configs {}
                               :status-iterations {}
                               :status-workers {}
                               :dashboard nil
                               :dashboard-json-data nil
                               :detail-projects {}
                               ;; Orch runner state
                               :runner-spawn-entry {}
                               :runner-config {}
                               :runner-cli-args []
                               :runner-parsed-cli-args nil
                               :runner-worker-task nil
                               :runner-worker-args nil
                               :runner-tick-result nil
                               :runner-zombies []
                               :runner-log nil}))

;; --- State accessors ---

(defn sessions [] (:sessions @state))
(defn configs [] (:configs @state))
(defn bead-statuses [] (:bead-statuses @state))
(defn zombies [] (:zombies @state))

;; --- State builders (Given steps) ---

(defn add-project-config
  "Add a project config entry."
  [slug config]
  (swap! state assoc-in [:configs slug] config))

(defn add-session
  "Add a session entry."
  [session-id attrs]
  (swap! state assoc-in [:sessions session-id] attrs))

(defn set-session-status
  "Set a session's status and age."
  [session-id status age-seconds]
  (swap! state update-in [:sessions session-id]
         merge {:status status :age-seconds age-seconds}))

(defn set-bead-status
  "Set a bead's status."
  [bead-id status]
  (swap! state assoc-in [:bead-statuses bead-id] status))

;; --- Actions (When steps) ---

(defn check-zombies!
  "Run zombie detection with accumulated state."
  []
  (let [{:keys [sessions configs bead-statuses]} @state
        session-list (mapv (fn [[sid attrs]]
                             (merge {:session-id sid} attrs))
                           sessions)
        result (orch/detect-zombies session-list configs bead-statuses)]
    (swap! state assoc :zombies result)))

;; --- Query helpers (Then steps) ---

(defn zombie?
  "Returns true if the given session-id is in the zombie list."
  [session-id]
  (let [session (get (sessions) session-id)
        label (:label session)]
    (some #(= label (:label %)) (zombies))))

(defn zombie-reason
  "Returns the zombie reason for the given session-id, or nil."
  [session-id]
  (let [session (get (sessions) session-id)
        label (:label session)]
    (:reason (first (filter #(= label (:label %)) (zombies))))))

;; --- Orch spawning helpers ---

(defn add-project
  "Add a project to registry with :active status and set its config."
  [slug config]
  (swap! state (fn [s]
                 (-> s
                     (update-in [:registry :projects] conj {:slug slug :status :active :path (str "/projects/" slug)})
                     (assoc-in [:configs slug] (merge {:status :active} config))))))

(defn set-active-iteration
  "Set an active iteration for a project."
  [slug iteration-number]
  (swap! state assoc-in [:iterations slug] iteration-number))

(defn remove-iteration
  "Remove the iteration for a project."
  [slug]
  (swap! state update :iterations dissoc slug))

(defn set-ready-beads
  "Set N ready beads for a project (generates synthetic bead ids)."
  [slug n]
  (let [beads (mapv (fn [i] {:id (str slug "-bead-" i)}) (range n))]
    (swap! state assoc-in [:beads slug] beads)))

(defn set-ready-bead-with-id
  "Set a single ready bead with a specific id for a project."
  [slug bead-id]
  (swap! state assoc-in [:beads slug] [{:id bead-id}]))

(defn set-active-workers
  "Set the active worker count for a project."
  [slug count]
  (swap! state assoc-in [:workers slug] count))

(defn orch-tick!
  "Run orch/tick with all accumulated state, store the result.
   Also captures formatted debug output if open-beads data is available."
  []
  (let [{:keys [registry configs iterations beads workers open-beads]} @state
        result (orch/tick registry configs iterations beads workers {})
        all-open-beads (or open-beads {})
        output (orch/format-debug-output registry configs iterations all-open-beads result workers)]
    (swap! state assoc :tick-result result :tick-output output :output output)))

(defn orch-tick-project!
  "Run orch/tick for a single project only.
   Filters registry and state to include only the specified project."
  [slug]
  (let [{:keys [registry configs iterations beads workers]} @state
        filtered-registry (update registry :projects
                                  (fn [ps] (filterv #(= slug (:slug %)) ps)))
        result (orch/tick filtered-registry configs iterations beads workers {})]
    (swap! state assoc :tick-result result)))

;; --- Orch result accessors ---

(defn tick-action
  "Returns the action string from the last tick result."
  []
  (:action (:tick-result @state)))

(defn spawn-count
  "Returns the number of spawns from the last tick result."
  []
  (count (:spawns (:tick-result @state))))

(defn idle-reason
  "Returns the idle reason from the last tick result."
  []
  (:reason (:tick-result @state)))

(defn spawn-label
  "Returns the label of the first spawn from the last tick result."
  []
  (:label (first (:spawns (:tick-result @state)))))

;; --- Worker session tracking helpers ---

(defn set-bead-id
  "Store a bead id. Accumulates into a vector for multi-bead scenarios."
  [bead-id]
  (swap! state update :bead-ids conj bead-id))

(defn set-session-id-literal
  "Store a literal session ID string for parsing."
  [session-id]
  (swap! state assoc :session-id-literal session-id))

(defn generate-session-id!
  "Generate session ID from the first stored bead id."
  []
  (let [bead-id (first (:bead-ids @state))
        result (orch/worker-session-id bead-id)]
    (swap! state assoc :session-id-result result)))

(defn generate-session-id-twice!
  "Generate session ID twice from the first stored bead id, store both."
  []
  (let [bead-id (first (:bead-ids @state))
        id1 (orch/worker-session-id bead-id)
        id2 (orch/worker-session-id bead-id)]
    (swap! state assoc :session-ids [id1 id2])))

(defn generate-session-ids-both!
  "Generate session IDs for all stored bead ids."
  []
  (let [ids (mapv orch/worker-session-id (:bead-ids @state))]
    (swap! state assoc :session-ids ids)))

(defn parse-session-id!
  "Parse the stored session ID literal to extract bead id."
  []
  (let [session-id (:session-id-literal @state)
        result (orch/parse-worker-session-id session-id)]
    (swap! state assoc :parsed-bead-id result)))

;; --- Session tracking result accessors ---

(defn session-id-result
  "Returns the generated session ID."
  []
  (:session-id-result @state))

(defn session-ids-identical?
  "Returns true if the two generated session IDs are identical."
  []
  (let [[id1 id2] (:session-ids @state)]
    (= id1 id2)))

(defn session-ids-different?
  "Returns true if the generated session IDs are all different."
  []
  (let [ids (:session-ids @state)]
    (apply distinct? ids)))

(defn parsed-bead-id
  "Returns the parsed bead id from the last parse-session-id! call."
  []
  (:parsed-bead-id @state))

;; --- Project lifecycle helpers ---

;; Given step builders

(defn set-bd-not-available
  "Set bd as not available for prerequisite checks."
  []
  (swap! state assoc-in [:prereq-opts :bd-available?] false))

(defn set-bd-available
  "Set bd as available for prerequisite checks."
  []
  (swap! state assoc-in [:prereq-opts :bd-available?] true))

(defn set-no-registry
  "Set registry as not existing."
  []
  (swap! state assoc-in [:prereq-opts :registry-exists?] false))

(defn set-registry-exists
  "Set registry as existing."
  []
  (swap! state assoc-in [:prereq-opts :registry-exists?] true))

(defn set-force-not-set
  "Set force flag to false."
  []
  (swap! state assoc-in [:prereq-opts :force?] false))

(defn set-force-set
  "Set force flag to true."
  []
  (swap! state assoc-in [:prereq-opts :force?] true))

(defn set-braids-dir-not-exists
  "Set braids dir as not existing."
  []
  (swap! state update :plan-opts merge {:braids-dir "/tmp/braids"
                                        :braids-dir-exists? false}))

(defn set-braids-dir-exists
  "Set braids dir as existing."
  []
  (swap! state update :plan-opts merge {:braids-dir "/tmp/braids"
                                        :braids-dir-exists? true}))

(defn set-braids-home-not-exists
  "Set braids home as not existing."
  []
  (swap! state update :plan-opts merge {:braids-home "/tmp/projects"
                                        :braids-home-exists? false}))

(defn set-braids-home-exists
  "Set braids home as existing."
  []
  (swap! state update :plan-opts merge {:braids-home "/tmp/projects"
                                        :braids-home-exists? true}))

(defn set-new-project-slug
  "Set the slug for a new project."
  [slug]
  (swap! state assoc-in [:new-project-params :slug] slug))

(defn set-new-project-name
  "Set the name for a new project."
  [name]
  (swap! state assoc-in [:new-project-params :name] name))

(defn set-new-project-goal
  "Set the goal for a new project."
  [goal]
  (swap! state assoc-in [:new-project-params :goal] goal))

(defn set-registry-with-project
  "Set up a registry containing a specific project."
  [slug]
  (swap! state assoc :registry {:projects [{:slug slug :status :active :path (str "/projects/" slug)}]}))

(defn set-new-registry-entry
  "Create a new registry entry to be added."
  [slug]
  (swap! state assoc :new-entry {:slug slug :status :active :path (str "/projects/" slug)}))

;; When step actions

(defn check-prerequisites!
  "Run init/check-prerequisites with accumulated state."
  []
  (let [opts (:prereq-opts @state)
        result (init/check-prerequisites opts)]
    (swap! state assoc :prereq-result result)))

(defn plan-init!
  "Run init/plan-init with accumulated state."
  []
  (let [opts (merge {:registry-path "/tmp/braids/registry.edn"
                     :config-path "/tmp/braids/config.edn"}
                    (:plan-opts @state))
        result (init/plan-init opts)]
    (swap! state assoc :plan-result result)))

(defn validate-new-project!
  "Run new/validate-new-params with accumulated state."
  []
  (let [params (:new-project-params @state)
        result (new-proj/validate-new-params params)]
    (swap! state assoc :validation-result result)))

(defn add-to-registry!
  "Try to add the stored entry to the registry."
  []
  (let [registry (:registry @state)
        entry (:new-entry @state)]
    (try
      (let [result (new-proj/add-to-registry registry entry)]
        (swap! state assoc :add-registry-result {:ok result}))
      (catch Exception e
        (swap! state assoc :add-registry-result {:error (.getMessage e)})))))

(defn build-project-config!
  "Run new/build-project-config with accumulated state."
  []
  (let [params (:new-project-params @state)
        result (new-proj/build-project-config params)]
    (swap! state assoc :project-config-result result)))

;; Then step accessors

(defn prereq-errors
  "Returns the list of prerequisite errors."
  []
  (:prereq-result @state))

(defn plan-actions
  "Returns the list of planned action keywords."
  []
  (mapv (comp clojure.core/name :action) (:plan-result @state)))

(defn validation-errors
  "Returns the list of validation errors."
  []
  (:validation-result @state))

(defn add-registry-error
  "Returns the error from add-to-registry, or nil."
  []
  (get-in @state [:add-registry-result :error]))

(defn project-config
  "Returns the built project config."
  []
  (:project-config-result @state))

;; --- Project listing helpers ---

(defn- table-row->project
  "Convert a table row (vector of strings) to a project map using headers."
  [headers row]
  (let [m (zipmap headers row)
        slug (get m "slug")
        status (get m "status")
        priority (get m "priority")
        iteration-num (get m "iteration")
        closed (get m "closed")
        total (get m "total")
        percent (get m "percent")
        workers-str (get m "workers")
        max-workers-str (get m "max-workers")
        path (get m "path")]
    (cond->
      {:slug slug
       :status (when (seq status) (keyword status))
       :priority (when (seq priority) (keyword priority))
       :path path
       :workers (when (seq workers-str) (parse-long workers-str))
       :max-workers (when (seq max-workers-str) (parse-long max-workers-str))}
      (seq iteration-num)
      (assoc :iteration
             (cond-> {:number iteration-num}
               (seq closed)
               (assoc :stats {:closed (parse-long closed)
                              :total (parse-long total)
                              :percent (parse-long percent)}))))))

(defn set-project-list-from-table
  "Build project list from table headers and rows."
  [headers rows]
  (let [projects (mapv #(table-row->project headers %) rows)]
    (swap! state assoc :list-projects projects)))

(defn set-empty-project-list
  "Set an empty project list."
  []
  (swap! state assoc :list-projects []))

(defn format-list!
  "Format the project list using list/format-list."
  []
  (let [projects (:list-projects @state)
        output (list/format-list {:projects projects})]
    (swap! state assoc :list-output output :output output)))

(defn format-list-json!
  "Format the project list as JSON using list/format-list-json."
  []
  (let [projects (:list-projects @state)
        output (list/format-list-json {:projects projects})]
    (swap! state assoc :list-json-output output :output output)))

(defn list-output
  "Returns the formatted list output."
  []
  (or (:list-output @state) (:list-json-output @state)))

(defn line-contains-dash?
  "Returns true if the line for the given slug contains a dash placeholder."
  [slug]
  (when-let [output (:list-output @state)]
    (let [lines (str/split-lines output)]
      (some (fn [line]
              (and (str/includes? line slug)
                   (str/includes? line "—")))
            lines))))

(defn colorized?
  "Returns true if the output contains the given text wrapped in the expected ANSI color."
  [output text color]
  (let [color-code (case color
                     "red" "\033[31m"
                     "green" "\033[32m"
                     "yellow" "\033[33m"
                     nil)]
    (and color-code
         (str/includes? output (str color-code))
         (str/includes? output text))))

(defn json-project
  "Find a project by slug in the JSON output. Handles both array and {projects: [...]} formats."
  [slug]
  (when-let [output (:list-json-output @state)]
    (let [parsed (json/parse-string output)
          projects (if (map? parsed) (get parsed "projects") parsed)]
      (first (filter #(= slug (get % "slug")) projects)))))

;; --- Ready beads helpers ---

(defn- table-row->ready-registry-project
  "Convert a table row to a registry project entry."
  [headers row]
  (let [m (zipmap headers row)]
    {:slug (get m "slug")
     :status (keyword (get m "status"))
     :priority (keyword (get m "priority"))
     :path (or (get m "path") (str "/projects/" (get m "slug")))}))

(defn set-registry-from-table
  "Build registry from table headers and rows. Sets both ready-registry and status-registry."
  [headers rows]
  (let [projects (mapv #(table-row->ready-registry-project headers %) rows)
        registry {:projects projects}]
    (swap! state assoc :ready-registry registry :status-registry registry)))

(defn set-project-config
  "Set ready-beads config for a project."
  [slug config]
  (let [parsed-config (reduce-kv
                        (fn [m k v]
                          (assoc m k (cond
                                       (= k :status) (keyword v)
                                       :else v)))
                        {}
                        config)]
    (swap! state assoc-in [:ready-configs slug]
           (merge {:status :active} parsed-config))))

(defn- table-row->ready-bead
  "Convert a table row to a ready bead map."
  [headers row]
  (let [m (zipmap headers row)]
    {:id (get m "id")
     :title (get m "title")
     :priority (get m "priority")}))

(defn set-project-ready-beads
  "Set ready beads for a project from table data."
  [slug headers rows]
  (let [beads (mapv #(table-row->ready-bead headers %) rows)]
    (swap! state assoc-in [:ready-beads slug] beads)))

(defn compute-ready-beads!
  "Run ready/ready-beads with accumulated ready-beads state.
   Uses :workers from shared state (same as orch spawning)."
  []
  (let [{:keys [ready-registry ready-configs ready-beads workers]} @state
        result (ready/ready-beads ready-registry ready-configs ready-beads workers)]
    (swap! state assoc :ready-result result)))

(defn ready-result
  "Returns the ready beads result."
  []
  (:ready-result @state))

(defn result-contains-bead?
  "Returns true if the ready result contains a bead with the given id."
  [bead-id]
  (some #(= bead-id (:id %)) (:ready-result @state)))

(defn set-ready-beads-to-format
  "Build beads for format-ready-output from table data."
  [headers rows]
  (let [beads (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:project (get m "project")
                         :id (get m "id")
                         :title (get m "title")
                         :priority (get m "priority")}))
                    rows)]
    (swap! state assoc :ready-format-beads beads)))

(defn set-no-ready-beads-to-format
  "Set empty beads for formatting."
  []
  (swap! state assoc :ready-format-beads []))

(defn format-ready-output!
  "Format ready beads using ready/format-ready-output."
  []
  (let [beads (:ready-format-beads @state)
        output (ready/format-ready-output beads)]
    (swap! state assoc :ready-output output :output output)))

(defn ready-output
  "Returns the formatted ready output."
  []
  (:ready-output @state))

(defn output
  "Returns the most recent output (generic accessor for any feature)."
  []
  (:output @state))

;; --- Iteration management helpers ---

;; Given step builders

(defn set-iteration-edn
  "Build an iteration EDN string with the given number, status, and story count."
  [number status story-count]
  (let [stories (vec (repeat story-count (str "story-" (rand-int 10000))))
        edn-str (pr-str {:number number :status (keyword status) :stories stories})]
    (swap! state assoc :iter-edn-str edn-str)))

(defn set-iteration-with-status
  "Store an iteration map with a given status for validation."
  [number status]
  (swap! state assoc :iter-data {:number number :status (keyword status) :stories []}))

(defn set-iteration-no-number
  "Store an iteration with no number for validation."
  []
  (swap! state assoc :iter-data {:status :planning :stories []}))

(defn set-iteration-stories
  "Store story IDs for annotation."
  [story-ids]
  (swap! state assoc :iter-stories story-ids))

(defn add-iter-bead
  "Add a bead with status and priority for annotation."
  [bead-id status priority]
  (swap! state update :iter-beads conj
         {"id" bead-id "status" status "priority" priority}))

(defn set-annotated-stories
  "Build pre-annotated stories for completion stats testing."
  [closed open total]
  (let [closed-stories (repeat closed {:id "c" :status "closed"})
        open-stories (repeat open {:id "o" :status "open"})
        stories (vec (concat closed-stories open-stories))]
    (swap! state assoc :iter-annotated stories)))

(defn set-iteration-number-status
  "Set up iteration data for formatting."
  [number status]
  (swap! state assoc :iter-format-data {:number number :status status :stories [] :stats nil}))

(defn add-story-with-status
  "Add a story with status to the iteration format data."
  [story-id status]
  (swap! state update-in [:iter-format-data :stories] conj
         {:id story-id :title story-id :status status :priority nil :deps []}))

(defn set-completion-stats
  "Set completion stats for formatting."
  [closed total]
  (let [percent (if (zero? total) 0 (int (* 100 (/ closed total))))]
    (swap! state assoc-in [:iter-format-data :stats]
           {:total total :closed closed :percent percent})))

;; When step actions

(defn parse-iteration-edn!
  "Parse the stored EDN string using iteration/parse-iteration-edn."
  []
  (let [edn-str (:iter-edn-str @state)
        result (iteration/parse-iteration-edn edn-str)]
    (swap! state assoc :iter-parsed result)))

(defn validate-iteration!
  "Validate the stored iteration data."
  []
  (let [data (:iter-data @state)
        errors (iteration/validate-iteration data)]
    (swap! state assoc :validation-result errors)))

(defn annotate-stories!
  "Annotate stored stories with stored bead data."
  []
  (let [stories (:iter-stories @state)
        beads (:iter-beads @state)
        result (iteration/annotate-stories stories beads)]
    (swap! state assoc :iter-annotated result)))

(defn calculate-completion-stats!
  "Calculate completion stats for the annotated stories."
  []
  (let [stories (:iter-annotated @state)
        result (iteration/completion-stats stories)]
    (swap! state assoc :iter-stats result)))

(defn format-iteration!
  "Format the iteration for human display."
  []
  (let [data (:iter-format-data @state)
        result (iteration/format-iteration data)]
    (swap! state assoc :output result)))

(defn format-iteration-json!
  "Format the iteration as JSON."
  []
  (let [data (:iter-format-data @state)
        result (iteration/format-iteration-json data)]
    (swap! state assoc :iter-json-output result)))

;; Then step accessors

(defn iteration-number
  "Returns the parsed iteration number."
  []
  (:number (:iter-parsed @state)))

(defn iteration-status
  "Returns the parsed iteration status as a string."
  []
  (name (:status (:iter-parsed @state))))

(defn iteration-guardrails
  "Returns the parsed iteration guardrails."
  []
  (:guardrails (:iter-parsed @state)))

(defn iteration-notes
  "Returns the parsed iteration notes."
  []
  (:notes (:iter-parsed @state)))

(defn story-status
  "Returns the status of a specific story after annotation."
  [story-id]
  (:status (first (filter #(= story-id (:id %)) (:iter-annotated @state)))))

(defn stats-total
  "Returns the total from completion stats."
  []
  (:total (:iter-stats @state)))

(defn stats-closed
  "Returns the closed count from completion stats."
  []
  (:closed (:iter-stats @state)))

(defn stats-percent
  "Returns the completion percentage from completion stats."
  []
  (:percent (:iter-stats @state)))

(defn iter-json-output
  "Returns the JSON output from format-iteration-json."
  []
  (:iter-json-output @state))

;; --- Configuration helpers ---

;; Given step builders

(defn set-config-from-table
  "Build config map from table headers and rows."
  [headers rows]
  (let [m (reduce (fn [acc row]
                    (let [kv (zipmap headers row)]
                      (assoc acc (keyword (get kv "key")) (get kv "value"))))
                  {}
                  rows)]
    (swap! state assoc :config-map m)))

(defn set-empty-config
  "Set an empty config string for parsing."
  []
  (swap! state assoc :config-edn-str "{}"))

;; When step actions

(defn list-config!
  "Format config using config/config-list and store output."
  []
  (let [cfg (:config-map @state)
        output (config/config-list cfg)]
    (swap! state assoc :output output)))

(defn get-config-key!
  "Get a config value by key string and store result."
  [key-str]
  (let [cfg (:config-map @state)
        result (config/config-get cfg key-str)]
    (swap! state assoc :config-result result)))

(defn set-config-key!
  "Set a config value and update the stored config map."
  [key-str value-str]
  (let [cfg (:config-map @state)
        updated (config/config-set cfg key-str value-str)]
    (swap! state assoc :config-map updated)))

(defn parse-config!
  "Parse the stored config EDN string using config/parse-config."
  []
  (let [edn-str (:config-edn-str @state)
        result (config/parse-config edn-str)]
    (swap! state assoc :config-map result)))

(defn request-config-help!
  "Run config/config-help and store output."
  []
  (let [output (config/config-help)]
    (swap! state assoc :output output)))

;; Then step accessors

(defn config-result
  "Returns the result from the last config get operation."
  []
  (:config-result @state))

(defn current-config
  "Returns the current config map."
  []
  (:config-map @state))

;; --- Project status / dashboard helpers ---

;; Given step builders

(defn set-project-configs-from-table
  "Build project configs from table headers and rows."
  [headers rows]
  (let [configs (reduce (fn [acc row]
                          (let [m (zipmap headers row)
                                slug (get m "slug")
                                max-w (parse-long (get m "max-workers"))]
                            (assoc acc slug {:max-workers max-w})))
                        {}
                        rows)]
    (swap! state update :status-configs merge configs)))

(defn set-active-iterations-from-table
  "Build active iterations from table headers and rows."
  [headers rows]
  (let [iterations (reduce (fn [acc row]
                             (let [m (zipmap headers row)
                                   slug (get m "slug")]
                               (assoc acc slug {:number (get m "number")
                                                :stats {:total (parse-long (get m "total"))
                                                        :closed (parse-long (get m "closed"))
                                                        :percent (parse-long (get m "percent"))}})))
                           {}
                           rows)]
    (swap! state update :status-iterations merge iterations)))

(defn set-active-workers-from-table
  "Build active workers from table headers and rows."
  [headers rows]
  (let [workers (reduce (fn [acc row]
                          (let [m (zipmap headers row)]
                            (assoc acc (get m "slug") (parse-long (get m "count")))))
                        {}
                        rows)]
    (swap! state update :status-workers merge workers)))

(defn set-empty-registry
  "Set an empty registry for status."
  []
  (swap! state assoc :status-registry {:projects []}
                     :ready-registry {:projects []}))

(defn set-dashboard-project
  "Build a dashboard project for detail formatting from key-value table.
   Table has headers as the first key-value pair, rows as additional pairs."
  [slug headers rows]
  (let [all-pairs (cons headers rows)
        m (reduce (fn [acc [k v]] (assoc acc k v)) {} (map vec all-pairs))]
    (swap! state assoc-in [:detail-projects slug]
           {:slug slug
            :status (get m "status" "unknown")
            :workers (when-let [w (get m "workers")] (parse-long w))
            :max-workers (when-let [w (get m "max-workers")] (parse-long w))})))

(defn set-project-iteration
  "Set iteration data for a detail project from key-value table."
  [slug headers rows]
  (let [all-pairs (cons headers rows)
        m (reduce (fn [acc [k v]] (assoc acc k v)) {} (map vec all-pairs))]
    (swap! state update-in [:detail-projects slug]
           assoc :iteration {:number (get m "number")
                             :stats {:total (parse-long (get m "total"))
                                     :closed (parse-long (get m "closed"))
                                     :percent (parse-long (get m "percent"))}})))

(defn set-project-stories
  "Set stories for a detail project from table data."
  [slug headers rows]
  (let [stories (mapv (fn [row]
                        (let [m (zipmap headers row)]
                          {:id (get m "id")
                           :title (get m "title")
                           :status (get m "status")}))
                      rows)]
    (swap! state update-in [:detail-projects slug :iteration] assoc :stories stories)))

(defn clear-project-iteration
  "Remove iteration from a detail project."
  [slug]
  (swap! state update-in [:detail-projects slug] dissoc :iteration))

;; When step actions

(defn build-dashboard!
  "Build dashboard from accumulated status state."
  []
  (let [{:keys [status-registry ready-registry status-configs status-iterations status-workers]} @state
        registry (if (seq (:projects status-registry))
                   status-registry
                   ready-registry)
        result (status/build-dashboard registry status-configs status-iterations status-workers)]
    (swap! state assoc :dashboard result)))

(defn format-project-detail!
  "Format a specific project detail and store output."
  [slug]
  (let [project (get-in @state [:detail-projects slug])
        result (status/format-project-detail project)]
    (swap! state assoc :output result)))

(defn format-dashboard!
  "Format the dashboard for human-readable output."
  []
  (let [dash (:dashboard @state)
        result (status/format-dashboard dash)]
    (swap! state assoc :output result)))

(defn format-dashboard-json!
  "Format the dashboard as JSON."
  []
  (let [dash (:dashboard @state)
        result (status/format-dashboard-json dash)
        parsed (json/parse-string result true)]
    (swap! state assoc :dashboard-json-data parsed :output result :list-json-output result)))

;; Then step accessors

(defn dashboard
  "Returns the dashboard data."
  []
  (:dashboard @state))

(defn dashboard-project
  "Find a project by slug in the dashboard."
  [slug]
  (first (filter #(= slug (:slug %)) (:projects (:dashboard @state)))))

(defn dashboard-json
  "Returns the parsed dashboard JSON data."
  []
  (:dashboard-json-data @state))

(defn dashboard-json-project
  "Find a project by slug in the dashboard JSON. Returns string-keyed map."
  [slug]
  (let [json-str (:output @state)
        parsed (json/parse-string json-str)]
    (first (filter #(= slug (get % "slug")) (get parsed "projects")))))

;; --- Orch runner helpers ---

;; Given step builders

(defn set-spawn-entry
  "Set the spawn entry map."
  [entry]
  (swap! state assoc :runner-spawn-entry entry))

(defn spawn-entry
  "Returns the current spawn entry."
  []
  (:runner-spawn-entry @state))

(defn update-spawn-entry
  "Merge additional fields into the spawn entry."
  [fields]
  (swap! state update :runner-spawn-entry merge fields))

(defn set-worker-agent
  "Set the worker agent on the spawn entry."
  [agent]
  (swap! state assoc-in [:runner-spawn-entry :worker-agent] agent))

(defn set-cli-args
  "Store CLI args for parsing."
  [args]
  (swap! state assoc :runner-cli-args args))

(defn cli-args
  "Returns the stored CLI args."
  []
  (:runner-cli-args @state))

(defn set-spawn-tick-result
  "Build a spawn tick result with beads."
  [count beads]
  (let [spawns (mapv (fn [b] {:bead b :worker-agent nil}) beads)]
    (swap! state assoc :runner-tick-result {:action "spawn" :spawns spawns})))

(defn add-spawn-beads
  "Add bead entries to the spawn tick result."
  [beads]
  (let [spawns (mapv (fn [b] {:bead b :worker-agent nil}) beads)]
    (swap! state assoc-in [:runner-tick-result :spawns] spawns)))

(defn set-idle-tick-result
  "Build an idle tick result with a reason."
  [reason]
  (swap! state assoc :runner-tick-result {:action "idle" :reason reason}))

(defn set-zombie-sessions
  "Build zombie session data."
  [count reasons]
  (let [zombies (mapv (fn [r] {:bead (str "z-" r) :reason r}) reasons)]
    (swap! state assoc :runner-zombies zombies)))

;; When step actions

(defn build-worker-task!
  "Build the worker task from the spawn entry."
  []
  (let [entry (:runner-spawn-entry @state)
        task (orch-runner/build-worker-task entry)]
    (swap! state assoc :runner-worker-task task)))

(defn worker-task
  "Returns the built worker task."
  []
  (:runner-worker-task @state))

(defn build-worker-args!
  "Build the worker args from the spawn entry.
   Also extracts the session ID from args for assert-session-id compatibility."
  []
  (let [entry (:runner-spawn-entry @state)
        config (:runner-config @state)
        args (orch-runner/build-worker-args config entry)
        sid-idx (when args (.indexOf ^java.util.List args "--session-id"))
        session-id (when (and sid-idx (>= sid-idx 0) (< (inc sid-idx) (count args)))
                     (nth args (inc sid-idx)))]
    (swap! state assoc :runner-worker-args args
           :session-id-result session-id)))

(defn worker-args
  "Returns the built worker args."
  []
  (:runner-worker-args @state))

(defn parse-cli-args!
  "Parse the stored CLI args."
  []
  (let [args (:runner-cli-args @state)
        result (orch-runner/parse-cli-args args)]
    (swap! state assoc :runner-parsed-cli-args result)))

(defn parsed-cli-args
  "Returns the parsed CLI args result."
  []
  (:runner-parsed-cli-args @state))

(defn format-spawn-log!
  "Format the spawn log from the tick result."
  []
  (let [result (:runner-tick-result @state)
        log (orch-runner/format-spawn-log result)]
    (swap! state assoc :runner-log log)))

(defn format-idle-log!
  "Format the idle log from the tick result."
  []
  (let [result (:runner-tick-result @state)
        log (orch-runner/format-idle-log result)]
    (swap! state assoc :runner-log log)))

(defn format-zombie-log!
  "Format the zombie log."
  []
  (let [zombies (:runner-zombies @state)
        log (orch-runner/format-zombie-log zombies)]
    (swap! state assoc :runner-log log)))

;; Then step accessors

(defn runner-tick-result
  "Returns the runner tick result."
  []
  (:runner-tick-result @state))

(defn runner-zombies
  "Returns the runner zombies."
  []
  (:runner-zombies @state))

(defn runner-log
  "Returns the runner log lines."
  []
  (:runner-log @state))

;; --- Orch output helpers ---

(defn configure-projects-from-table
  "Set up projects from table data with columns: slug, status, priority,
   max-workers, active-iteration, active-workers."
  [headers rows]
  (doseq [row rows]
    (let [m (zipmap headers row)
          slug (get m "slug")
          status (keyword (get m "status"))
          priority (keyword (get m "priority"))
          max-workers (parse-long (get m "max-workers"))
          active-iteration (get m "active-iteration")
          active-workers (parse-long (get m "active-workers"))]
      ;; Add to registry
      (swap! state update-in [:registry :projects] conj
             {:slug slug :status status :priority priority :path (str "/projects/" slug)})
      ;; Set config
      (swap! state assoc-in [:configs slug]
             {:status status :max-workers max-workers})
      ;; Set active iteration (if non-empty)
      (when (and active-iteration (seq active-iteration))
        (swap! state assoc-in [:iterations slug] active-iteration))
      ;; Set active workers
      (swap! state assoc-in [:workers slug] active-workers))))

(defn set-project-beads
  "Set beads with id, title, status for a project.
   Stores in :open-beads for format-debug-output (all non-closed beads shown)
   and also sets :beads for ready beads (only ready ones used by tick)."
  [slug headers rows]
  (let [beads (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:id (get m "id")
                         :title (get m "title")
                         :status (get m "status")}))
                    rows)
        ready-beads (filterv #(= "ready" (:status %)) beads)]
    (swap! state assoc-in [:open-beads slug] beads)
    (swap! state assoc-in [:beads slug] ready-beads)))

(defn orch-tick-with-output!
  "Run orch/tick with all accumulated state, store the result,
   then format debug output and store it."
  []
  (let [{:keys [registry configs iterations beads workers open-beads]} @state
        result (orch/tick registry configs iterations beads workers {})
        all-open-beads (or open-beads {})
        output (orch/format-debug-output registry configs iterations all-open-beads result workers)]
    (swap! state assoc :tick-result result :tick-output output :output output)))

(defn tick-output
  "Returns the formatted tick output."
  []
  (:tick-output @state))

(defn output-contains-line?
  "Returns true if any line in the output contains the given text as a substring."
  [text]
  (when-let [output (:tick-output @state)]
    (some #(str/includes? % text) (str/split-lines output))))

(defn output-contains?
  "Returns true if the text appears anywhere in the tick output."
  [text]
  (when-let [output (:tick-output @state)]
    (str/includes? output text)))

(defn output-has-before?
  "Returns true if text a appears before text b in the tick output."
  [a b]
  (when-let [output (:tick-output @state)]
    (let [idx-a (str/index-of output a)
          idx-b (str/index-of output b)]
      (and idx-a idx-b (< idx-a idx-b)))))
