(ns braids.core
  (:require [braids.config :as config]
            [braids.config-io :as config-io]
            [braids.init-io :as init-io]
            [braids.iteration-io :as iter-io]
            [braids.list-io :as list-io]
            [braids.new-io :as new-io]
            [braids.orch-runner-io :as orch-runner-io]
            [braids.ready :as ready]
            [braids.ready-io :as ready-io]
            [clojure.string :as str]))

(def commands
  {"list"      {:command :list
                :doc     "Show projects with status, iterations, and progress"
                :help    "Usage: braids list [--json]\n\nShow all registered projects with status, iterations, and progress.\n\nOptions:\n  --json    Output as JSON"}
   "iteration" {:command :iteration
                :doc     "Show active iteration and bead statuses"
                :help    "Usage: braids iteration [--project <path>] [--json]\n\nShow the active iteration and bead statuses for a project.\n\nOptions:\n  --project <path>  Project path (default: current directory)\n  --json            Output as JSON"}
   "ready"     {:command :ready
                :doc     "List beads ready to work"
                :help    "Usage: braids ready\n\nList all beads that are unblocked and ready for work across all active projects."}
   "new"       {:command :new
                :doc     "Create a new project"
                :help    "Usage: braids new <slug> [--path <path>]\n\nCreate a new braids project with scaffolding."}
   "init"      {:command :init
                :doc     "First-time setup for braids"
                :help    "Usage: braids init\n\nRun first-time setup: create directories, install bd, configure BRAIDS_HOME."}
   "config"    {:command :config
                :doc     "Get/set/list braids configuration"
                :help    "Usage: braids config <subcommand> [args]\n\nSubcommands:\n  list           Show all config values\n  get <key>      Get a config value\n  set <key> <v>  Set a config value"}
   "help"      {:command :help
                :doc     "Show this help message"}
   "orch"      {:command :orch
                :doc     "Run orchestrator: compute spawns, start workers (defaults to dry run)"
                :help    "Usage: braids orch [--dry-run] [--confirmed] [--verbose]\n\nRun the orchestrator: scan projects, compute spawn decisions, start workers.\nDefaults to dry-run mode (no workers spawned).\n\nOptions:\n  --dry-run    Show what would happen without spawning (default)\n  --confirmed  Actually spawn workers\n  --verbose    Print detailed project/bead information"}})

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
      (merge {:command  (get-in commands [first-arg :command])
              :args     rest-args
              :cmd-name first-arg})

      :else
      {:command :unknown :input first-arg})))

;; --- Command handlers: each takes sub-args and returns exit code ---

(defn- run-help [sub-args]
  (let [target   (first sub-args)
        cmd-help (when target (get-in commands [target :help]))]
    (println (or cmd-help (help-text)))
    0))

(defn- run-unknown [input]
  (println (str "Unknown command: " input))
  (println)
  (println (help-text))
  1)

(defn- run-list [sub-args]
  (println (list-io/load-and-list {:json? (some #{"--json"} sub-args)}))
  0)

(defn- run-iteration [sub-args]
  (let [project-path (or (second (drop-while #(not= "--project" %) sub-args))
                         (System/getProperty "user.dir"))]
    (println (iter-io/load-and-show {:project-path project-path
                                     :json?        (some #{"--json"} sub-args)}))
    0))

(defn- run-ready [_sub-args]
  (println (ready/format-ready-output (ready-io/gather-and-compute)))
  0)

(defn- run-init [sub-args]
  (let [result (init-io/run-init (vec sub-args))]
    (println (:message result))
    (:exit result)))

(defn- run-new [sub-args]
  (let [result (new-io/run-new (vec sub-args))]
    (println (:message result))
    (:exit result)))

(defn- run-config-get [sub-sub-args]
  (if (empty? sub-sub-args)
    (do (println "Usage: braids config get <key>") 1)
    (let [result (config/config-get (config-io/load-config) (first sub-sub-args))]
      (println (or (:ok result) (:error result)))
      (if (:ok result) 0 1))))

(defn- run-config-set [sub-sub-args]
  (if (< (count sub-sub-args) 2)
    (do (println "Usage: braids config set <key> <value>") 1)
    (let [cfg     (config-io/load-config)
          updated (config/config-set cfg (first sub-sub-args) (second sub-sub-args))]
      (config-io/save-config! updated)
      (println (str (first sub-sub-args) " = " (second sub-sub-args)))
      0)))

(defn- run-config [sub-args]
  (case (first sub-args)
    "list" (do (println (config/config-list (config-io/load-config))) 0)
    "get" (run-config-get (rest sub-args))
    "set" (run-config-set (rest sub-args))
    (do (println (config/config-help)) 0)))

(defn- run-orch [sub-args]
  (orch-runner-io/run-orch-command! (vec sub-args)))

(def ^:private command-handlers
  {:help      run-help
   :list      run-list
   :iteration run-iteration
   :ready     run-ready
   :init      run-init
   :new       run-new
   :config    run-config
   :orch      run-orch})

(defn- show-subcommand-help [cmd-name]
  (println (or (get-in commands [cmd-name :help]) (help-text)))
  0)

(defn run [args]
  (let [{:keys [command input cmd-name]} (dispatch args)
        sub-args (:args (dispatch args))]
    (cond
      (= command :unknown)
      (run-unknown input)

      (and sub-args (subcommand-help? sub-args))
      (show-subcommand-help cmd-name)

      :else
      (if-let [handler (get command-handlers command)]
        (handler sub-args)
        (do (println (str "Command '" (name command) "' not yet implemented.")) 0)))))
