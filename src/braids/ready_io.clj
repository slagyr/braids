(ns braids.ready-io
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]
            [clojure.string :as str]
            [braids.project-config :as pc]
            [braids.config-io :as config-io]
            [braids.ready :as ready]
            [braids.registry :as registry]))

(def default-braids-home
  (str (fs/expand-home "~/Projects")))

(def default-state-home
  (str (fs/expand-home "~/.openclaw/braids")))

(defn resolve-braids-home []
  (config-io/resolve-braids-home))

(defn resolve-state-home []
  "Returns the directory for agent infrastructure files (registry, orchestrator state, STATUS).
   Defaults to ~/.openclaw/braids/"
  default-state-home)

(defn- expand-path [path]
  (if (str/starts-with? path "~/")
    (str (fs/expand-home "~") "/" (subs path 2))
    path))

(defn load-registry
  "Load registry from registry.edn. No markdown fallback."
  [braids-home]
  (let [edn-path (str braids-home "/registry.edn")]
    (if (fs/exists? edn-path)
      (registry/parse-registry (slurp edn-path))
      {:projects []})))

(defn load-project-config
  "Load project config from .braids/config.edn (preferred) or .braids/project.edn (legacy). No markdown fallback."
  [project-path]
  (let [path (expand-path project-path)
        config-edn (str path "/.braids/config.edn")
        legacy-edn (str path "/.braids/project.edn")]
    (cond
      (fs/exists? config-edn)
      (pc/parse-project-config (slurp config-edn))

      (fs/exists? legacy-edn)
      (pc/parse-project-config (slurp legacy-edn))

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
