(ns braids.list
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(defn format-list
  "Format registry projects as a human-friendly table."
  [{:keys [projects]}]
  (if (or (empty? projects) (nil? projects))
    "No projects registered."
    (let [headers ["SLUG" "STATUS" "PRIORITY" "PATH"]
          rows (mapv (fn [{:keys [slug status priority path]}]
                       [(or slug "")
                        (if status (name status) "")
                        (if priority (name priority) "")
                        (or path "")])
                     projects)
          all-rows (cons headers rows)
          widths (mapv (fn [col]
                         (apply max (map #(count (nth % col "")) all-rows)))
                       (range (count headers)))
          fmt-row (fn [row]
                    (str/join "  "
                      (map-indexed (fn [i cell]
                                     (let [w (nth widths i)]
                                       (format (str "%-" w "s") cell)))
                                   row)))
          header-line (fmt-row headers)
          separator (str/join "  " (map #(apply str (repeat % "-")) widths))]
      (str/join "\n" (concat [header-line separator] (map fmt-row rows))))))

(defn format-list-json
  "Format registry projects as JSON."
  [{:keys [projects]}]
  (json/generate-string
    (mapv (fn [{:keys [slug status priority path]}]
            {:slug slug
             :status (when status (name status))
             :priority (when priority (name priority))
             :path path})
          (or projects []))))
