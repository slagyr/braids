(ns braids.core
  (:require [clojure.string :as str]
            [braids.ready :as ready]
            [braids.ready-io :as ready-io]
            [braids.list-io :as list-io]
            [braids.iteration-io :as iter-io]
            [braids.new-io :as new-io]
            [braids.init-io :as init-io]
            [braids.config :as config]
            [braids.config-io :as config-io]
            [braids.orch-runner-io :as orch-runner-io]))

(def commands
  {"list"      {:command :list      :doc "Show projects with status, iterations, and progress"
                :help "Usage: braids list [--json]\n\nShow all registered projects with status, iterations, and progress.\n\nOptions:\n  --json    Output as JSON"}
   "iteration" {:command :iteration :doc "Show active iteration and bead statuses"
                :help "Usage: braids iteration [--project <path>] [--json]\n\nShow the active iteration and bead statuses for a project.\n\nOptions:\n  --project <path>  Project path (default: current directory)\n  --json            Output as JSON"}
   "ready"     {:command :ready     :doc "List beads ready to work"
                :help "Usage: braids ready\n\nList all beads that are unblocked and ready for work across all active projects."}
   "new"       {:command :new       :doc "Create a new project"
                :help "Usage: braids new <slug> [--path <path>]\n\nCreate a new braids project with scaffolding."}
   "init"      {:command :init      :doc "First-time setup for braids"
                :help "Usage: braids init\n\nRun first-time setup: create directories, install bd, configure BRAIDS_HOME."}
   "config"    {:command :config    :doc "Get/set/list braids configuration"
                :help "Usage: braids config <subcommand> [args]\n\nSubcommands:\n  list           Show all config values\n  get <key>      Get a config value\n  set <key> <v>  Set a config value"}
   "help"      {:command :help      :doc "Show this help message"}
   "orch"      {:command :orch      :doc "Run orchestrator: compute spawns, start workers (defaults to dry run)"
                :help "Usage: braids orch [--dry-run] [--verbose]\n\nRun the orchestrator: scan projects, compute spawn decisions, start workers.\nDefaults to dry-run mode (no workers spawned).\n\nOptions:\n  --dry-run   Show what would happen without spawning (default)\n  --verbose   Print detailed output to stdout"}})

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

(defn- subcommand-help? [args]
  (some #{"--help" "-h"} args))

(defn dispatch [args]
  (let [first-arg (first args)
        rest-args (vec (rest args))]
    (cond
      (or (nil? first-arg) (= first-arg "--help") (= first-arg "-h"))
      {:command :help}

      (contains? commands first-arg)
      (merge {:command (get-in commands [first-arg :command])
              :args rest-args
              :cmd-name first-arg})

      :else
      {:command :unknown :input first-arg})))

(defn run [args]
  (let [{:keys [command input cmd-name] :as dispatched} (dispatch args)
        sub-args (:args dispatched)]
    ;; Handle --help for any sub-command
    (if (and (not= command :help) (not= command :unknown) (subcommand-help? sub-args))
      (let [help-str (get-in commands [cmd-name :help])]
        (if help-str
          (do (println help-str) 0)
          (do (println (help-text)) 0)))
      (case command
        :help (do (println (help-text)) 0)
        :unknown (do (println (str "Unknown command: " input))
                     (println)
                     (println (help-text))
                     1)
        :list (let [json? (some #{"--json"} sub-args)]
                (println (list-io/load-and-list {:json? json?}))
                0)
        :iteration (let [json? (some #{"--json"} sub-args)
                         project-path (or (second (drop-while #(not= "--project" %) sub-args))
                                          (System/getProperty "user.dir"))]
                     (println (iter-io/load-and-show {:project-path project-path :json? json?}))
                     0)
        :ready (let [result (ready-io/gather-and-compute)]
                 (println (ready/format-ready-output result))
                 0)
        :init (let [result (init-io/run-init (vec sub-args))]
                (println (:message result))
                (:exit result))
        :new (let [result (new-io/run-new (vec sub-args))]
               (println (:message result))
               (:exit result))
        :config (let [sub (first sub-args)
                      sub-sub-args (rest sub-args)]
                  (case sub
                    "list" (do (println (config/config-list (config-io/load-config))) 0)
                    "get" (if (empty? sub-sub-args)
                            (do (println "Usage: braids config get <key>") 1)
                            (let [result (config/config-get (config-io/load-config) (first sub-sub-args))]
                              (if (:ok result)
                                (do (println (:ok result)) 0)
                                (do (println (:error result)) 1))))
                    "set" (if (< (count sub-sub-args) 2)
                            (do (println "Usage: braids config set <key> <value>") 1)
                            (let [cfg (config-io/load-config)
                                  updated (config/config-set cfg (first sub-sub-args) (second sub-sub-args))]
                              (config-io/save-config! updated)
                              (println (str (first sub-sub-args) " = " (second sub-sub-args)))
                              0))
                    ;; no subcommand or unknown
                    (do (println (config/config-help)) 0)))
        :orch (orch-runner-io/run-orch-command! (vec sub-args))
        ;; Default for unimplemented commands
        (do (println (str "Command '" (name command) "' not yet implemented.")) 0)))))
