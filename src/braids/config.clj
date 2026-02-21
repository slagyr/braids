(ns braids.config
  "Pure functions for braids config.edn management."
  (:require [clojure.string :as str]
            [braids.edn-format :refer [edn-format]]))

(def defaults
  {:braids-home "~/Projects"})

(defn parse-config
  "Parse config EDN string. Returns map with defaults applied."
  [s]
  (merge defaults (read-string s)))

(defn serialize-config
  "Serialize config map to EDN string."
  [config]
  (edn-format config))

(defn config-get
  "Get a config value by key string. Returns {:ok value} or {:error msg}."
  [config key-str]
  (let [k (keyword key-str)]
    (if (contains? config k)
      {:ok (get config k)}
      {:error (str "Key '" key-str "' not found in config.")})))

(defn config-set
  "Set a config value. Returns updated config map."
  [config key-str value-str]
  (assoc config (keyword key-str) value-str))

(defn config-list
  "Format config map for display."
  [config]
  (str/join "\n"
    (for [[k v] (sort-by key config)]
      (str (name k) " = " v))))

(defn config-help []
  (str/join "\n"
    ["Usage: braids config <subcommand>"
     ""
     "Subcommands:"
     "  list           List all config values"
     "  get <key>      Get a config value"
     "  set <key> <val> Set a config value"
     ""
     "Examples:"
     "  braids config list"
     "  braids config get braids-home"
     "  braids config set braids-home ~/MyProjects"]))
