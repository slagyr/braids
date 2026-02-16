(ns braids.new-io
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [braids.new :as new]
            [braids.registry :as registry]))

(def braids-home (or (System/getenv "BRAIDS_HOME")
                       (str (System/getProperty "user.home") "/Projects")))

(def registry-path (str (System/getProperty "user.home") "/.openclaw/braids/registry.edn"))

(defn parse-new-args [args]
  (let [positional (first (remove #(str/starts-with? % "-") args))]
    (loop [remaining (vec (remove #{positional} args))
           result {:slug positional}]
      (if (empty? remaining)
        result
        (let [[flag val & rest-args] remaining]
          (case flag
            "--name"           (recur (vec rest-args) (assoc result :name val))
            "--goal"           (recur (vec rest-args) (assoc result :goal val))
            "--priority"       (recur (vec rest-args) (assoc result :priority (keyword val)))
            "--autonomy"       (recur (vec rest-args) (assoc result :autonomy (keyword val)))
            "--checkin"        (recur (vec rest-args) (assoc result :checkin (keyword val)))
            "--channel"        (recur (vec rest-args) (assoc result :channel val))
            "--max-workers"    (recur (vec rest-args) (assoc result :max-workers (parse-long val)))
            "--worker-timeout" (recur (vec rest-args) (assoc result :worker-timeout (parse-long val)))
            "--guardrails"     (recur (vec rest-args) (update result :guardrails (fnil conj []) val))
            "--braids-home"  (recur (vec rest-args) (assoc result :braids-home val))
            ;; skip unknown flags
            (recur (vec (rest remaining)) result)))))))

(defn run-new
  ([args] (run-new args {}))
  ([args {:keys [registry-file]}]
   (let [params (parse-new-args args)
         home (or (:braids-home params) braids-home)
         slug (:slug params)
         reg-path (or registry-file registry-path)
         errors (new/validate-new-params params)]
     (if (seq errors)
       {:exit 1 :message (str "Errors:\n" (str/join "\n" (map #(str "  - " %) errors)))}
       (let [project-dir (str home "/" slug)]
         (if (fs/exists? project-dir)
           {:exit 1 :message (str "Error: directory already exists: " project-dir)}
           (let [registry (if (fs/exists? reg-path)
                            (registry/parse-registry (slurp reg-path))
                            {:projects []})
                 existing-slugs (set (map :slug (:projects registry)))]
             (if (contains? existing-slugs slug)
               {:exit 1 :message (str "Error: project '" slug "' already exists in registry")}
               (let [config (new/build-project-config params)
                     entry (new/build-registry-entry params project-dir)
                     agents-md (new/build-agents-md)
                     new-registry (new/add-to-registry registry entry)]
                 ;; Create directories
                 (fs/create-dirs (str project-dir "/.project/iterations/001"))
                 ;; Write project.edn
                 (spit (str project-dir "/.project/project.edn") (pr-str config))
                 ;; Write ITERATION.md
                 (spit (str project-dir "/.project/iterations/001/ITERATION.md")
                       "# Iteration 001\n\nStatus: planning\n\n## Stories\n\n## Guardrails\n\n## Notes\n")
                 ;; Write AGENTS.md
                 (spit (str project-dir "/AGENTS.md") agents-md)
                 ;; Init git
                 (proc/shell {:dir project-dir} "git" "init" "-q")
                 ;; Init bd
                 (proc/shell {:dir project-dir} "bd" "init" "-q")
                 ;; Save registry
                 (spit reg-path (pr-str new-registry))
                 ;; Initial commit
                 (proc/shell {:dir project-dir} "git" "add" "-A")
                 (proc/shell {:dir project-dir} "git" "commit" "-q" "-m" (str "Initialize project: " slug))
                 {:exit 0
                  :message (str "✓ Created project: " slug "\n"
                                "  Path: " project-dir "\n"
                                "  Config: .project/project.edn\n"
                                "  Iteration: 001 (planning)\n"
                                "  Registry: updated")})))))))))
