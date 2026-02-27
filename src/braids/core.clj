(ns braids.core
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [cheshire.core :as json]
            [braids.ready :as ready]
            [braids.ready-io :as ready-io]
            [braids.orch :as orch]
            [braids.orch-io :as orch-io]
            [braids.list-io :as list-io]
            [braids.iteration-io :as iter-io]
            [braids.new-io :as new-io]
            [braids.init-io :as init-io]
            [braids.config :as config]
            [braids.config-io :as config-io]))

(def commands
  {"list"      {:command :list      :doc "Show projects with status, iterations, and progress"}
   "iteration" {:command :iteration :doc "Show active iteration and bead statuses"}
   "ready"     {:command :ready     :doc "List beads ready to work"}
   "orch-tick" {:command :orch-tick :doc "Orchestrator tick: compute spawn decisions (JSON)"}
   "orch-run"  {:command :orch-run  :doc "Orchestrator run: tick + pre-formatted sessions_spawn params (JSON)"}
   "spawn-msg" {:command :spawn-msg :doc "Emit spawn message for a bead (from orch-tick output)"}
   "new"       {:command :new       :doc "Create a new project"}
   "init"      {:command :init      :doc "First-time setup for braids"}
   "config"    {:command :config    :doc "Get/set/list braids configuration"}
   "help"      {:command :help      :doc "Show this help message"}})

(def ^:private ansi
  {:bold-white  "\033[1;37m"
   :bold-cyan   "\033[1;36m"
   :bold-yellow "\033[1;33m"
   :bold-blue   "\033[1;34m"
   :reset       "\033[0m"})

(defn- c [text color]
  (str (get ansi color "") text (:reset ansi)))

(defn help-text []
  (str/join "\n"
    [(str (c "braids" :bold-white) " — CLI for the braids skill")
     ""
     (str (c "Usage:" :bold-cyan) " braids <command> [args...]")
     ""
     (c "Commands:" :bold-yellow)
     (str/join "\n"
       (for [[name {:keys [doc]}] (sort-by key commands)]
         (format "  %s%s" (c (format "%-12s" name) :bold-blue) doc)))
     ""
     (c "Options:" :bold-yellow)
     (str "  " (c "-h, --help" :bold-blue) "   Show this help message")]))

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
      :list (let [json? (some #{"--json"} (:args (dispatch args)))]
              (println (list-io/load-and-list {:json? json?}))
              0)
      :iteration (let [args (:args (dispatch args))
                       json? (some #{"--json"} args)
                       ;; Use --project path or default to cwd
                       project-path (or (second (drop-while #(not= "--project" %) args))
                                        (System/getProperty "user.dir"))]
                   (println (iter-io/load-and-show {:project-path project-path :json? json?}))
                   0)
      :ready (let [result (ready-io/gather-and-compute)]
               (println (ready/format-ready-output result))
               0)
      :orch-tick (let [result (orch-io/gather-and-tick)]
                   (println (orch/format-tick-json result))
                   0)
      :orch-run (let [args (:args (dispatch args))
                       sessions-str (second (drop-while #(not= "--sessions" %) args))
                       session-labels-json (second (drop-while #(not= "--session-labels" %) args))
                       {:keys [result debug-ctx]} (cond
                                sessions-str (orch-io/gather-and-tick-from-session-labels-debug sessions-str)
                                session-labels-json (orch-io/gather-and-tick-with-zombies-debug session-labels-json)
                                :else (orch-io/gather-and-tick-debug))
                       debug-str (orch/format-debug-output
                                   (:registry debug-ctx) (:configs debug-ctx)
                                   (:iterations debug-ctx) (:open-beads debug-ctx) result)]
                  (binding [*out* *err*]
                    (print debug-str)
                    (flush))
                  (println (orch/format-orch-run-json result))
                  0)
      :spawn-msg (let [args (:args (dispatch args))
                       json? (some #{"--json"} args)
                       input (first (remove #(str/starts-with? % "-") args))
                       spawn (when input
                               (json/parse-string input true))]
                   (if spawn
                     (let [spawn (-> spawn
                                     (set/rename-keys {:worker_timeout :worker-timeout}))]
                       (if json?
                         (println (orch/format-spawn-msg-json spawn))
                         (println (orch/spawn-msg spawn)))
                       0)
                     (do (println "Usage: braids spawn-msg '<spawn-json>' [--json]")
                         (println "  Pass a spawn entry JSON (from orch-tick output) as argument.")
                         1)))
      :init (let [args (:args (dispatch args))
                 result (init-io/run-init args)]
              (println (:message result))
              (:exit result))
      :new (let [args (:args (dispatch args))
                 result (new-io/run-new args)]
             (println (:message result))
             (:exit result))
      :config (let [args (:args (dispatch args))
                    sub (first args)
                    sub-args (rest args)]
                (case sub
                  "list" (do (println (config/config-list (config-io/load-config))) 0)
                  "get" (if (empty? sub-args)
                          (do (println "Usage: braids config get <key>") 1)
                          (let [result (config/config-get (config-io/load-config) (first sub-args))]
                            (if (:ok result)
                              (do (println (:ok result)) 0)
                              (do (println (:error result)) 1))))
                  "set" (if (< (count sub-args) 2)
                          (do (println "Usage: braids config set <key> <value>") 1)
                          (let [cfg (config-io/load-config)
                                updated (config/config-set cfg (first sub-args) (second sub-args))]
                            (config-io/save-config! updated)
                            (println (str (first sub-args) " = " (second sub-args)))
                            0))
                  ;; no subcommand or unknown
                  (do (println (config/config-help)) 0)))
      ;; Default for unimplemented commands
      (do (println (str "Command '" (name command) "' not yet implemented.")) 0))))
