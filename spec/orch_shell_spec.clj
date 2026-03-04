(ns orch-shell-spec
  "Tests for the braids orch CLI command.
   Tests both pure functions and subprocess integration."
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.string :as str]
            [braids.orch-runner :as runner]))

;; ── Helper to run `bb braids orch` as a subprocess ──

(def project-root (str (fs/canonicalize ".")))

(defn run-braids-orch
  "Run `bb braids orch` with optional extra args and env vars.
   Returns {:exit :out :err}."
  [args & {:keys [env]}]
  (let [base-env {"PATH" (System/getenv "PATH")
                  "HOME" (System/getenv "HOME")}
        merged-env (merge base-env (or env {}))
        cmd (into ["bb" "braids" "orch"] args)
        result (apply proc/shell {:dir project-root
                                  :out :string
                                  :err :string
                                  :continue true
                                  :env merged-env}
                      cmd)]
    {:exit (:exit result) :out (:out result) :err (:err result)}))

;; ── Pure function tests (orch-runner) ──

(describe "braids.orch-runner (pure functions)"

  (describe "parse-cli-args"

    (it "returns defaults for empty args (dry-run=true)"
      (should= {:dry-run true :verbose false} (runner/parse-cli-args [])))

    (it "parses --dry-run"
      (should= {:dry-run true :verbose false} (runner/parse-cli-args ["--dry-run"])))

    (it "parses --run"
      (should= {:dry-run false :verbose false} (runner/parse-cli-args ["--run"])))

    (it "parses --verbose"
      (should= {:dry-run true :verbose true} (runner/parse-cli-args ["--verbose"])))

    (it "returns error for unknown arg"
      (let [result (runner/parse-cli-args ["--bogus"])]
        (should-contain :error result)
        (should (str/includes? (:error result) "--bogus")))))

  (describe "build-worker-task"

    (it "includes worker.md reference"
      (let [task (runner/build-worker-task
                   {:path "~/p" :bead "b" :iteration "1" :channel "c"})]
        (should (str/includes? task "worker.md"))))

    (it "includes all spawn fields"
      (let [task (runner/build-worker-task
                   {:path "~/Projects/test" :bead "test-abc" :iteration "001" :channel "12345"})]
        (should (str/includes? task "~/Projects/test"))
        (should (str/includes? task "test-abc"))
        (should (str/includes? task "001"))
        (should (str/includes? task "12345")))))

  (describe "build-worker-args"

    (it "includes required openclaw agent args"
      (let [args (runner/build-worker-args {}
                   {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 1800})]
        (should (some #{"agent"} args))
        (should (some #{"--message"} args))
        (should (some #{"--session-id"} args))
        (should (some #{"--thinking"} args))
        (should (some #{"--timeout"} args))))

    (it "includes --agent when worker-agent is set"
      (let [args (runner/build-worker-args {}
                   {:path "~/p" :bead "b" :iteration "1" :channel "c"
                    :worker-timeout 1800 :worker-agent "scrapper"})]
        (should (some #{"--agent"} args))
        (should= "scrapper" (nth args (inc (.indexOf args "--agent"))))))

    (it "omits --agent when worker-agent is nil"
      (let [args (runner/build-worker-args {}
                   {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 1800})]
        (should-not (some #{"--agent"} args))))

    (it "uses default thinking=high"
      (let [args (runner/build-worker-args {}
                   {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 1800})]
        (should= "high" (nth args (inc (.indexOf args "--thinking"))))))

    (it "generates unique session IDs"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"}
            args1 (runner/build-worker-args {} spawn)
            args2 (runner/build-worker-args {} spawn)]
        (should-not= (nth args1 (inc (.indexOf args1 "--session-id")))
                     (nth args2 (inc (.indexOf args2 "--session-id")))))))

  (describe "log-line"

    (it "returns message without timestamp"
      (should= "test" (runner/log-line "test")))

    (it "includes the message"
      (should= "hello world" (runner/log-line "hello world"))))

  (describe "format-spawn-log"

    (it "shows worker count"
      (let [lines (runner/format-spawn-log {:action "spawn" :spawns [{:bead "b1"} {:bead "b2"}]})]
        (should (some #(str/includes? % "2 worker") lines))))

    (it "includes bead IDs"
      (let [lines (runner/format-spawn-log {:action "spawn" :spawns [{:bead "my-bead-abc"}]})]
        (should (some #(str/includes? % "my-bead-abc") lines)))))

  (describe "format-idle-log"

    (it "includes reason"
      (let [lines (runner/format-idle-log {:reason "all-at-capacity"})]
        (should (some #(str/includes? % "all-at-capacity") lines))))

    (it "includes idle"
      (let [lines (runner/format-idle-log {:reason "no-ready-beads"})]
        (should (some #(str/includes? % "Idle") lines)))))

  (describe "format-zombie-log"

    (it "returns nil for empty zombies"
      (should-be-nil (runner/format-zombie-log [])))

    (it "includes zombie count and details"
      (let [lines (runner/format-zombie-log [{:bead "z1" :reason "bead-closed"}
                                             {:bead "z2" :reason "timeout"}])]
        (should (some #(str/includes? % "2 zombie") lines))
        (should (some #(str/includes? % "z1") lines))
        (should (some #(str/includes? % "bead-closed") lines))))))

;; ── CLI integration tests (braids orch subprocess) ──

(describe "braids orch (CLI)"

  (it "rejects unknown arguments"
    (let [result (run-braids-orch ["--bogus"])]
      (should= 1 (:exit result))
      (should (str/includes? (str (:out result) (:err result)) "Unknown arg"))))

  (it "outputs to stdout (not log files)"
    ;; All output should go to stdout now
    (let [tmp (str (fs/create-temp-dir {:prefix "braids-orch-cli-test-"}))
          state-dir (str tmp "/state")]
      (try
        (fs/create-dirs state-dir)
        (spit (str state-dir "/registry.edn") "{:projects []}")
        (let [result (run-braids-orch []
                       :env {"BRAIDS_STATE_HOME" state-dir
                             "BRAIDS_OPENCLAW_HOME" tmp})]
          (should= 0 (:exit result))
          (should (str/includes? (:out result) "DRY-RUN")))
        (finally
          (proc/shell {:continue true} "rm" "-rf" tmp)))))

  (it "defaults to dry-run mode"
    (let [tmp (str (fs/create-temp-dir {:prefix "braids-orch-dryrun-test-"}))
          state-dir (str tmp "/state")]
      (try
        (fs/create-dirs state-dir)
        (spit (str state-dir "/registry.edn") "{:projects []}")
        (let [result (run-braids-orch []
                       :env {"BRAIDS_STATE_HOME" state-dir
                             "BRAIDS_OPENCLAW_HOME" tmp})]
          (should= 0 (:exit result))
          (should (str/includes? (:out result) "DRY-RUN")))
        (finally
          (proc/shell {:continue true} "rm" "-rf" tmp))))))
