(ns braids.core
  (:require [clojure.string :as str]))

(def commands
  {"ready" {:command :ready :doc "List beads ready to work"}
   "help"  {:command :help  :doc "Show this help message"}})

(defn help-text []
  (str/join "\n"
    ["braids — CLI for the projects skill"
     ""
     "Usage: braids <command> [args...]"
     ""
     "Commands:"
     (str/join "\n"
       (for [[name {:keys [doc]}] (sort-by key commands)]
         (format "  %-12s %s" name doc)))
     ""
     "Options:"
     "  -h, --help   Show this help message"]))

(defn dispatch [args]
  (let [first-arg (first args)
        rest-args (vec (rest args))]
    (cond
      (or (nil? first-arg) (= first-arg "--help") (= first-arg "-h"))
      {:command :help}

      (contains? commands first-arg)
      (merge {:command (get-in commands [first-arg :command])
              :args rest-args})

      :else
      {:command :unknown :input first-arg})))

(defn run [args]
  (let [{:keys [command input]} (dispatch args)]
    (case command
      :help (do (println (help-text)) 0)
      :unknown (do (println (str "Unknown command: " input))
                   (println)
                   (println (help-text))
                   1)
      ;; Default for unimplemented commands
      (do (println (str "Command '" (name command) "' not yet implemented.")) 0))))
