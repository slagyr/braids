;; mutation-tested: 2026-03-07
(ns braids.orch-runner
  "Pure functions for the braids orch runner.
   Converts orchestrator tick results into openclaw agent spawn commands."
  (:require [clojure.string :as str]))

(def worker-task-template
  "You are a project worker for the braids skill. Read and follow ~/.openclaw/skills/braids/references/worker.md

Project: %s
Bead: %s
Iteration: %s
Channel: %s
Agent: %s
Model: %s
Announcement-Prefix: %s")

(defn build-worker-task
  "Build the task message for a worker spawn."
  [{:keys [path bead iteration channel worker-agent worker-model]}]
  (let [agent (or worker-agent "default")
        model (or worker-model "default")
        prefix (str "Braids worker " agent " | " model " | " bead ":")]
    (format worker-task-template path bead iteration channel agent model prefix)))

(defn build-worker-session-key
  "Build the isolated session key for a worker spawn.
   Pattern: agent:<agent-id>:braids-<bead>-worker
   This creates a session in the agent's store that is isolated
   from the main DM session (which may carry Discord channel context)."
  [worker-agent bead]
  (let [agent-id (or worker-agent "main")]
    (str "agent:" agent-id ":braids-" bead "-worker")))

(defn build-worker-args
  "Build the openclaw agent CLI args for a spawn entry.
   Returns a vector of strings."
  [config {:keys [bead path iteration channel worker-agent thinking worker-timeout] :as spawn}]
  (let [task (build-worker-task spawn)
        session-id (str "braids-" bead "-worker")
        thinking (or thinking (:worker-thinking config) "high")
        timeout (str (or worker-timeout 1800))
        base-args ["agent"
                   "--message" task
                   "--session-id" session-id
                   "--thinking" thinking
                   "--timeout" timeout]]
    (if (and worker-agent (not (str/blank? worker-agent)))
      (into base-args ["--agent" worker-agent])
      (vec base-args))))

(defn log-line
  "Format a log line (no timestamp — keep output clean)."
  [msg]
  msg)

(defn- redact-message-arg
  "Replace the --message value in an args vector with \"<task>\" for log readability."
  [args]
  (let [msg-idx (.indexOf ^java.util.List args "--message")]
    (if (and (>= msg-idx 0) (< (inc msg-idx) (count args)))
      (assoc (vec args) (inc msg-idx) "<task>")
      (vec args))))

(defn format-spawn-log
  "Format log lines for a spawn action. Returns vector of strings.
   Calls build-worker-args for each spawn to log the actual command."
  [config tick-result]
  (let [spawns (:spawns tick-result)
        n (count spawns)]
    (into [(log-line (str "Spawning " n " worker(s)"))]
          (map (fn [spawn]
                 (let [args (build-worker-args config spawn)
                       redacted (redact-message-arg args)]
                   (log-line (str "  → openclaw " (str/join " " redacted)))))
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
          "--run"       (recur (rest remaining) (assoc opts :dry-run false))
          "--verbose"   (recur (rest remaining) (assoc opts :verbose true))
          {:error (str "Unknown arg: " arg "\n\nUsage: braids orch [--dry-run] [--confirmed] [--verbose]\n\n  --dry-run    Show what would happen without spawning (default)\n  --confirmed  Actually spawn workers\n  --verbose    Print detailed project/bead information")})))))
