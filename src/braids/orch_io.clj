(ns braids.orch-io
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.string :as str]
            [braids.orch :as orch]
            [braids.ready-io :as rio]))

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
               project-path)]
    (try
      (let [result (proc/shell {:dir path :out :string :err :string}
                               "bd" "list" "--json")
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
               project-path)]
    (try
      (let [result (proc/shell {:dir path :out :string :err :string}
                               "bd" "list" "--json")
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
        (mapv (fn [s] {:label (:label s)
                       :status (:status s)
                       :age-seconds (or (:ageSeconds s) (:age-seconds s) (:age_seconds s) 0)})
              parsed)))
    (catch Exception _ [])))

(defn gather-and-tick
  "Full IO pipeline for orch tick: load everything, compute spawn decisions."
  ([] (gather-and-tick {}))
  ([{:keys [braids-home state-home session-labels]
     :or {session-labels []}}]
   (let [home (or state-home (rio/resolve-state-home))
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
         workers (rio/count-workers session-labels)
         notifications (into {} (map (fn [{:keys [slug]}]
                                       [slug (select-keys (get configs slug)
                                                          [:notifications :notification-mentions])])
                                     active-projects))
         open-beads (into {} (map (fn [{:keys [slug path]}]
                                    [slug (if (contains? iterations slug)
                                            (load-open-beads path)
                                            [])])
                                  active-projects))]
     (orch/tick reg configs iterations beads workers notifications open-beads))))


(defn gather-and-tick-with-zombies
  "Enhanced orch pipeline with zombie detection. Accepts session-info as JSON string.
   Returns tick result with :zombies key included."
  [session-info-json]
  (let [sessions (parse-session-labels session-info-json)
        labels (mapv :label sessions)
        home (rio/resolve-state-home)
        reg (rio/load-registry home)
        active-projects (filter #(= :active (:status %)) (:projects reg))
        configs (into {} (map (fn [{:keys [slug path]}]
                                [slug (rio/load-project-config path)])
                              active-projects))
        ;; Only load bead statuses for projects that have active sessions
        project-labels (filter #(str/starts-with? (:label %) "project:") sessions)
        projects-with-sessions (set (keep (fn [{:keys [label]}]
                                            (let [parts (str/split label #":" 3)]
                                              (when (>= (count parts) 2) (second parts))))
                                          project-labels))
        ;; Batch load bead statuses per project (one bd list call per project, not per bead)
        bead-statuses (if (empty? projects-with-sessions)
                        {}
                        (reduce (fn [acc {:keys [slug path]}]
                                  (if (contains? projects-with-sessions slug)
                                    (merge acc (load-bead-statuses path))
                                    acc))
                                {} active-projects))
        ;; Detect zombies
        zombies (orch/detect-zombies sessions configs bead-statuses)
        zombie-labels (set (map :label zombies))
        ;; Filter out zombie labels from worker count
        clean-labels (vec (remove zombie-labels labels))
        ;; Now run normal tick with clean labels
        iterations (into {} (keep (fn [{:keys [slug path]}]
                                    (when-let [iter (find-active-iteration path)]
                                      [slug iter]))
                                  active-projects))
        beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (rio/load-ready-beads path)
                                      [])])
                            active-projects))
        workers (rio/count-workers clean-labels)
        notifications (into {} (map (fn [{:keys [slug]}]
                                      [slug (select-keys (get configs slug)
                                                         [:notifications :notification-mentions])])
                                    active-projects))
        open-beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (load-open-beads path)
                                      [])])
                            active-projects))
        tick-result (orch/tick reg configs iterations beads workers notifications open-beads)]
    (cond-> tick-result
      (seq zombies) (assoc :zombies zombies))))

