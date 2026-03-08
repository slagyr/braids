;; mutation-tested: 2026-03-07
(ns braids.init
  (:require [clojure.string :as str]))

(defn check-prerequisites [{:keys [registry-exists? bd-available? force?]}]
  (cond-> []
    (not bd-available?)
    (conj "bd (beads) is not installed. Install it from https://github.com/nickthecook/bd")

    (and registry-exists? (not force?))
    (conj "braids is already initialized (registry.edn exists). Use --force to reinitialize.")))

(defn plan-init [{:keys [braids-dir braids-home registry-path config-path
                          braids-dir-exists? braids-home-exists?]}]
  (cond-> []
    (not braids-dir-exists?)
    (conj {:action :create-braids-dir :path braids-dir})

    (not braids-home-exists?)
    (conj {:action :create-braids-home :path braids-home})

    true
    (conj {:action :create-registry :path registry-path})

    true
    (conj {:action :save-config :path config-path :braids-home braids-home})))

(defn format-result [{:keys [success? errors braids-dir braids-home registry-path actions-taken]}]
  (if success?
    (str "✓ braids initialized\n"
         "  State dir:     " braids-dir "\n"
         "  Braids home:   " braids-home "\n"
         "  Registry:      " registry-path)
    (str "Error:\n" (str/join "\n" (map #(str "  - " %) errors)))))
