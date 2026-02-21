(ns braids.registry
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [braids.edn-format :refer [edn-format]]))

(def valid-statuses #{:active :paused :blocked})
(def valid-priorities #{:high :normal :low})

(defn parse-registry [edn-str]
  (edn/read-string edn-str))

(defn validate-registry [{:keys [projects]}]
  (let [errors (atom [])]
    (doseq [{:keys [slug status priority path]} projects]
      (when-not slug
        (swap! errors conj "Project missing :slug"))
      (when (and status (not (valid-statuses status)))
        (swap! errors conj (str "Invalid status: " status " for " slug)))
      (when (and priority (not (valid-priorities priority)))
        (swap! errors conj (str "Invalid priority: " priority " for " slug))))
    ;; Check duplicate slugs
    (let [slugs (map :slug projects)
          dupes (for [[s c] (frequencies slugs) :when (> c 1)] s)]
      (doseq [d dupes]
        (swap! errors conj (str "Duplicate slug: " d))))
    @errors))

(defn registry->edn-string [registry]
  (edn-format registry))
