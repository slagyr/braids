(ns braids.orch-io
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.string :as str]
            [braids.orch :as orch]
            [braids.ready-io :as rio]
            [braids.config-io :as config-io]
            [braids.sys :as sys]))

(defn parse-iteration-status-edn
  "Extract the status from iteration.edn content string.
   Returns the status string (lowercase) or nil."
  [content]
  (try
    (when-let [parsed (edn/read-string content)]
      (when-let [s (:status parsed)]
        (name s)))
    (catch Exception _ nil)))

(defn find-active-iteration
  "Scan .braids/iterations/*/iteration.edn for one with :status :active.
   Returns the iteration number (directory name) or nil."
  [project-path]
  (let [path (if (str/starts-with? project-path "~/")
               (str (fs/expand-home "~") "/" (subs project-path 2))
               project-path)
        iter-dir (cond
                   (fs/directory? (str path "/.braids/iterations")) (str path "/.braids/iterations")
                   (fs/directory? (str path "/.project/iterations")) (str path "/.project/iterations")
                   :else (str path "/.braids/iterations"))]
    (when (fs/exists? iter-dir)
      (some (fn [dir]
              (let [iter-edn (str dir "/iteration.edn")]
                (when (fs/exists? iter-edn)
                  (let [content (slurp iter-edn)]
                    (when (= "active" (parse-iteration-status-edn content))
                      (fs/file-name dir))))))
            (sort (fs/list-dir iter-dir))))))

(defn load-bead-statuses
  "Load all bead statuses for a project using `bd list --json`.
   Returns a map of bead-id -> status-string (e.g. open, closed)."
  [project-path]
  (let [path (if (str/starts-with? project-path "~/")
               (str (fs/expand-home "~") "/" (subs project-path 2))
               project-path)
        cfg (config-io/load-config)
        bin (or (System/getenv "BD_BIN") (sys/bd-bin cfg))]
    (try
      (let [result (proc/shell {:dir path :out :string :err :string
                                :extra-env (sys/subprocess-env cfg)}
                               bin "list" "--json")
            parsed (json/parse-string (:out result) true)]
        (if (sequential? parsed)
          (into {} (map (fn [b] [(:id b) (str/lower-case (or (:status b) "open"))]) parsed))
          {}))
      (catch Exception _ {}))))

(defn load-open-beads
  "Load open (non-closed) beads for a project using `bd list --json`.
   Returns a vector of bead maps that are not closed."
  [project-path]
  (let [path (if (str/starts-with? project-path "~/")
               (str (fs/expand-home "~") "/" (subs project-path 2))
               project-path)
        cfg (config-io/load-config)
        bin (or (System/getenv "BD_BIN") (sys/bd-bin cfg))]
    (try
      (let [result (proc/shell {:dir path :out :string :err :string
                                :extra-env (sys/subprocess-env cfg)}
                               bin "list" "--json")
            parsed (json/parse-string (:out result) true)]
        (if (sequential? parsed)
          (vec (filter #(not= "closed" (str/lower-case (or (:status %) "open"))) parsed))
          []))
      (catch Exception _ []))))

(defn- parse-session-labels
  "Parse session info from JSON. Accepts either:
   - Array of strings (labels only, no zombie detection possible for bead-closed/timeout)
   - Array of objects with :label, :status, :ageSeconds"
  [json-str]
  (try
    (let [parsed (json/parse-string json-str true)]
      (if (string? (first parsed))
        ;; Plain labels — wrap in maps
        (mapv (fn [l] {:label l :status "running" :age-seconds 0}) parsed)
        ;; Full session objects
        (mapv (fn [s] (cond-> {:label (:label s)
                               :status (:status s)
                               :age-seconds (or (:ageSeconds s) (:age-seconds s) (:age_seconds s) 0)}
                        (:sessionId s) (assoc :session-id (:sessionId s))))
              parsed)))
    (catch Exception _ [])))

;; ── Shared IO-loading helpers ──

(defn- load-project-data
  "Load all project data needed for an orch tick.
   Returns a map with :reg, :active-projects, :configs, :iterations,
   :beads, :notifications, and :open-beads."
  []
  (let [home (rio/resolve-state-home)
        reg (rio/load-registry home)
        active-projects (filter #(= :active (:status %)) (:projects reg))
        configs (into {} (map (fn [{:keys [slug path]}]
                                [slug (rio/load-project-config path)])
                              active-projects))
        iterations (into {} (keep (fn [{:keys [slug path]}]
                                    (when-let [iter (find-active-iteration path)]
                                      [slug iter]))
                                  active-projects))
        beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (rio/load-ready-beads path)
                                      [])])
                            active-projects))
        notifications (into {} (map (fn [{:keys [slug]}]
                                      [slug (select-keys (get configs slug)
                                                         [:notifications :notification-mentions])])
                                    active-projects))
        open-beads (into {} (map (fn [{:keys [slug path]}]
                                   [slug (if (contains? iterations slug)
                                           (load-open-beads path)
                                           [])])
                                 active-projects))]
    {:reg reg :active-projects active-projects :configs configs
     :iterations iterations :beads beads :notifications notifications
     :open-beads open-beads}))

