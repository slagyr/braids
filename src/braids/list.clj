;; mutation-tested: 2026-03-07
(ns braids.list
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

;; ANSI color codes
(def ^:private colors
  {:red     "\033[31m"
   :green   "\033[32m"
   :yellow  "\033[33m"
   :reset   "\033[0m"})

(defn- colorize [text color]
  (str (get colors color "") text (:reset colors)))

(defn- status-color [status]
  (case (str status)
    "active" :green
    "paused" :yellow
    "inactive" :yellow
    "blocked" :red
    nil))

(defn- priority-color [priority]
  (case (str priority)
    "high" :red
    "low" :yellow
    nil))

(defn- progress-color [percent]
  (cond
    (nil? percent) nil
    (>= percent 100) :green
    (>= percent 50) :yellow
    :else :red))

(defn- format-progress [{:keys [iteration]}]
  (if-let [stats (:stats iteration)]
    (str (:closed stats) "/" (:total stats) " (" (:percent stats) "%)")
    "—"))

(defn- format-iteration [{:keys [iteration]}]
  (if iteration
    (str (:number iteration))
    "—"))

(defn- format-workers [{:keys [workers max-workers]}]
  (if (and workers max-workers)
    (str workers "/" max-workers)
    "—"))

(defn format-list
  "Format registry projects as a rich colored table."
  [{:keys [projects]}]
  (if (or (empty? projects) (nil? projects))
    "No projects registered."
    (let [headers ["SLUG" "STATUS" "PRIORITY" "ITERATION" "PROGRESS" "WORKERS" "PATH"]
          ;; Build plain text rows for width calculation
          plain-rows (mapv (fn [proj]
                             [(or (:slug proj) "")
                              (if (:status proj) (name (:status proj)) "")
                              (if (:priority proj) (name (:priority proj)) "")
                              (format-iteration proj)
                              (format-progress proj)
                              (format-workers proj)
                              (or (:path proj) "")])
                           projects)
          all-rows (cons headers plain-rows)
          widths (mapv (fn [col]
                         (apply max (map #(count (nth % col "")) all-rows)))
                       (range (count headers)))
          pad (fn [text w] (format (str "%-" w "s") text))
          ;; Colorize cells
          color-pad (fn [text w color]
                      (let [padded (pad text w)]
                        (if color (colorize padded color) padded)))
          colorize-row (fn [proj plain-row]
                         (let [status-str (nth plain-row 1)
                               priority-str (nth plain-row 2)
                               percent (get-in proj [:iteration :stats :percent])]
                           [(pad (nth plain-row 0) (nth widths 0))
                            (color-pad status-str (nth widths 1) (status-color status-str))
                            (color-pad priority-str (nth widths 2) (priority-color priority-str))
                            (pad (nth plain-row 3) (nth widths 3))
                            (color-pad (nth plain-row 4) (nth widths 4) (progress-color percent))
                            (pad (nth plain-row 5) (nth widths 5))
                            (pad (nth plain-row 6) (nth widths 6))]))
          header-line (str/join "  " (map-indexed (fn [i h] (pad h (nth widths i))) headers))
          separator (str/join "  " (map #(apply str (repeat % "-")) widths))
          data-lines (map (fn [proj plain-row]
                            (str/join "  " (colorize-row proj plain-row)))
                          projects plain-rows)]
      (str/join "\n" (concat [header-line separator] data-lines)))))

(defn format-list-json
  "Format registry projects as JSON."
  [{:keys [projects]}]
  (json/generate-string
    (mapv (fn [{:keys [slug status priority path iteration workers max-workers]}]
            (cond-> {:slug slug
                     :status (when status (name status))
                     :priority (when priority (name priority))
                     :path path}
              iteration (assoc :iteration {:number (:number iteration)
                                           :stats (:stats iteration)})
              workers (assoc :workers workers)
              max-workers (assoc :max_workers max-workers)))
          (or projects []))))
