;; mutation-tested: 2026-03-07
(ns braids.status
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(defn build-dashboard
  "Build dashboard data from registry, configs, iterations, and workers.
   iterations is a map of slug -> {:number :status :stories :stats}
   workers is a map of slug -> count"
  [registry configs iterations workers]
  {:projects
   (mapv (fn [{:keys [slug status priority path]}]
           (let [cfg (get configs slug)
                 iter (get iterations slug)
                 worker-count (get workers slug 0)
                 max-w (or (:max-workers cfg) 1)]
             (cond-> {:slug slug
                      :status (if status (name status) "unknown")
                      :priority (if priority (name priority) "normal")
                      :path path
                      :workers worker-count
                      :max-workers max-w}
               iter (assoc :iteration iter))))
         (:projects registry))})

(defn- status-icon [status]
  (case status
    "closed" "✓"
    "in_progress" "▶"
    "blocked" "✗"
    "open" "○"
    "?"))

(defn format-project-detail
  "Format detailed view for a single project."
  [{:keys [slug status iteration workers max-workers]}]
  (if-not iteration
    (str slug " [" status "] — no active iteration")
    (let [{:keys [number stories stats]} iteration
          header (str slug " [" status "] — Iteration " number
                      " — " (:closed stats) "/" (:total stats)
                      " (" (:percent stats) "%) — workers: " workers "/" max-workers)
          separator (apply str (repeat (min (count header) 60) "─"))
          story-lines (map (fn [{:keys [id title status]}]
                             (str "  " (status-icon status) " " id ": " title " [" status "]"))
                           stories)]
      (str/join "\n" (concat [header separator] story-lines)))))

(defn format-dashboard
  "Format dashboard for human-readable output."
  [{:keys [projects]}]
  (if (empty? projects)
    "No projects registered."
    (let [header "BRAIDS STATUS"
          separator (apply str (repeat 40 "─"))
          project-lines
          (map (fn [{:keys [slug status iteration workers max-workers] :as proj}]
                 (if iteration
                   (let [{:keys [number stats]} iteration]
                     (str "  " slug " [" status "] iter:" number
                          " " (:closed stats) "/" (:total stats)
                          " (" (:percent stats) "%) workers:" workers "/" max-workers))
                   (str "  " slug " [" status "]")))
               projects)]
      (str/join "\n" (concat [header separator] project-lines)))))

(defn format-dashboard-json
  "Format dashboard as JSON."
  [{:keys [projects]}]
  (json/generate-string
    {:projects
     (mapv (fn [{:keys [slug status priority path iteration workers max-workers]}]
             (cond-> {:slug slug :status status :priority priority :path path
                      :workers workers :max-workers max-workers}
               iteration (assoc :iteration iteration)))
           projects)}))
