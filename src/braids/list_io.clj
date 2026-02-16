(ns braids.list-io
  (:require [braids.ready-io :as rio]
            [braids.list :as list]))

(defn load-and-list
  "Load registry and format project list."
  [{:keys [json?]}]
  (let [state-home (rio/resolve-state-home)
        reg (rio/load-registry state-home)]
    (if json?
      (list/format-list-json reg)
      (list/format-list reg))))