(defn gather-and-tick-from-session-labels
  "Simplified orch pipeline accepting space-separated session labels.
   Detects bead-closed zombies only (no session status/age available).
   Returns tick result with :zombies key if any found."
  [sessions-str]
  (let [labels (orch/parse-session-labels-string sessions-str)
        home (rio/resolve-state-home)
        reg (rio/load-registry home)
        active-projects (filter #(= :active (:status %)) (:projects reg))
        configs (into {} (map (fn [{:keys [slug path]}]
                                [slug (rio/load-project-config path)])
                              active-projects))
        ;; Extract projects with active sessions for batch bead status loading
        projects-with-sessions (set (keep (fn [label]
                                            (let [parts (str/split label #":" 3)]
                                              (when (>= (count parts) 2) (second parts))))
                                          labels))
        ;; Batch load bead statuses per project
        bead-statuses (if (empty? projects-with-sessions)
                        {}
                        (reduce (fn [acc {:keys [slug path]}]
                                  (if (contains? projects-with-sessions slug)
                                    (merge acc (load-bead-statuses path))
                                    acc))
                                {} active-projects))
        ;; Detect bead-closed zombies
        zombies (orch/detect-zombies-from-labels labels bead-statuses)
        zombie-labels (set (map :label zombies))
        ;; Filter out zombie labels from worker count
        clean-labels (vec (remove zombie-labels labels))
        ;; Normal tick computation
        iterations (into {} (keep (fn [{:keys [slug path]}]
                                    (when-let [iter (find-active-iteration path)]
                                      [slug iter]))
                                  active-projects))
        beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (rio/load-ready-beads path)
                                      [])])
                            active-projects))
        workers (rio/count-workers clean-labels)
        notifications (into {} (map (fn [{:keys [slug]}]
                                      [slug (select-keys (get configs slug)
                                                         [:notifications :notification-mentions])])
                                    active-projects))
        open-beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (load-open-beads path)
                                      [])])
                            active-projects))
        tick-result (orch/tick reg configs iterations beads workers notifications open-beads)]
    (cond-> tick-result
      (seq zombies) (assoc :zombies zombies))))

(defn gather-and-tick-debug
  "Like gather-and-tick but returns {:result tick-result :debug-ctx {...}} for debug output."
  ([] (gather-and-tick-debug {}))
  ([{:keys [braids-home state-home session-labels]
     :or {session-labels []}}]
   (let [home (or state-home (rio/resolve-state-home))
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
         workers (rio/count-workers session-labels)
         notifications (into {} (map (fn [{:keys [slug]}]
                                       [slug (select-keys (get configs slug)
                                                          [:notifications :notification-mentions])])
                                     active-projects))
         open-beads (into {} (map (fn [{:keys [slug path]}]
                                    [slug (if (contains? iterations slug)
                                            (load-open-beads path)
                                            [])])
                                  active-projects))
         result (orch/tick reg configs iterations beads workers notifications open-beads)]
     {:result result
      :debug-ctx {:registry reg :configs configs :iterations iterations :open-beads open-beads :ready-beads beads :workers workers}})))

(defn gather-and-tick-from-session-labels-debug
  "Like gather-and-tick-from-session-labels but returns {:result :debug-ctx}."
  [sessions-str]
  (let [labels (orch/parse-session-labels-string sessions-str)
        home (rio/resolve-state-home)
        reg (rio/load-registry home)
        active-projects (filter #(= :active (:status %)) (:projects reg))
        configs (into {} (map (fn [{:keys [slug path]}]
                                [slug (rio/load-project-config path)])
                              active-projects))
        projects-with-sessions (set (keep (fn [label]
                                            (let [parts (str/split label #":" 3)]
                                              (when (>= (count parts) 2) (second parts))))
                                          labels))
        bead-statuses (if (empty? projects-with-sessions)
                        {}
                        (reduce (fn [acc {:keys [slug path]}]
                                  (if (contains? projects-with-sessions slug)
                                    (merge acc (load-bead-statuses path))
                                    acc))
                                {} active-projects))
        zombies (orch/detect-zombies-from-labels labels bead-statuses)
        zombie-labels (set (map :label zombies))
        clean-labels (vec (remove zombie-labels labels))
        iterations (into {} (keep (fn [{:keys [slug path]}]
                                    (when-let [iter (find-active-iteration path)]
                                      [slug iter]))
                                  active-projects))
        beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (rio/load-ready-beads path)
                                      [])])
                            active-projects))
        workers (rio/count-workers clean-labels)
        notifications (into {} (map (fn [{:keys [slug]}]
                                      [slug (select-keys (get configs slug)
                                                         [:notifications :notification-mentions])])
                                    active-projects))
        open-beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (load-open-beads path)
                                      [])])
                            active-projects))
        tick-result (orch/tick reg configs iterations beads workers notifications open-beads)
        result (cond-> tick-result
                 (seq zombies) (assoc :zombies zombies))]
    {:result result
     :debug-ctx {:registry reg :configs configs :iterations iterations :open-beads open-beads :ready-beads beads :workers workers}}))

