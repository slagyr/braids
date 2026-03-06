(ns braids.features.harness
  "Test harness for generated feature specs. Manages mutable test state
   and provides helpers that generated code calls."
  (:require [braids.orch :as orch]))

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
                               :parsed-bead-id nil}))

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
