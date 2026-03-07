(ns braids.features.orch-runner-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Orchestrator runner"

  (context "Build worker task message from template"
    (it "Build worker task message from template"
      (h/reset!)
      (h/set-spawn-entry {:path "~/Projects/test" :bead "test-abc"})
      (h/update-spawn-entry {:iteration "001" :channel "12345"})
      (h/build-worker-task!)
      (should (clojure.string/includes? (h/worker-task) "~/Projects/test"))
      (should (clojure.string/includes? (h/worker-task) "test-abc"))
      (should (clojure.string/includes? (h/worker-task) "001"))
      (should (clojure.string/includes? (h/worker-task) "worker.md"))))

  (context "Build worker CLI args with session ID"
    (it "Build worker CLI args with session ID"
      (h/reset!)
      (h/set-spawn-entry {:bead "proj-abc"})
      (h/build-worker-args!)
      (should (some #(= "--message" %) (h/worker-args)))
      (should (some #(= "--session-id" %) (h/worker-args)))
      (should= "braids-proj-abc-worker" (h/session-id-result))
      (should (some #(= "--thinking" %) (h/worker-args)))
      (should (some #(= "--timeout" %) (h/worker-args)))
      (should-not (some #(= "--agent" %) (h/worker-args)))))

  (context "Build args with custom agent"
    (it "Build args with custom agent"
      (h/reset!)
      (h/set-spawn-entry {:bead "proj-abc"})
      (h/set-worker-agent "scrapper")
      (h/build-worker-args!)
      (should (some #(= "--agent" %) (h/worker-args)))
      (let [args (h/worker-args)
            idx (.indexOf args "--agent")]
        (should (>= idx 0))
        (should= "scrapper" (nth args (inc idx))))))

  (context "Parse CLI args defaults to dry-run"
    (it "Parse CLI args defaults to dry-run"
      (h/reset!)
      (h/set-cli-args [])
      (h/parse-cli-args!)
      (should= true (:dry-run (h/parsed-cli-args)))
      (should= false (:verbose (h/parsed-cli-args)))))

  (context "Parse --confirmed enables run"
    (it "Parse --confirmed enables run"
      (h/reset!)
      (h/set-cli-args ["--confirmed"])
      (h/parse-cli-args!)
      (should= false (:dry-run (h/parsed-cli-args)))))

  (context "Parse unknown arg returns error"
    (it "Parse unknown arg returns error"
      (h/reset!)
      (h/set-cli-args ["--bogus"])
      (h/parse-cli-args!)
      (should (:error (h/parsed-cli-args)))
      (should (clojure.string/includes? (:error (h/parsed-cli-args)) "--bogus"))))

  (context "Format spawn log"
    (it "Format spawn log"
      (h/reset!)
      (h/set-spawn-tick-result 2 [])
      (h/add-spawn-beads ["b1" "b2"])
      (h/format-spawn-log!)
      (should (some #(clojure.string/includes? % "2 worker") (h/runner-log)))
      (should (some #(clojure.string/includes? % "b1") (h/runner-log)))
      (should (some #(clojure.string/includes? % "b2") (h/runner-log)))))

  (context "Format idle log"
    (it "Format idle log"
      (h/reset!)
      (h/set-idle-tick-result "all-at-capacity")
      (h/format-idle-log!)
      (should (some #(clojure.string/includes? % "Idle") (h/runner-log)))
      (should (some #(clojure.string/includes? % "all-at-capacity") (h/runner-log)))))

  (context "Format zombie log"
    (it "Format zombie log"
      (h/reset!)
      (h/set-zombie-sessions 2 ["bead-closed" "timeout"])
      (h/format-zombie-log!)
      (should (some #(clojure.string/includes? % "2 zombie") (h/runner-log)))
      (should (some #(clojure.string/includes? % "bead-closed") (h/runner-log)))
      (should (some #(clojure.string/includes? % "timeout") (h/runner-log))))))
