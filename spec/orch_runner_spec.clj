(ns orch-runner-spec
  (:require [speclj.core :refer :all]
            [braids.orch-runner :as runner]
            [clojure.string :as str]))

(describe "braids.orch-runner"

  (describe "parse-cli-args"

    (it "returns defaults for empty args"
      (should= {:dry-run false :verbose false} (runner/parse-cli-args [])))

    (it "parses --dry-run"
      (should= {:dry-run true :verbose false} (runner/parse-cli-args ["--dry-run"])))

    (it "parses --verbose"
      (should= {:dry-run false :verbose true} (runner/parse-cli-args ["--verbose"])))

    (it "parses both flags"
      (should= {:dry-run true :verbose true} (runner/parse-cli-args ["--dry-run" "--verbose"])))

    (it "returns error for unknown arg"
      (let [result (runner/parse-cli-args ["--bogus"])]
        (should-contain :error result)
        (should (str/includes? (:error result) "--bogus")))))

  (describe "build-worker-task"

    (it "formats the task with all fields"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001" :channel "12345"}
            task (runner/build-worker-task spawn)]
        (should (str/includes? task "~/Projects/test"))
        (should (str/includes? task "test-abc"))
        (should (str/includes? task "001"))
        (should (str/includes? task "12345"))
        (should (str/includes? task "worker.md")))))

  (describe "build-worker-args"

    (it "includes required openclaw agent args"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001"
                   :channel "12345" :worker-timeout 1800}
            args (runner/build-worker-args spawn)]
        (should (some #{"agent"} args))
        (should (some #{"--message"} args))
        (should (some #{"--session-id"} args))
        (should (some #{"--thinking"} args))
        (should (some #{"--timeout"} args))
        (should (some #{"--deliver"} args))))

    (it "includes --agent when worker-agent is set"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001"
                   :channel "12345" :worker-timeout 1800 :worker-agent "scrapper"}
            args (runner/build-worker-args spawn)]
        (should (some #{"--agent"} args))
        ;; --agent should be followed by the agent id
        (let [agent-idx (.indexOf args "--agent")]
          (should= "scrapper" (nth args (inc agent-idx))))))

    (it "omits --agent when worker-agent is nil"
      (let [spawn {:path "~/Projects/test" :bead "test-abc" :iteration "001"
                   :channel "12345" :worker-timeout 1800}
            args (runner/build-worker-args spawn)]
        (should-not (some #{"--agent"} args))))

    (it "uses default thinking=low"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 1800}
            args (runner/build-worker-args spawn)
            thinking-idx (.indexOf args "--thinking")]
        (should= "low" (nth args (inc thinking-idx)))))

    (it "uses provided thinking level"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"
                   :worker-timeout 1800 :thinking "high"}
            args (runner/build-worker-args spawn)
            thinking-idx (.indexOf args "--thinking")]
        (should= "high" (nth args (inc thinking-idx)))))

    (it "uses default timeout=1800 when not provided"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"}
            args (runner/build-worker-args spawn)
            timeout-idx (.indexOf args "--timeout")]
        (should= "1800" (nth args (inc timeout-idx)))))

    (it "uses provided timeout"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c" :worker-timeout 3600}
            args (runner/build-worker-args spawn)
            timeout-idx (.indexOf args "--timeout")]
        (should= "3600" (nth args (inc timeout-idx)))))

    (it "generates unique session IDs"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"}
            args1 (runner/build-worker-args spawn)
            args2 (runner/build-worker-args spawn)
            session-idx1 (.indexOf args1 "--session-id")
            session-idx2 (.indexOf args2 "--session-id")]
        (should-not= (nth args1 (inc session-idx1))
                     (nth args2 (inc session-idx2))))))

  (describe "log-line"

    (it "includes timestamp in brackets"
      (let [line (runner/log-line "hello")]
        (should (str/starts-with? line "["))
        (should (str/includes? line "] hello"))))

    (it "includes the message"
      (let [line (runner/log-line "test message")]
        (should (str/ends-with? line "test message")))))

  (describe "format-spawn-log"

    (it "includes action=spawn"
      (let [result {:action "spawn" :spawns [{:bead "b1" :worker-agent "ag"}]}
            lines (runner/format-spawn-log result)]
        (should (some #(str/includes? % "action=spawn") lines))))

    (it "shows worker count"
      (let [result {:action "spawn"
                    :spawns [{:bead "b1"} {:bead "b2"}]}
            lines (runner/format-spawn-log result)]
        (should (some #(str/includes? % "2 worker") lines))))

    (it "includes bead IDs"
      (let [result {:action "spawn" :spawns [{:bead "my-bead-123"}]}
            lines (runner/format-spawn-log result)]
        (should (some #(str/includes? % "my-bead-123") lines)))))

  (describe "format-idle-log"

    (it "includes action=idle"
      (let [lines (runner/format-idle-log {:reason "no-ready-beads" :disable-cron false})]
        (should (some #(str/includes? % "action=idle") lines))))

    (it "includes reason"
      (let [lines (runner/format-idle-log {:reason "all-at-capacity" :disable-cron false})]
        (should (some #(str/includes? % "reason=all-at-capacity") lines))))

    (it "includes disable_cron flag"
      (let [lines (runner/format-idle-log {:reason "no-ready-beads" :disable-cron true})]
        (should (some #(str/includes? % "disable_cron=true") lines)))))

  (describe "format-zombie-log"

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
        (should (some #(str/includes? % "bead-closed") lines))))))
