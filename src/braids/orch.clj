(ns braids.orch
  (:require [cheshire.core :as json]
            [braids.project-config :as pc]))

(def priority-order {:high 0 :normal 1 :low 2})

(defn- eligible-projects
  "Returns active projects that have active iterations and aren't paused by config."
  [registry configs iterations]
  (->> (:projects registry)
       (filter #(= :active (:status %)))
       (filter (fn [{:keys [slug]}]
                 (let [cfg (get configs slug)]
                   (or (nil? cfg) (= :active (:status cfg))))))
       (filter (fn [{:keys [slug]}]
                 (contains? iterations slug)))
       (sort-by #(get priority-order (:priority %) 1))))

(defn no-ready-beads-projects
  "Returns slugs of projects that have active iterations, capacity, but no ready beads."
  [registry configs iterations beads workers]
  (->> (eligible-projects registry configs iterations)
       (filter (fn [{:keys [slug]}]
                 (let [cfg (get configs slug)
                       max-w (or (:max-workers cfg) 1)
                       current-w (get workers slug 0)]
                   (and (< current-w max-w)
                        (empty? (get beads slug []))))))
       (map :slug)
       vec))

(defn tick
  "Pure orchestrator tick. Given registry, project configs, active iterations map
   (slug->iteration-number), ready beads per project, worker counts, and notification configs,
   returns a decision: spawn list or idle with reason."
  [registry configs iterations beads workers _notifications]
  (let [eligible (eligible-projects registry configs iterations)]
    (if (empty? eligible)
      {:action "idle" :reason "no-active-iterations"}
      ;; Build spawn list: for each eligible project with capacity and beads
      (let [spawns (vec
                    (mapcat
                     (fn [{:keys [slug path]}]
                       (let [cfg (get configs slug)
                             max-w (or (:max-workers cfg) 1)
                             current-w (get workers slug 0)
                             available (- max-w current-w)
                             project-beads (get beads slug [])
                             to-spawn (take available project-beads)
                             iteration (get iterations slug)
                             channel (or (:channel cfg) "")
                             timeout (or (:worker-timeout cfg) (:worker-timeout pc/defaults))]
                         (map (fn [bead]
                                {:project slug
                                 :bead (:id bead)
                                 :iteration iteration
                                 :channel channel
                                 :path path
                                 :label (str "project:" slug ":" (:id bead))
                                 :worker-timeout timeout})
                              to-spawn)))
                     eligible))
            ;; Determine if any projects had beads but were at capacity
            any-at-capacity (some (fn [{:keys [slug]}]
                                    (let [cfg (get configs slug)
                                          max-w (or (:max-workers cfg) 1)
                                          current-w (get workers slug 0)]
                                      (and (>= current-w max-w)
                                           (seq (get beads slug [])))))
                                  eligible)]
        (cond
          (seq spawns)
          {:action "spawn" :spawns spawns}

          ;; Check: are all eligible projects at capacity with beads?
          any-at-capacity
          {:action "idle" :reason "all-at-capacity"}

          :else
          {:action "idle" :reason "no-ready-beads"})))))

(defn spawn-msg
  "Generate the spawn message string from a spawn entry (as returned by tick).
   Format: Project/Bead/Iteration/Channel, one per line."
  [{:keys [path bead iteration channel]}]
  (str "Project: " path "\n"
       "Bead: " bead "\n"
       "Iteration: " iteration "\n"
       "Channel: " channel))

(defn format-spawn-msg-json
  "Format spawn message as JSON with message, label, and worker_timeout fields."
  [{:keys [label worker-timeout] :as spawn}]
  (json/generate-string
   {:message (spawn-msg spawn)
    :label label
    :worker_timeout worker-timeout}))

(defn format-tick-json
  "Format tick result as JSON string."
  [result]
  (json/generate-string result {:key-fn #(-> % name (.replace "-" "_"))}))
