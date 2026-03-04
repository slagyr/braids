(ns braids.orch-runner
  "Pure functions for the braids orch runner.
   Converts orchestrator tick results into openclaw agent spawn commands."
  (:require [clojure.string :as str]))

(defn build-worker-args
  "Build the openclaw agent CLI args for a spawn entry.
   Returns a vector of strings."
  [config {:keys [bead path iteration channel worker-agent thinking worker-timeout] :as spawn}]
  (let [task (build-worker-task spawn)
        session-id (str (java.util.UUID/randomUUID))
        thinking (or thinking (:worker-thinking config) "high")
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
      (vec (concat ["agent" "--agent" worker-agent] (rest base-args)))
      (vec base-args))))

(defn log-line
  "Format a log line (no timestamp — keep output clean)."
  [msg]
  msg)

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
          "--dry-run"   (recur (rest remaining) (assoc opts :dry-run true))
          "--confirmed" (recur (rest remaining) (assoc opts :dry-run false))
          "--verbose"   (recur (rest remaining) (assoc opts :verbose true))
          {:error (str "Unknown arg: " arg "\n\nUsage: braids orch [--dry-run] [--confirmed] [--verbose]\n\n  --dry-run    Show what would happen without spawning (default)\n  --confirmed  Actually spawn workers\n  --verbose    Print detailed project/bead information")})))))
