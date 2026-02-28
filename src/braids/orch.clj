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
   (slug->iteration-number), ready beads per project, worker counts, notification configs,
   and optionally open-beads (slug->all-open-beads including blocked),
   returns a decision: spawn list or idle with reason.
   When open-beads is provided: disable-cron is true if no project has workable (non-blocked) open beads.
   When open-beads is nil (backward compat): disable-cron false for no-ready-beads."
  ([registry configs iterations beads workers notifications]
   (tick registry configs iterations beads workers notifications nil))
  ([registry configs iterations beads workers _notifications open-beads]
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
                                   eligible)
             ;; Check if any eligible project has workable (non-blocked) open beads.
             ;; Blocked beads need human intervention, not more orchestrator polling,
             ;; so they should not keep the cron alive.
             has-open-beads (if (nil? open-beads)
                              true ;; backward compat: assume open beads exist
                              (some (fn [{:keys [slug]}]
                                      (seq (remove #(= "blocked" (some-> (:status %) name clojure.string/lower-case))
                                                   (get open-beads slug []))))
                                    eligible))]
         (cond
           (seq spawns)
           {:action "spawn" :spawns spawns}

           ;; Check: are all eligible projects at capacity with beads?
           any-at-capacity
           {:action "idle" :reason "all-at-capacity" :disable-cron false}

           :else
           {:action "idle" :reason "no-ready-beads"
            :disable-cron (not (boolean has-open-beads))}))))))

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
       (keep (fn [{:keys [label status age-seconds session-id]}]
               (when-let [[slug bead-id] (parse-project-label label)]
                 (let [cfg (get configs slug {})
                       timeout (or (:worker-timeout cfg) (:worker-timeout pc/defaults))
                       bead-status (get bead-statuses bead-id "open")
                       zombie-entry (cond-> {:slug slug :bead bead-id :label label}
                                       session-id (assoc :session-id session-id))]
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

(defn parse-session-labels-string
  "Parse a space-separated string of session labels into a vector of project: labels."
  [s]
  (if (or (nil? s) (clojure.string/blank? s))
    []
    (->> (clojure.string/split (clojure.string/trim s) #"\s+")
         (filterv #(clojure.string/starts-with? % "project:")))))

(defn detect-zombies-from-labels
  "Lightweight zombie detection from plain labels (no session status/age).
   Only detects bead-closed zombies. Returns vector of zombie entries."
  [labels bead-statuses]
  (->> labels
       (keep (fn [label]
               (when-let [[slug bead-id] (parse-project-label label)]
                 (when (= "closed" (get bead-statuses bead-id "open"))
                   {:slug slug :bead bead-id :label label :reason "bead-closed"}))))
       vec))

;; ANSI color codes for debug output
(def ^:private ansi
  {:bold-white "\033[1;37m"
   :green      "\033[32m"
   :red        "\033[31m"
   :yellow     "\033[33m"
   :cyan       "\033[36m"
   :dim        "\033[2m"
   :reset      "\033[0m"})

(defn- no-color? []
  (some? (System/getenv "NO_COLOR")))

(defn- c
  "Wrap text in ANSI color. Returns plain text if NO_COLOR is set."
  [text color]
  (if (no-color?)
    (str text)
    (str (get ansi color "") text (:reset ansi))))

(defn- bead-status-icon [status]
  (case status
    "blocked" "🚫"
    ("in-progress" "in_progress") "⚙️"
    "closed" "✓"
    "○"))

(defn- bead-status-color [status]
  (case status
    "blocked" :red
    "closed" :dim
    ("open" "in-progress" "in_progress") :green
    :green))

(defn- config-status-color [status]
  (case (some-> status name clojure.string/lower-case)
    "active" :green
    ("paused" "blocked") :red
    :green))

(defn- decision-color [action reason]
  (case action
    "spawn" :green
    "idle" (case reason
             ("no-active-iterations") :red
             ("no-ready-beads" "all-at-capacity") :yellow
             :red)
    :yellow))

(defn format-debug-output
  "Format human-readable debug output for orch-run. Pure function.
   Takes registry, configs, iterations map, open-beads map (slug->[bead-maps]),
   and tick-result. Returns a multi-line string for stderr."
  [registry configs iterations open-beads tick-result]
  (let [active-projects (->> (:projects registry)
                             (filter #(= :active (:status %)))
                             (filter (fn [{:keys [slug]}]
                                       (let [cfg (get configs slug)]
                                         (or (nil? cfg) (= :active (:status cfg))))))
                             (sort-by #(get priority-order (:priority %) 1)))
        project-lines
        (mapv (fn [{:keys [slug]}]
                (let [cfg (get configs slug)
                      status (or (:status cfg) :active)
                      iter (get iterations slug)
                      beads (get open-beads slug [])
                      status-str (c (name status) (config-status-color status))
                      name-str (c slug :bold-white)
                      iter-str (if iter
                                 (str "iteration " (c iter :cyan))
                                 "(no iteration)")]
                  (if (empty? beads)
                    (if iter
                      (str "  " name-str "  " status-str "  " iter-str "  → all closed ✓")
                      (str "  " name-str "  " status-str "  " iter-str))
                    (let [blocked (count (filter #(= "blocked" (some-> (:status %) name clojure.string/lower-case)) beads))
                          summary (if (pos? blocked)
                                    (str "→ " (count beads) " beads (" blocked " blocked)")
                                    (str "→ " (count beads) " beads"))
                          header (str "  " name-str "  " status-str "  " iter-str "  " summary)
                          bead-lines (mapv (fn [b]
                                            (let [bs (or (some-> (:status b) name clojure.string/lower-case) "open")
                                                  icon (bead-status-icon bs)
                                                  id-suffix (last (clojure.string/split (:id b) #"-"))
                                                  colored-status (c bs (bead-status-color bs))]
                                              (str "    " icon " " id-suffix "  " colored-status)))
                                          beads)]
                      (clojure.string/join "\n" (cons header bead-lines))))))
              active-projects)
        decision-line (let [{:keys [action reason disable-cron]} tick-result
                            spawns (:spawns tick-result)
                            desc (if (= "spawn" action)
                                   (str "spawn: " (count spawns) " worker(s)")
                                   (str action ": " reason))
                            cron-note (when (some? disable-cron)
                                        (str "  [disable_cron: " disable-cron "]"))
                            color (decision-color action reason)]
                        (str "\n  → " (c (str desc cron-note) color)))]
    (str (clojure.string/join "\n" project-lines) "\n" decision-line "\n")))

(defn format-tick-json
  "Format tick result as JSON string."
  [result]
  (json/generate-string result {:key-fn #(-> % name (.replace "-" "_"))}))

(defn format-orch-run-json
  "Format tick result as JSON with spawns containing structural data only.
   Idle results pass through. Spawn results include project, bead, iteration,
   channel, path, label, runTimeoutSeconds, cleanup, thinking, and optionally agentId.
   The orchestrator agent constructs the task message itself using the template
   in orchestrator.md. If tick-result contains :zombies, includes them in the output."
  [tick-result]
  (let [zombies (:zombies tick-result)
        base (if (= "spawn" (:action tick-result))
               (let [formatted-spawns (mapv (fn [spawn]
                                              (cond-> {:project (:project spawn)
                                                       :bead (:bead spawn)
                                                       :iteration (:iteration spawn)
                                                       :channel (:channel spawn)
                                                       :path (:path spawn)
                                                       :label (:label spawn)
                                                       :runTimeoutSeconds (:worker-timeout spawn)
                                                       :cleanup "delete"
                                                       :thinking "low"}
                                                (:worker-agent spawn) (assoc :agentId (:worker-agent spawn))))
                                            (:spawns tick-result))]
                 {:action "spawn" :spawns formatted-spawns})
               (into {} (map (fn [[k v]] [(-> (name k) (.replace "-" "_")) v])
                             (dissoc tick-result :zombies))))]
    (let [formatted-zombies (when (seq zombies)
                               (mapv (fn [z]
                                       (cond-> {:slug (:slug z)
                                                :bead (:bead z)
                                                :label (:label z)
                                                :reason (:reason z)}
                                         (:session-id z) (assoc :sessionId (:session-id z))))
                                     zombies))]
      (json/generate-string (cond-> base
                              formatted-zombies (assoc :zombies formatted-zombies))))))