(defn gather-and-tick-with-zombies-debug
  "Like gather-and-tick-with-zombies but returns {:result :debug-ctx}."
  [session-info-json]
  (let [sessions (parse-session-labels session-info-json)
        labels (mapv :label sessions)
        home (rio/resolve-state-home)
        reg (rio/load-registry home)
        active-projects (filter #(= :active (:status %)) (:projects reg))
        configs (into {} (map (fn [{:keys [slug path]}]
                                [slug (rio/load-project-config path)])
                              active-projects))
        project-labels (filter #(str/starts-with? (:label %) "project:") sessions)
        projects-with-sessions (set (keep (fn [{:keys [label]}]
                                            (let [parts (str/split label #":" 3)]
                                              (when (>= (count parts) 2) (second parts))))
                                          project-labels))
        bead-statuses (if (empty? projects-with-sessions)
                        {}
                        (reduce (fn [acc {:keys [slug path]}]
                                  (if (contains? projects-with-sessions slug)
                                    (merge acc (load-bead-statuses path))
                                    acc))
                                {} active-projects))
        zombies (orch/detect-zombies sessions configs bead-statuses)
        zombie-labels (set (map :label zombies))
        clean-labels (vec (remove zombie-labels labels))
        iterations (into {} (keep (fn [{:keys [slug path]}]
                                    (when-let [iter (find-active-iteration path)]
                                      [slug iter]))
                                  active-projects))
        beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (rio/load-ready-beads path)
                                      [])])
                            active-projects))
        workers (rio/count-workers clean-labels)
        notifications (into {} (map (fn [{:keys [slug]}]
                                      [slug (select-keys (get configs slug)
                                                         [:notifications :notification-mentions])])
                                    active-projects))
        open-beads (into {} (map (fn [{:keys [slug path]}]
                              [slug (if (contains? iterations slug)
                                      (load-open-beads path)
                                      [])])
                            active-projects))
        tick-result (orch/tick reg configs iterations beads workers notifications open-beads)
        result (cond-> tick-result
                 (seq zombies) (assoc :zombies zombies))]
    {:result result
     :debug-ctx {:registry reg :configs configs :iterations iterations :open-beads open-beads :ready-beads beads :workers workers}}))

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
                                       (let [label (or (:label session) "")]
                                         (when (str/starts-with? label "project:")
                                           {:label label
                                            :status "running"
                                            :age-seconds (long (/ (- now (or (:updatedAt session) now)) 1000))}))))
                                     vec))
                              (catch Exception _ []))))))
              vec))))))

