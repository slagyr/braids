(ns braids.orch-runner-spec
  (:require [speclj.core :refer :all]
            [braids.orch-runner :as runner]
            [clojure.string :as str]))

(describe "braids.orch-runner"

  (context "parse-cli-args"

    (it "returns defaults for empty args (dry-run=true)"
      (should= {:dry-run true :verbose false} (runner/parse-cli-args [])))

    (it "parses --dry-run"
      (should= {:dry-run true :verbose false} (runner/parse-cli-args ["--dry-run"])))

    (it "parses --run"
      (should= {:dry-run false :verbose false} (runner/parse-cli-args ["--run"])))

    (it "parses --verbose"
      (should= {:dry-run true :verbose true} (runner/parse-cli-args ["--verbose"])))

    (it "parses --run and --verbose"
      (should= {:dry-run false :verbose true} (runner/parse-cli-args ["--run" "--verbose"])))

    (it "returns error for unknown arg"
      (let [result (runner/parse-cli-args ["--bogus"])]
        (should-contain :error result)
        (should (str/includes? (:error result) "--bogus")))))

  (context "build-worker-task"

    (it "formats the task with all fields"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001" :channel "12345"}
            task (runner/build-worker-task spawn)]
        (should (str/includes? task "~/Projects/test"))
        (should (str/includes? task "test-abc"))
        (should (str/includes? task "001"))
        (should (str/includes? task "12345"))
        (should (str/includes? task "worker.md")))))

  (context "build-worker-args"

    (it "includes required openclaw agent args"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001"
                   :channel "12345" :worker-timeout 1800}
            args (runner/build-worker-args {} spawn)]
        (should (some #{"agent"} args))
        (should (some #{"--message"} args))
        (should (some #{"--session-id"} args))
        (should (some #{"--thinking"} args))
        (should (some #{"--timeout"} args))))

    (it "includes --agent when worker-agent is set"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001"
                   :channel "12345" :worker-timeout 1800 :worker-agent "scrapper"}
            args (runner/build-worker-args {} spawn)]
        (should (some #{"--agent"} args))
        (let [agent-idx (.indexOf args "--agent")]
          (should= "scrapper" (nth args (inc agent-idx))))))

    (it "omits --agent when worker-agent is nil"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001"
                   :channel "12345" :worker-timeout 1800}
            args (runner/build-worker-args {} spawn)]
        (should-not (some #{"--agent"} args))))

    (it "uses default thinking=high"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 1800}
            args (runner/build-worker-args {} spawn)
            thinking-idx (.indexOf args "--thinking")]
        (should= "high" (nth args (inc thinking-idx)))))

    (it "uses provided thinking level"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"
                   :worker-timeout 1800 :thinking "high"}
            args (runner/build-worker-args {} spawn)
            thinking-idx (.indexOf args "--thinking")]
        (should= "high" (nth args (inc thinking-idx)))))

    (it "uses default timeout=1800 when not provided"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"}
            args (runner/build-worker-args {} spawn)
            timeout-idx (.indexOf args "--timeout")]
        (should= "1800" (nth args (inc timeout-idx)))))

    (it "uses provided timeout"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 3600}
            args (runner/build-worker-args {} spawn)
            timeout-idx (.indexOf args "--timeout")]
        (should= "3600" (nth args (inc timeout-idx)))))

    (it "generates deterministic session ID based on bead-id"
      (let [spawn {:path "~/p" :bead "proj-abc" :iteration "1" :channel "c"}
            args (runner/build-worker-args {} spawn)
            session-idx (.indexOf args "--session-id")]
        (should= "braids-proj-abc-worker" (nth args (inc session-idx)))))

    (it "generates same session ID for same bead across calls"
      (let [spawn {:path "~/p" :bead "proj-abc" :iteration "1" :channel "c"}
            args1 (runner/build-worker-args {} spawn)
            args2 (runner/build-worker-args {} spawn)
            session-idx1 (.indexOf args1 "--session-id")
            session-idx2 (.indexOf args2 "--session-id")]
        (should= (nth args1 (inc session-idx1))
                 (nth args2 (inc session-idx2)))))))

  (context "log-line"

    (it "returns the message as-is (no timestamp)"
      (should= "hello" (runner/log-line "hello"))))

  (context "format-spawn-log"

    (it "shows worker count"
      (let [result {:action "spawn"
                    :spawns [{:bead "b1"} {:bead "b2"}]}
            lines (runner/format-spawn-log result)]
        (should (some #(str/includes? % "2 worker") lines))))

    (it "includes bead IDs"
      (let [result {:action "spawn" :spawns [{:bead "my-bead-123"}]}
            lines (runner/format-spawn-log result)]
        (should (some #(str/includes? % "my-bead-123") lines)))))

  (context "format-idle-log"

    (it "includes reason"
      (let [lines (runner/format-idle-log {:reason "all-at-capacity"})]
        (should (some #(str/includes? % "all-at-capacity") lines))))

    (it "includes idle message"
      (let [lines (runner/format-idle-log {:reason "no-ready-beads"})]
        (should (some #(str/includes? % "Idle") lines)))))

  (context "format-zombie-log"

    (it "returns nil for empty zombies"
      (should-be-nil (runner/format-zombie-log [])))

    (it "includes zombie count"
      (let [zombies [{:bead "z1" :reason "bead-closed"} {:bead "z2" :reason "timeout"}]
            lines (runner/format-zombie-log zombies)]
        (should (some #(str/includes? % "2 zombie") lines))))

    (it "includes bead IDs and reasons"
      (let [zombies [{:bead "z1" :reason "bead-closed"}]
            lines (runner/format-zombie-log zombies)]
        (should (some #(str/includes? % "z1") lines))
        (should (some #(str/includes? % "bead-closed") lines)))))
