(ns braids.orch-runner-io
  "IO effects for the braids orch runner: spawning workers, disabling cron, logging."
  (:require [babashka.process :as proc]
            [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]
            [braids.orch-runner :as runner]
            [braids.orch-io :as orch-io]))

(def default-log-file "/tmp/braids-orch.log")

(defn- log-file-path []
  (or (System/getenv "BRAIDS_ORCH_LOG") default-log-file))

(defn write-log!
  "Append lines to the log file."
  [lines]
  (let [path (log-file-path)]
    (spit path (str (str/join "\n" lines) "\n") :append true)))

(defn- verbose-log! [verbose? lines]
  (when verbose?
    (doseq [line lines]
      (binding [*out* *err*]
        (println line))))
  (write-log! lines))

(defn spawn-worker!
  "Fire an openclaw agent worker in the background (fire-and-forget).
   In dry-run mode, logs what would happen without executing."
  [spawn opts log-fn]
  (let [{:keys [dry-run]} opts
        {:keys [bead]} spawn
        args (runner/build-worker-args spawn)]
    (if dry-run
      (log-fn [(runner/log-line (str "DRY-RUN: would spawn openclaw agent for " bead))])
      (do
        ;; Fire and forget — redirect output to log file
        (proc/process (into ["openclaw"] args)
                      {:out (log-file-path) :err (log-file-path)})
        (log-fn [(runner/log-line (str "Spawned worker: bead=" bead))])))))

(defn find-cron-id!
  "Find the braids-orchestrator cron job ID via openclaw CLI.
   Returns the ID string or nil."
  []
  (try
    (let [result (proc/shell {:out :string :err :string :continue true}
                             "openclaw" "cron" "list" "--json")]
      (when (zero? (:exit result))
        (-> (json/parse-string (:out result) true)
            :jobs
            (->> (filter #(= "braids-orchestrator" (:name %)))
                 first
                 :id))))
    (catch Exception _ nil)))

(defn disable-cron!
  "Disable the braids-orchestrator cron job.
   In dry-run mode, logs what would happen without executing."
  [opts log-fn]
  (let [{:keys [dry-run]} opts
        cron-id (find-cron-id!)]
    (if cron-id
      (if dry-run
        (log-fn [(runner/log-line (str "DRY-RUN: would disable cron " cron-id))])
        (do
          (try
            (proc/shell {:continue true} "openclaw" "cron" "disable" cron-id)
            (log-fn [(runner/log-line (str "Disabled cron job " cron-id))])
            (catch Exception e
              (log-fn [(runner/log-line (str "WARN: failed to disable cron: " (.getMessage e)))])))))
      (log-fn [(runner/log-line "WARN: braids-orchestrator cron job not found")]))))

(defn run-orch!
  "Run one orchestrator tick. Calls orch-io to compute the tick result,
   then acts on spawns or idle based on the result.
   Options: {:dry-run bool :verbose bool}"
  ([] (run-orch! {}))
  ([opts]
   (let [{:keys [dry-run verbose]} opts
         log-fn (fn [lines] (verbose-log! verbose lines))]
     (try
       ;; Get tick result from stores
       (let [openclaw-home (or (System/getenv "BRAIDS_OPENCLAW_HOME")
                               (str (System/getProperty "user.home") "/.openclaw"))
             tick-result (orch-io/gather-and-tick-from-stores openclaw-home)
             action (:action tick-result)
             zombies (seq (:zombies tick-result))]
        (println (json/generate-string tick-result))

         ;; Log zombies if any
         (when zombies
           (log-fn (runner/format-zombie-log (:zombies tick-result))))

         (cond
           (= "spawn" action)
           (let [spawns (:spawns tick-result)]
             (log-fn (runner/format-spawn-log tick-result))
             (doseq [spawn spawns]
               (spawn-worker! spawn opts log-fn))
             (log-fn [(runner/log-line "All workers spawned")]))

           (= "idle" action)
           (do
             (log-fn (runner/format-idle-log tick-result))
             (when (:disable-cron tick-result)
               (disable-cron! opts log-fn)))

           :else
           (log-fn [(runner/log-line (str "WARN: unknown action: " action))]))

         (log-fn [(runner/log-line "Orchestrator tick complete")])
         0)
       (catch Exception e
         (let [lines [(runner/log-line (str "ERROR: " (.getMessage e)))]]
           (verbose-log! verbose lines))
         1)))))

(defn run-orch-command!
  "Parse CLI args and run the orch command. Returns exit code."
  [args]
  (let [opts (runner/parse-cli-args (vec args))]
    (if (:error opts)
      (do
        (binding [*out* *err*]
          (println (:error opts)))
        1)
      (run-orch! opts))))

(defn -main
  "Babashka entry point"
  [& args]
  (System/exit (run-orch-command! args)))
