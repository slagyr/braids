;; mutation-tested: 2026-03-07
(ns braids.ready
  (:require [clojure.string :as str]))

(def priority-order {:high 0 :normal 1 :low 2})

;; ANSI color codes
(def ^:private colors
  {:red        "\033[31m"
   :green      "\033[32m"
   :yellow     "\033[33m"
   :cyan       "\033[36m"
   :bold-white "\033[1;37m"
   :reset      "\033[0m"})

(defn- colorize [text color]
  (str (get colors color "") text (:reset colors)))

(defn- priority-color [priority]
  (case priority
    ("P0" "P1") :red
    "P2" :yellow
    "P3" :green
    nil))

(defn ready-beads
  "Pure function: given registry, project configs, ready beads per project,
   and worker counts per project, returns a flat list of ready beads
   annotated with :project slug. Ordered by project priority (high first)."
  [registry configs beads workers]
  (let [active-projects (->> (:projects registry)
                             (filter #(= :active (:status %)))
                             (filter (fn [{:keys [slug]}]
                                       (let [cfg (get configs slug)]
                                         (or (nil? cfg) (= :active (:status cfg))))))
                             (sort-by #(get priority-order (:priority %) 1)))]
    (->> active-projects
         (mapcat (fn [{:keys [slug]}]
                   (let [cfg (get configs slug)
                         max-w (or (:max-workers cfg) 1)
                         current-w (get workers slug 0)]
                     (when (< current-w max-w)
                       (map #(assoc % :project slug) (get beads slug []))))))
         vec)))

(defn format-ready-output
  "Format ready beads for human-readable CLI output with ANSI colors."
  [beads]
  (if (empty? beads)
    "No ready beads."
    (str/join "\n"
      (map-indexed
        (fn [i {:keys [project id title priority]}]
          (let [p (if (number? priority) (str "P" priority) (or priority "?"))
                colored-p (if-let [c (priority-color p)]
                            (colorize p c)
                            p)]
            (format "%d) [%s] %s: %s (%s)"
                    (inc i)
                    colored-p
                    id
                    (colorize title :bold-white)
                    (colorize project :cyan))))
        beads))))
