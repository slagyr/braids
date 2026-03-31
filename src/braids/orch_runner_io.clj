(ns braids.orch-runner-io
  "IO effects for the braids orch runner: spawning workers, logging to stdout."
  (:require [babashka.process :as proc]
            [clojure.string :as str]
            [braids.orch :as orch]
            [braids.orch-io :as orch-io]
            [braids.orch-runner :as runner]
            [braids.config-io :as config-io]
            [braids.sys :as sys]))

(defn spawn-worker!
  "Fire an openclaw agent worker in the background (fire-and-forget).
   In dry-run mode, logs what would happen without executing."
  [spawn {:keys [dry-run]}]
  (let [{:keys [bead]} spawn
        cfg (config-io/load-config)
        args (runner/build-worker-args cfg spawn)
        bin (or (System/getenv "OPENCLAW_BIN") (sys/openclaw-bin cfg))]
    (if dry-run
      (println (runner/log-line (str "DRY-RUN: would spawn worker for " bead)))
      (do
        (proc/process (into [bin] args)
                      {:out :discard :err :discard
                       :extra-env (sys/subprocess-env cfg)})))))

(defn run-orch!
  "Run one orchestrator tick. Gathers state, computes decisions, acts.
   All output goes to stdout for easy piping/logging.
   Options: {:dry-run bool :verbose bool}"
  ([] (run-orch! {:dry-run true}))
  ([opts]
   (let [{:keys [dry-run verbose]} opts]
     (try
       (let [openclaw-home (or (System/getenv "BRAIDS_OPENCLAW_HOME")
                               (str (System/getProperty "user.home") "/.openclaw"))
             {:keys [result debug-ctx]} (orch-io/gather-and-tick-from-stores-debug openclaw-home)
             action (:action result)
             zombies (seq (:zombies result))]

         ;; Mode banner at the very top
         (let [mode-label (if dry-run "DRY-RUN" "LIVE-RUN")
               ts (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
                           (java.time.LocalDateTime/now))]
           (println (str "-- " mode-label " started at " ts " --")))
         (println)

         ;; Always print the human-readable summary
         (let [debug-str (orch/format-debug-output
                           (:registry debug-ctx) (:configs debug-ctx)
                           (:iterations debug-ctx) (:open-beads debug-ctx) result (:workers debug-ctx))]
           (print debug-str)
           (flush))

         ;; Log and kill zombies if any
         (when zombies
           (doseq [line (runner/format-zombie-log (:zombies result))]
             (println line))
           (doseq [zombie zombies]
             (when (:session-id zombie)
               (try
                 ;; Kill the zombie session via openclaw CLI
                 (let [cfg (config-io/load-config)
                       bin (or (System/getenv "OPENCLAW_BIN") (sys/openclaw-bin cfg))]
                   (proc/process (into [bin] ["sessions" "kill" (:session-id zombie)])
                                 {:out :string :err :string}))
                 (println (runner/log-line (str "Killed zombie session: " (:session-id zombie) " reason=" (:reason zombie))))
                 (catch Exception e
                   (println (runner/log-line (str "Failed to kill zombie session: " (:session-id zombie) " " (.getMessage e)))))))))

         (when (= "spawn" action)
           (let [spawns (:spawns result)
                 cfg (config-io/load-config)]
             (doseq [line (runner/format-spawn-log cfg result)]
               (println line))
             (doseq [spawn spawns]
               (spawn-worker! spawn opts))))

         ;; Footer
         (let [mode-label (if dry-run "DRY-RUN" "LIVE-RUN")
               ts (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
                           (java.time.LocalDateTime/now))]
           (println (str "-- " mode-label " completed at " ts " --")))

         0)
       (catch Exception e
         (println (runner/log-line (str "ERROR: " (.getMessage e))))
         1)))))

(defn run-orch-command!
  "Parse CLI args and run the orch command. Returns exit code."
  [args]
  (let [opts (runner/parse-cli-args (vec args))]
    (if (:error opts)
      (do
        (println (:error opts))
        1)
      (run-orch! opts))))

(defn -main
  "Babashka entry point"
  [& args]
  (System/exit (run-orch-command! args)))
