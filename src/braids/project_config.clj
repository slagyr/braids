(ns braids.project-config
  (:require [clojure.edn :as edn]))

(def valid-statuses #{:active :paused :blocked})
(def valid-priorities #{:high :normal :low})
(def valid-autonomy #{:full :ask-first :research-only})
(def valid-checkins #{:daily :weekly :on-demand})

(def default-notifications
  {:iteration-start true
   :bead-start true
   :bead-complete true
   :iteration-complete true
   :no-ready-beads true
   :question true
   :blocker true})

(def defaults
  {:max-workers 1
   :worker-timeout 3600
   :checkin :on-demand
   :channel nil
   :notifications default-notifications})

(defn- normalize-mentions
  "Normalize notification-mentions values: strings become single-element vectors."
  [mentions]
  (when mentions
    (into {}
      (map (fn [[k v]]
             [k (if (string? v) [v] v)])
           mentions))))

(defn parse-project-config [edn-str]
  (let [raw (edn/read-string edn-str)
        mentions (normalize-mentions (:notification-mentions raw))]
    (-> (merge defaults (dissoc raw :goal :guardrails :notification-mentions))
        (assoc :notifications (merge default-notifications (:notifications raw)))
        (cond-> mentions (assoc :notification-mentions mentions)))))

(defn validate-project-config [{:keys [name status priority autonomy max-workers worker-timeout]}]
  (let [errors (atom [])]
    (when-not name
      (swap! errors conj "Missing :name"))
    (when (and status (not (valid-statuses status)))
      (swap! errors conj (str "Invalid status: " status)))
    (when (and priority (not (valid-priorities priority)))
      (swap! errors conj (str "Invalid priority: " priority)))
    (when (and autonomy (not (valid-autonomy autonomy)))
      (swap! errors conj (str "Invalid autonomy: " autonomy)))
    (when (and max-workers (not (pos? max-workers)))
      (swap! errors conj "max-workers must be positive"))
    (when (and worker-timeout (not (pos? worker-timeout)))
      (swap! errors conj "worker-timeout must be positive"))
    @errors))

(defn project-config->edn-string [config]
  (pr-str config))
