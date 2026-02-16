(ns braids.orch-io
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [braids.orch :as orch]
            [braids.ready-io :as rio]))

(defn parse-iteration-status
  "Extract the status value from ITERATION.md content.
   Tolerates multiple formats: 'Status: active', '- **Status:** active', '**Status:** active', etc.
   Returns the status string (lowercase) or nil."
  [content]
  (when-let [m (re-find #"(?i)\*{0,2}Status:\*{0,2}\s*(\w+)" content)]
    (str/lower-case (second m))))

(defn find-active-iteration
  "Scan .project/iterations/*/ITERATION.md for one with Status: active.
   Returns the iteration number (directory name) or nil.
   Tolerates markdown formatting variations in the Status field."
  [project-path]
  (let [path (if (str/starts-with? project-path "~/")
               (str (fs/expand-home "~") "/" (subs project-path 2))
               project-path)
        iter-dir (str path "/.project/iterations")]
    (when (fs/exists? iter-dir)
      (some (fn [dir]
              (let [iter-md (str dir "/ITERATION.md")]
                (when (fs/exists? iter-md)
                  (let [content (slurp iter-md)]
                    (when (= "active" (parse-iteration-status content))
                      (fs/file-name dir))))))
            (sort (fs/list-dir iter-dir))))))

(defn gather-and-tick
  "Full IO pipeline for orch tick: load everything, compute spawn decisions."
  ([] (gather-and-tick {}))
  ([{:keys [projects-home state-home session-labels]
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
                                     active-projects))]
     (orch/tick reg configs iterations beads workers notifications))))
