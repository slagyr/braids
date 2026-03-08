;; mutation-tested: 2026-03-07
(ns braids.iteration
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [braids.edn-format :refer [edn-format]]))

(def iteration-defaults
  {:status :planning
   :stories []
   :notes []
   :guardrails []})

(defn parse-iteration-edn
  "Parse an iteration.edn string into a map with defaults applied."
  [edn-str]
  (let [raw (edn/read-string edn-str)]
    (merge iteration-defaults raw)))

(defn validate-iteration
  "Validate an iteration map. Returns a vector of error strings (empty if valid)."
  [{:keys [number status stories]}]
  (let [errors (atom [])]
    (when-not number (swap! errors conj "Missing :number"))
    (when-not status (swap! errors conj "Missing :status"))
    (when (and status (not (#{:planning :active :complete} status)))
      (swap! errors conj (str "Invalid status: " status)))
    (when-not (vector? stories)
      (swap! errors conj "Missing or invalid :stories"))
    @errors))

(defn iteration->edn-string
  "Serialize iteration data to an EDN string."
  [data]
  (edn-format (select-keys data [:number :status :stories :guardrails :notes])))

(defn story-ids
  "Extract story ids from iteration data.
   Handles both string ids and map entries."
  [iteration]
  (mapv #(if (string? %) % (:id %)) (:stories iteration)))

;; --- Annotation and formatting ---

(defn- normalize-story
  "Normalize a story entry to {:id ... :title ...} map.
   Handles both string ids and map entries."
  [story]
  (if (string? story)
    {:id story :title nil}
    story))

(defn annotate-stories
  "Annotate stories with status, priority, and deps from bead data.
   beads is a seq of maps with string keys.
   Stories can be maps with :id/:title or plain string ids."
  [stories beads]
  (let [bead-map (into {} (map (fn [b] [(get b "id") b]) beads))]
    (mapv (fn [story]
            (let [{:keys [id title]} (normalize-story story)]
              (if-let [bead (get bead-map id)]
                {:id id
                 :title (or title (get bead "title") id)
                 :status (get bead "status" "unknown")
                 :priority (get bead "priority")
                 :deps (mapv #(get % "depends_on_id")
                             (get bead "dependencies" []))}
                {:id id :title (or title id) :status "unknown" :priority nil :deps []})))
          stories)))

(defn completion-stats [stories]
  (let [total (count stories)
        closed (count (filter #(= "closed" (:status %)) stories))]
    {:total total
     :closed closed
     :percent (if (zero? total) 0 (int (* 100 (/ closed total))))}))

(defn- status-icon [status]
  (case status
    "closed" "✓"
    "in_progress" "▶"
    "blocked" "✗"
    "open" "○"
    "?"))

(defn format-iteration
  "Format iteration data for human-readable output."
  [{:keys [number status stories stats]}]
  (let [header (str "Iteration " number " [" status "] — "
                    (:closed stats) "/" (:total stats) " done (" (:percent stats) "%)")
        separator (apply str (repeat (count header) "─"))
        story-lines (map (fn [{:keys [id title status priority deps]}]
                           (let [icon (status-icon status)
                                 p-str (when priority (str " P" priority))
                                 dep-str (when (seq deps)
                                           (str " ← " (str/join ", " deps)))]
                             (str "  " icon " " id ": " title
                                  " [" status (or p-str "") "]"
                                  (or dep-str ""))))
                         stories)]
    (str/join "\n" (concat [header separator] story-lines))))

(defn format-iteration-json
  "Format iteration data as JSON."
  [{:keys [number status stories stats]}]
  (json/generate-string
    {:number number
     :status status
     :stories (mapv (fn [{:keys [id title status priority deps]}]
                      {:id id :title title :status status
                       :priority priority :deps deps})
                    stories)
     :stats stats}))
