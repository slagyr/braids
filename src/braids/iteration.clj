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
  "Extract story ids from iteration data."
  [iteration]
  (mapv :id (:stories iteration)))

;; --- Legacy markdown parsing (needed for migration) ---

(defn parse-iteration-number [content]
  (when-let [m (re-find #"#\s*Iteration\s+(\S+)" content)]
    (second m)))

(defn parse-iteration-status [content]
  (when-let [m (re-find #"(?i)\*{0,2}Status:\*{0,2}\s*(\w+)" content)]
    (str/lower-case (second m))))

(defn parse-iteration-stories
  "Extract story ids and titles from ITERATION.md content."
  [content]
  (let [lines (str/split-lines content)
        in-stories (atom false)
        results (atom [])]
    (doseq [line lines]
      (cond
        (re-matches #"##\s+Stories\s*" line)
        (reset! in-stories true)

        (and @in-stories (re-matches #"##\s+.*" line))
        (reset! in-stories false)

        (and @in-stories (re-matches #"-\s+\S+:.*" line))
        (when-let [m (re-find #"-\s+(\S+):\s+(.*)" line)]
          (swap! results conj {:id (second m) :title (str/trim (nth m 2))}))))
    @results))

(defn- parse-section-items
  "Parse items from a markdown section (## SectionName) as a list of strings."
  [content section-name]
  (let [lines (str/split-lines content)
        in-section (atom false)
        results (atom [])]
    (doseq [line lines]
      (cond
        (re-matches (re-pattern (str "##\\s+" section-name "\\s*")) line)
        (reset! in-section true)

        (and @in-section (re-matches #"##\s+.*" line))
        (reset! in-section false)

        (and @in-section (re-find #"^-\s+(.*)" line))
        (when-let [m (re-find #"^-\s+(.*)" line)]
          (swap! results conj (str/trim (second m))))))
    @results))

(defn migrate-iteration-md
  "Convert ITERATION.md content to an iteration EDN map."
  [md-content]
  (let [number (parse-iteration-number md-content)
        status (keyword (or (parse-iteration-status md-content) "planning"))
        stories (parse-iteration-stories md-content)
        guardrails (parse-section-items md-content "Guardrails")
        notes (parse-section-items md-content "Notes")
        result {:number number :status status :stories stories}]
    (cond-> result
      (seq guardrails) (assoc :guardrails guardrails)
      (seq notes) (assoc :notes notes))))

;; --- Annotation and formatting ---

(defn annotate-stories
  "Annotate stories with status, priority, and deps from bead data.
   beads is a seq of maps with string keys."
  [stories beads]
  (let [bead-map (into {} (map (fn [b] [(get b "id") b]) beads))]
    (mapv (fn [{:keys [id title]}]
            (if-let [bead (get bead-map id)]
              {:id id
               :title title
               :status (get bead "status" "unknown")
               :priority (get bead "priority")
               :deps (mapv #(get % "depends_on_id")
                           (get bead "dependencies" []))}
              {:id id :title title :status "unknown" :priority nil :deps []}))
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
