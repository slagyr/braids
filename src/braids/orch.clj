(ns braids.orch
  (:require [clojure.string]
            [cheshire.core :as json]
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
      {:action "idle" :reason "no-active-iterations" :disable-cron true}
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
                             timeout (or (:worker-timeout cfg) (:worker-timeout pc/defaults))
                             agent-id (:worker-agent cfg)]
                         (map (fn [bead]
                                (cond-> {:project slug
                                         :bead (:id bead)
                                         :iteration iteration
                                         :channel channel
                                         :path path
                                         :label (str "project:" slug ":" (:id bead))
                                         :worker-timeout timeout}
                                  agent-id (assoc :worker-agent agent-id)))
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
          {:action "idle" :reason "all-at-capacity" :disable-cron false}

          :else
          {:action "idle" :reason "no-ready-beads" :disable-cron false})))))

(defn- parse-project-label
  "Parse a project: label into [slug bead-id] or nil."
  [label]
  (when (and label (.startsWith label "project:"))
    (let [parts (clojure.string/split label #":" 3)]
      (when (= 3 (count parts))
        [(nth parts 1) (nth parts 2)]))))

(def ^:private ended-statuses #{"completed" "failed" "error" "stopped"})

(defn detect-zombies
  "Pure zombie detection. Given session info, project configs, and a map of
   bead-id->status-string, returns a vector of zombie entries.
   Each session is {:label :status :age-seconds}.
   Zombie reasons: session-ended, bead-closed, timeout."
  [sessions configs bead-statuses]
  (->> sessions
       (keep (fn [{:keys [label status age-seconds]}]
               (when-let [[slug bead-id] (parse-project-label label)]
                 (let [cfg (get configs slug {})
                       timeout (or (:worker-timeout cfg) (:worker-timeout pc/defaults))
                       bead-status (get bead-statuses bead-id "open")
                       zombie-entry {:slug slug :bead bead-id :label label}]
                   (cond
                     ;; Session ended — always a zombie
                     (contains? ended-statuses status)
                     (assoc zombie-entry :reason "session-ended")

                     ;; Bead closed but session still running
                     (= "closed" bead-status)
                     (assoc zombie-entry :reason "bead-closed")

                     ;; Session exceeded timeout with open bead
                     (and (> age-seconds timeout) (= "open" bead-status))
                     (assoc zombie-entry :reason "timeout")

                     :else nil)))))
       vec))

(def worker-instruction
  "You are a project worker for the braids skill. Read and follow ~/.openclaw/skills/braids/references/worker.md")

(defn spawn-msg
  "Generate the spawn message string from a spawn entry (as returned by tick).
   Includes worker.md instruction prefix followed by Project/Bead/Iteration/Channel."
  [{:keys [path bead iteration channel]}]
  (str worker-instruction "\n\n"
       "Project: " path "\n"
       "Bead: " bead "\n"
       "Iteration: " iteration "\n"
       "Channel: " channel))

(defn format-spawn-msg-json
  "Format spawn message as JSON with fields matching sessions_spawn parameters:
   task, label, runTimeoutSeconds, cleanup, thinking."
  [{:keys [label worker-timeout] :as spawn}]
  (json/generate-string
   {:task (spawn-msg spawn)
    :label label
    :runTimeoutSeconds worker-timeout
    :cleanup "delete"
    :thinking "low"}))

(defn format-tick-json
  "Format tick result as JSON string."
  [result]
  (json/generate-string result {:key-fn #(-> % name (.replace "-" "_"))}))

(defn format-orch-run-json
  "Format tick result as JSON with spawns pre-formatted for sessions_spawn.
   Idle results pass through. Spawn results have each spawn entry replaced
   with the full sessions_spawn parameters (task, label, runTimeoutSeconds, etc.).
   If a spawn has :worker-agent, includes agentId in the output.
   If tick-result contains :zombies, includes them in the output."
  [tick-result]
  (let [zombies (:zombies tick-result)
        base (if (= "spawn" (:action tick-result))
               (let [formatted-spawns (mapv (fn [spawn]
                                              (cond-> {:task (spawn-msg spawn)
                                                       :label (:label spawn)
                                                       :runTimeoutSeconds (:worker-timeout spawn)
                                                       :cleanup "delete"
                                                       :thinking "low"}
                                                (:worker-agent spawn) (assoc :agentId (:worker-agent spawn))))
                                            (:spawns tick-result))]
                 {:action "spawn" :spawns formatted-spawns})
               (into {} (map (fn [[k v]] [(-> (name k) (.replace "-" "_")) v])
                             (dissoc tick-result :zombies))))]
    (json/generate-string (cond-> base
                            (seq zombies) (assoc :zombies zombies)))))
