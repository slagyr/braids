(ns braids.orch-shell-spec
  "Tests for the braids orch CLI command.
   Tests both pure functions and subprocess integration."
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [braids.orch-runner :as runner]
            [braids.orch-runner-io :as runner-io]))

;; ── Pure function tests (orch-runner) ──

(describe "braids.orch-runner (pure functions)"

  (context "parse-cli-args"

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

  (context "build-worker-task"

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

  (context "build-worker-args"

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

    (it "generates deterministic session ID based on bead-id"
      (let [spawn {:path "~/p" :bead "b" :iteration "1" :channel "c"}
            args1 (runner/build-worker-args {} spawn)
            args2 (runner/build-worker-args {} spawn)
            sid1 (nth args1 (inc (.indexOf args1 "--session-id")))
            sid2 (nth args2 (inc (.indexOf args2 "--session-id")))]
        (should= sid1 sid2)
        (should= "braids-b-worker" sid1))))

  (context "log-line"

    (it "returns message without timestamp"
      (should= "test" (runner/log-line "test")))

    (it "includes the message"
      (should= "hello world" (runner/log-line "hello world"))))

  (context "format-spawn-log"

    (it "shows worker count"
      (let [lines (runner/format-spawn-log {:action "spawn" :spawns [{:bead "b1"} {:bead "b2"}]})]
        (should (some #(str/includes? % "2 worker") lines))))

    (it "includes bead IDs"
      (let [lines (runner/format-spawn-log {:action "spawn" :spawns [{:bead "my-bead-abc"}]})]
        (should (some #(str/includes? % "my-bead-abc") lines)))))

  (context "format-idle-log"

    (it "includes reason"
      (let [lines (runner/format-idle-log {:reason "all-at-capacity"})]
        (should (some #(str/includes? % "all-at-capacity") lines))))

    (it "includes idle"
      (let [lines (runner/format-idle-log {:reason "no-ready-beads"})]
        (should (some #(str/includes? % "Idle") lines)))))

  (context "format-zombie-log"

    (it "returns nil for empty zombies"
      (should-be-nil (runner/format-zombie-log [])))

    (it "includes zombie count and details"
      (let [lines (runner/format-zombie-log [{:bead "z1" :reason "bead-closed"}
                                             {:bead "z2" :reason "timeout"}])]
        (should (some #(str/includes? % "2 zombie") lines))
        (should (some #(str/includes? % "z1") lines))
        (should (some #(str/includes? % "bead-closed") lines))))))

;; ── CLI integration tests (pure, no subprocess) ──
;; These test the CLI flow via run-orch-command! without shelling out.
;; The actual subprocess spawning is not tested here (see bb test:integration).

(describe "braids orch (CLI flow)"

  (it "rejects unknown arguments via run-orch-command!"
    (let [output (with-out-str
                   (let [exit (runner-io/run-orch-command! ["--bogus"])]
                     (should= 1 exit)))]
      (should (str/includes? output "--bogus"))))

  (it "defaults to dry-run mode (parse-cli-args empty args)"
    (let [opts (runner/parse-cli-args [])]
      (should= true (:dry-run opts))
      (should= false (:verbose opts))))

  (it "--confirmed disables dry-run"
    (let [opts (runner/parse-cli-args ["--confirmed"])]
      (should= false (:dry-run opts)))))
