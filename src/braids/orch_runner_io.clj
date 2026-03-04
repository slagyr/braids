(ns braids.orch-runner-io
  "IO effects for the braids orch runner: spawning workers, logging to stdout."
  (:require [babashka.process :as proc]
            [clojure.string :as str]
            [braids.orch :as orch]
            [braids.orch-io :as orch-io]
            [braids.orch-runner :as runner]
            [braids.sys :as sys]))

(def ^:private openclaw-bin
  "Full path to the openclaw binary. Use OPENCLAW_BIN env var to override."
  (or (System/getenv "OPENCLAW_BIN") "/Users/zane/.local/bin/openclaw"))

(defn spawn-worker!
  "Fire an openclaw agent worker in the background (fire-and-forget).
   In dry-run mode, logs what would happen without executing."
  [spawn {:keys [dry-run]}]
  (let [{:keys [bead]} spawn
        args (runner/build-worker-args spawn)]
    (if dry-run
      (println (runner/log-line (str "DRY-RUN: would spawn worker for " bead)))
      (do
        (proc/process (into [openclaw-bin] args)
                      {:out :inherit :err :inherit :env (sys/subprocess-env)})
        (println (runner/log-line (str "Spawned worker: bead=" bead)))))))

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
         (let [ts (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
                           (java.time.LocalDateTime/now))]
           (println (if dry-run
                      (str "── DRY-RUN ── " ts)
                      (str "── CONFIRMED ── " ts))))
         (println)

         ;; Always print the human-readable summary
         (let [debug-str (orch/format-debug-output
                           (:registry debug-ctx) (:configs debug-ctx)
                           (:iterations debug-ctx) (:open-beads debug-ctx) result)]
           (print debug-str)
           (flush))

         ;; Log zombies if any
         (when zombies
           (doseq [line (runner/format-zombie-log (:zombies result))]
             (println line)))

         (when (= "spawn" action)
           (let [spawns (:spawns result)]
             (doseq [line (runner/format-spawn-log result)]
               (println line))
             (doseq [spawn spawns]
               (spawn-worker! spawn opts))))

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