(defn- tick-with-data
  "Run orch/tick with loaded project data and worker labels."
  [{:keys [reg configs iterations beads notifications open-beads]} worker-labels]
  (orch/tick reg configs iterations beads (rio/count-workers worker-labels) notifications open-beads))

(defn- load-bead-statuses-for-projects
  "Batch load bead statuses only for projects that have active sessions."
  [projects-with-sessions active-projects]
  (if (empty? projects-with-sessions)
    {}
    (reduce (fn [acc {:keys [slug path]}]
              (if (contains? projects-with-sessions slug)
                (merge acc (load-bead-statuses path))
                acc))
            {} active-projects)))

(defn- extract-project-slugs-from-labels
  "Extract project slugs from session labels (strings like 'project:slug:bead')."
  [labels]
  (set (keep (fn [label]
               (let [parts (str/split (if (map? label) (:label label) label) #":" 3)]
                 (when (>= (count parts) 2) (second parts))))
             labels)))

(defn- filter-zombie-labels
  "Remove zombie labels from a label list, returning clean labels."
  [labels zombies]
  (let [zombie-labels (set (map :label zombies))]
    (vec (remove zombie-labels labels))))

(defn- wrap-debug
  "Wrap a tick result with debug context from loaded project data."
  [result {:keys [reg configs iterations open-beads beads] :as _data} workers]
  {:result result
   :debug-ctx {:registry reg :configs configs :iterations iterations
               :open-beads open-beads :ready-beads beads :workers workers}})

;; ── Public API ──

(defn gather-and-tick
  "Full IO pipeline for orch tick: load everything, compute spawn decisions."
  ([] (gather-and-tick {}))
  ([{:keys [state-home session-labels]
     :or {session-labels []}}]
   (let [data (load-project-data)]
     (tick-with-data data session-labels))))

(defn gather-and-tick-with-zombies
  "Enhanced orch pipeline with zombie detection. Accepts session-info as JSON string.
   Returns tick result with :zombies key included."
  [session-info-json]
  (let [sessions (parse-session-labels session-info-json)
        labels (mapv :label sessions)
        data (load-project-data)
        project-labels (filter #(str/starts-with? (:label %) "project:") sessions)
        projects-with-sessions (extract-project-slugs-from-labels project-labels)
        bead-statuses (load-bead-statuses-for-projects projects-with-sessions (:active-projects data))
        zombies (orch/detect-zombies sessions (:configs data) bead-statuses)
        clean-labels (filter-zombie-labels labels zombies)
        tick-result (tick-with-data data clean-labels)]
    (cond-> tick-result
      (seq zombies) (assoc :zombies zombies))))

(defn gather-and-tick-from-session-labels
  "Simplified orch pipeline accepting space-separated session labels.
   Detects bead-closed zombies only (no session status/age available).
   Returns tick result with :zombies key if any found."
  [sessions-str]
  (let [labels (orch/parse-session-labels-string sessions-str)
        data (load-project-data)
        projects-with-sessions (extract-project-slugs-from-labels labels)
        bead-statuses (load-bead-statuses-for-projects projects-with-sessions (:active-projects data))
        zombies (orch/detect-zombies-from-labels labels bead-statuses)
        clean-labels (filter-zombie-labels labels zombies)
        tick-result (tick-with-data data clean-labels)]
    (cond-> tick-result
      (seq zombies) (assoc :zombies zombies))))

(defn load-sessions-from-stores
  "Read all agent session store files under openclaw-home/agents/*/sessions/sessions.json.
   Returns a vector of maps with :label, :status, :age-seconds for sessions
   that have a project: label."
  ([] (load-sessions-from-stores (str (System/getProperty "user.home") "/.openclaw")))
  ([openclaw-home]
   (let [agents-dir (str openclaw-home "/agents")]
     (if-not (fs/exists? agents-dir)
       []
       (let [now (System/currentTimeMillis)]
         (->> (fs/list-dir agents-dir)
              (mapcat (fn [agent-dir]
                        (let [store-path (str agent-dir "/sessions/sessions.json")]
                          (if-not (fs/exists? store-path)
                            []
                            (try
                              (let [store (json/parse-string (slurp store-path) true)]
                                (->> store
                                     (keep (fn [[_key session]]
                                       (let [label (or (:label session) "")
                                             session-id-val (or (:sessionId session) (name _key))
                                             age (long (/ (- now (or (:updatedAt session) now)) 1000))]
                                         (cond
                                           ;; Match by project: label
                                           (str/starts-with? label "project:")
                                           (cond-> {:label label
                                                    :status "running"
                                                    :age-seconds age}
                                             (:sessionId session) (assoc :session-id (:sessionId session)))

                                           ;; Match by deterministic worker session-id pattern
                                           (orch/parse-worker-session-id session-id-val)
                                           {:label label
                                            :status "running"
                                            :age-seconds age
                                            :session-id session-id-val
                                            :worker-bead-id (orch/parse-worker-session-id session-id-val)}))))
                                     vec))
                              (catch Exception _ []))))))
              vec))))))

(defn- build-synthetic-labels
  "Build synthetic project labels from session-id matched worker sessions."
  [sessions active-projects]
  (keep (fn [{:keys [worker-bead-id]}]
          (when worker-bead-id
            (let [sorted-projects (sort-by #(- (count (:slug %))) active-projects)]
              (some (fn [{:keys [slug]}]
                      (when (str/starts-with? worker-bead-id (str slug "-"))
                        (str "project:" slug ":" worker-bead-id)))
                    sorted-projects))))
        sessions))

(defn gather-and-tick-from-stores
  "Full orch pipeline that reads session info directly from openclaw session stores.
   No external session input needed. Returns tick result with :zombies."
  ([] (gather-and-tick-from-stores (str (System/getProperty "user.home") "/.openclaw")))
  ([openclaw-home]
   (let [sessions (load-sessions-from-stores openclaw-home)
         labels (mapv :label sessions)
         data (load-project-data)
         projects-with-sessions (extract-project-slugs-from-labels sessions)
         bead-statuses (load-bead-statuses-for-projects projects-with-sessions (:active-projects data))
         zombies (orch/detect-zombies sessions (:configs data) bead-statuses)
         clean-labels (filter-zombie-labels labels zombies)
         sid-labels (build-synthetic-labels sessions (:active-projects data))
         all-labels (into clean-labels sid-labels)
         tick-result (tick-with-data data all-labels)]
     (cond-> tick-result
       (seq zombies) (assoc :zombies zombies)))))

(defn gather-and-tick-from-stores-debug
  "Like gather-and-tick-from-stores but returns {:result :debug-ctx}."
  ([] (gather-and-tick-from-stores-debug (str (System/getProperty "user.home") "/.openclaw")))
  ([openclaw-home]
   (let [sessions (load-sessions-from-stores openclaw-home)
         labels (mapv :label sessions)
         data (load-project-data)
         projects-with-sessions (extract-project-slugs-from-labels sessions)
         bead-statuses (load-bead-statuses-for-projects projects-with-sessions (:active-projects data))
         zombies (orch/detect-zombies sessions (:configs data) bead-statuses)
         clean-labels (filter-zombie-labels labels zombies)
         sid-labels (build-synthetic-labels sessions (:active-projects data))
         all-labels (into clean-labels sid-labels)
         workers (rio/count-workers all-labels)
         tick-result (orch/tick (:reg data) (:configs data) (:iterations data)
                               (:beads data) workers (:notifications data) (:open-beads data))
         result (cond-> tick-result
                  (seq zombies) (assoc :zombies zombies))]
     (wrap-debug result data workers))))