(defn gather-and-tick-from-stores
  "Full orch pipeline that reads session info directly from openclaw session stores.
   No external session input needed. Returns tick result with :zombies."
  ([] (gather-and-tick-from-stores (str (System/getProperty "user.home") "/.openclaw")))
  ([openclaw-home]
   (let [sessions (load-sessions-from-stores openclaw-home)
         labels (mapv :label sessions)
         home (rio/resolve-state-home)
         reg (rio/load-registry home)
         active-projects (filter #(= :active (:status %)) (:projects reg))
         configs (into {} (map (fn [{:keys [slug path]}]
                                 [slug (rio/load-project-config path)])
                               active-projects))
         projects-with-sessions (set (keep (fn [{:keys [label]}]
                                             (let [parts (str/split label #":" 3)]
                                               (when (>= (count parts) 2) (second parts))))
                                           sessions))
         bead-statuses (if (empty? projects-with-sessions)
                         {}
                         (reduce (fn [acc {:keys [slug path]}]
                                   (if (contains? projects-with-sessions slug)
                                     (merge acc (load-bead-statuses path))
                                     acc))
                                 {} active-projects))
         zombies (orch/detect-zombies sessions configs bead-statuses)
         zombie-labels (set (map :label zombies))
         clean-labels (vec (remove zombie-labels labels))
         iterations (into {} (keep (fn [{:keys [slug path]}]
                                     (when-let [iter (find-active-iteration path)]
                                       [slug iter]))
                                   active-projects))
         beads (into {} (map (fn [{:keys [slug path]}]
                               [slug (if (contains? iterations slug)
                                       (rio/load-ready-beads path)
                                       [])])
                             active-projects))
         workers (rio/count-workers clean-labels)
         notifications (into {} (map (fn [{:keys [slug]}]
                                       [slug (select-keys (get configs slug)
                                                          [:notifications :notification-mentions])])
                                     active-projects))
         open-beads (into {} (map (fn [{:keys [slug path]}]
                                    [slug (if (contains? iterations slug)
                                            (load-open-beads path)
                                            [])])
                                  active-projects))
         tick-result (orch/tick reg configs iterations beads workers notifications open-beads)]
     (cond-> tick-result
       (seq zombies) (assoc :zombies zombies)))))

(defn gather-and-tick-from-stores-debug
  "Like gather-and-tick-from-stores but returns {:result :debug-ctx}."
  ([] (gather-and-tick-from-stores-debug (str (System/getProperty "user.home") "/.openclaw")))
  ([openclaw-home]
   (let [sessions (load-sessions-from-stores openclaw-home)
         labels (mapv :label sessions)
         home (rio/resolve-state-home)
         reg (rio/load-registry home)
         active-projects (filter #(= :active (:status %)) (:projects reg))
         configs (into {} (map (fn [{:keys [slug path]}]
                                 [slug (rio/load-project-config path)])
                               active-projects))
         projects-with-sessions (set (keep (fn [{:keys [label]}]
                                             (let [parts (str/split label #":" 3)]
                                               (when (>= (count parts) 2) (second parts))))
                                           sessions))
         bead-statuses (if (empty? projects-with-sessions)
                         {}
                         (reduce (fn [acc {:keys [slug path]}]
                                   (if (contains? projects-with-sessions slug)
                                     (merge acc (load-bead-statuses path))
                                     acc))
                                 {} active-projects))
         zombies (orch/detect-zombies sessions configs bead-statuses)
         zombie-labels (set (map :label zombies))
         clean-labels (vec (remove zombie-labels labels))
         iterations (into {} (keep (fn [{:keys [slug path]}]
                                     (when-let [iter (find-active-iteration path)]
                                       [slug iter]))
                                   active-projects))
         beads (into {} (map (fn [{:keys [slug path]}]
                               [slug (if (contains? iterations slug)
                                       (rio/load-ready-beads path)
                                       [])])
                             active-projects))
         workers (rio/count-workers clean-labels)
         notifications (into {} (map (fn [{:keys [slug]}]
                                       [slug (select-keys (get configs slug)
                                                          [:notifications :notification-mentions])])
                                     active-projects))
         open-beads (into {} (map (fn [{:keys [slug path]}]
                                    [slug (if (contains? iterations slug)
                                            (load-open-beads path)
                                            [])])
                                  active-projects))
         tick-result (orch/tick reg configs iterations beads workers notifications open-beads)
         result (cond-> tick-result
                  (seq zombies) (assoc :zombies zombies))]
     {:result result
      :debug-ctx {:registry reg :configs configs :iterations iterations
                  :open-beads open-beads :ready-beads beads :workers workers}})))
