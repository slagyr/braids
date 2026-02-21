(ns braids.edn-format
  (:require [clojure.pprint :as pp]))

(defn edn-format
  "Pretty-print an EDN data structure to a string with proper indentation."
  [data]
  (binding [pp/*print-right-margin* 80]
    (with-out-str (pp/pprint data))))
