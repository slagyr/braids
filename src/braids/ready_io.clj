(ns braids.ready-io
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]
            [clojure.string :as str]
            [braids.migration :as migration]
            [braids.project-config :as pc]
            [braids.ready :as ready]
            [braids.registry :as registry]))

(def default-braids-home
  (str (fs/expand-home "~/Projects")))

(def default-state-home
  (str (fs/expand-home "~/.openclaw/braids")))

(defn resolve-braids-home []
  ;; Check for registry.edn or registry.md to find BRAIDS_HOME
  ;; For now, use default
  default-braids-home)

(defn resolve-state-home []
  "Returns the directory for agent infrastructure files (registry, orchestrator state, STATUS).
   Defaults to ~/.openclaw/braids/"
  default-state-home)

(defn- expand-path [path]
  (if (str/starts-with? path "~/")
    (str (fs/expand-home "~") "/" (subs path 2))
    path))

(defn load-registry
  "Load registry from BRAIDS_HOME. Tries registry.edn first, falls back to registry.md."
  [braids-home]
  (let [edn-path (str braids-home "/registry.edn")
        md-path (str braids-home "/registry.md")]
    (cond
      (fs/exists? edn-path)
      (registry/parse-registry (slurp edn-path))

      (fs/exists? md-path)
      (migration/parse-registry-md (slurp md-path))

      :else
      {:projects []})))

(defn load-project-config
  "Load project config. Tries .project/project.edn first, falls back to .project/PROJECT.md, then PROJECT.md."
  [project-path]
  (let [path (expand-path project-path)
        edn-path (str path "/.project/project.edn")
        md-path (str path "/.project/PROJECT.md")
        root-md-path (str path "/PROJECT.md")]
    (cond
      (fs/exists? edn-path)
      (pc/parse-project-config (slurp edn-path))

      (fs/exists? md-path)
      (migration/parse-project-md (slurp md-path))

      (fs/exists? root-md-path)
      (migration/parse-project-md (slurp root-md-path))

      :else
      pc/defaults)))

(defn load-ready-beads
  "Run `bd ready --json` in the project directory and parse the result."
  [project-path]
  (let [path (expand-path project-path)]
    (try
      (let [result (proc/shell {:dir path :out :string :err :string}
                               "bd" "ready" "--json")
            parsed (json/parse-string (:out result) true)]
        (if (sequential? parsed)
          (mapv (fn [b] {:id (:id b) :title (:title b)
                         :priority (:priority b)}) parsed)
          []))
      (catch Exception _e []))))

(defn count-workers
  "Count active worker sessions per project slug.
   Expects a function that returns session labels."
  [session-labels]
  (->> session-labels
       (filter #(str/starts-with? % "project:"))
       (map (fn [label]
              (let [parts (str/split label #":")]
                (when (>= (count parts) 2) (second parts)))))
       (filter some?)
       frequencies))

(defn gather-and-compute
  "Full IO pipeline: load registry, configs, beads, and compute ready list."
  ([] (gather-and-compute {}))
  ([{:keys [braids-home state-home session-labels]
     :or {session-labels []}}]
   (let [home (or state-home (resolve-state-home))
         reg (load-registry home)
         active-projects (filter #(= :active (:status %)) (:projects reg))
         configs (into {} (map (fn [{:keys [slug path]}]
                                 [slug (load-project-config path)])
                               active-projects))
         beads (into {} (map (fn [{:keys [slug path]}]
                               [slug (load-ready-beads path)])
                             active-projects))
         workers (count-workers session-labels)]
     (ready/ready-beads reg configs beads workers))))
