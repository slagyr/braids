(ns braids.features.harness
  "Test harness for generated feature specs. Manages mutable test state
   and provides helpers that generated code calls."
  (:require [braids.orch :as orch]
            [braids.init :as init]
            [braids.new :as new-proj]
            [braids.list :as list]
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
                               :list-json-output nil}))

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
  "Run orch/tick with all accumulated state, store the result."
  []
  (let [{:keys [registry configs iterations beads workers]} @state
        result (orch/tick registry configs iterations beads workers {})]
    (swap! state assoc :tick-result result)))

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
