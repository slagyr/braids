(ns braids.orch-runner
  "Pure functions for the braids orch runner.
   Converts orchestrator tick results into openclaw agent spawn commands."
  (:require [clojure.string :as str]))

(def worker-task-template
  "You are a project worker for the braids skill. Read and follow ~/.openclaw/skills/braids/references/worker.md

Project: %s
Bead: %s
Iteration: %s
Channel: %s")

(defn build-worker-task
  "Build the task message for a worker spawn."
  [{:keys [path bead iteration channel]}]
  (format worker-task-template path bead iteration channel))

(defn build-worker-args
  "Build the openclaw agent CLI args for a spawn entry.
   Returns a vector of strings."
  [{:keys [bead path iteration channel worker-agent thinking worker-timeout] :as spawn}]
  (let [task (build-worker-task spawn)
        session-id (str (java.util.UUID/randomUUID))
        thinking (or thinking "low")
        timeout (str (or worker-timeout 1800))
        base-args ["agent"
                   "--message" task
                   "--session-id" session-id
                   "--thinking" thinking
                   "--timeout" timeout
                   "--deliver"
                   "--reply-channel" "discord"
                   "--reply-to" channel]]
    (if (and worker-agent (not (str/blank? worker-agent)))
      (vec (concat ["--agent" worker-agent] base-args))
      (vec base-args))))

(defn log-line
  "Format a log line with ISO timestamp."
  [msg]
  (let [ts (.format (java.time.LocalDateTime/now)
                    (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss"))]
    (str "[" ts "] " msg)))

(defn format-spawn-log
  "Format log lines for a spawn action. Returns vector of strings."
  [tick-result]
  (let [spawns (:spawns tick-result)
        n (count spawns)]
    (into [(log-line (str "Spawning " n " worker(s)"))]
          (map (fn [{:keys [bead worker-agent]}]
                 (log-line (str "  → bead=" bead " agent=" (or worker-agent "default"))))
               spawns))))

(defn format-idle-log
  "Format log lines for an idle action. Returns vector of strings."
  [{:keys [reason]}]
  [(log-line (str "Idle: " reason))])

(defn format-zombie-log
  "Format log lines for zombies. Returns vector of strings."
  [zombies]
  (when (seq zombies)
    (into [(log-line (str "Found " (count zombies) " zombie(s)"))]
          (map (fn [{:keys [bead reason]}]
                 (log-line (str "  zombie: " bead " reason=" reason)))
               zombies))))

(defn parse-cli-args
  "Parse CLI args vector into options map.
   Defaults to dry-run mode. Use --run to actually spawn workers.
   Returns {:dry-run bool :verbose bool} or {:error string}."
  [args]
  (loop [remaining args
         opts {:dry-run true :verbose false}]
    (if (empty? remaining)
      opts
      (let [arg (first remaining)]
        (case arg
          "--dry-run" (recur (rest remaining) (assoc opts :dry-run true))
          "--run" (recur (rest remaining) (assoc opts :dry-run false))
          "--verbose" (recur (rest remaining) (assoc opts :verbose true))
          {:error (str "Unknown arg: " arg "\n\nUsage: braids orch [--dry-run] [--run] [--verbose]\n\n  --dry-run   Show what would happen without spawning (default)\n  --run       Actually spawn workers\n  --verbose   Print detailed project/bead information")})))))
