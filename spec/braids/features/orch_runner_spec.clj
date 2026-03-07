(ns braids.features.orch-runner-spec
  (:require [speclj.core :refer :all]))

(describe "Orchestrator runner"

  (context "Build worker task message from template"
    (it "Build worker task message from template"
      ;; Given a spawn entry with path "~/Projects/test" and bead "test-abc"
      ;; And iteration "001" and channel "12345"
      ;; When building the worker task
      ;; Then the task should contain "~/Projects/test"
      ;; And the task should contain "test-abc"
      ;; And the task should contain "001"
      ;; And the task should contain "worker.md"
      (pending "not yet implemented")))

  (context "Build worker CLI args with session ID"
    (it "Build worker CLI args with session ID"
      ;; Given a spawn entry with bead "proj-abc"
      ;; And no custom worker agent
      ;; When building the worker args
      ;; Then the args should include "--message"
      ;; And the args should include "--session-id"
      ;; And the session ID should be "braids-proj-abc-worker"
      ;; And the args should include "--thinking"
      ;; And the args should include "--timeout"
      ;; And the args should not include "--agent"
      (pending "not yet implemented")))

  (context "Build args with custom agent"
    (it "Build args with custom agent"
      ;; Given a spawn entry with bead "proj-abc"
      ;; And worker agent "scrapper"
      ;; When building the worker args
      ;; Then the args should include "--agent"
      ;; And the agent value should be "scrapper"
      (pending "not yet implemented")))

  (context "Parse CLI args defaults to dry-run"
    (it "Parse CLI args defaults to dry-run"
      ;; Given no CLI arguments
      ;; When parsing CLI args
      ;; Then dry-run should be true
      ;; And verbose should be false
      (pending "not yet implemented")))

  (context "Parse --confirmed enables run"
    (it "Parse --confirmed enables run"
      ;; Given CLI arguments "--confirmed"
      ;; When parsing CLI args
      ;; Then dry-run should be false
      (pending "not yet implemented")))

  (context "Parse unknown arg returns error"
    (it "Parse unknown arg returns error"
      ;; Given CLI arguments "--bogus"
      ;; When parsing CLI args
      ;; Then parsing should return an error
      ;; And the error should contain "--bogus"
      (pending "not yet implemented")))

  (context "Format spawn log"
    (it "Format spawn log"
      ;; Given a spawn tick result with 2 workers
      ;; And beads "b1" and "b2"
      ;; When formatting the spawn log
      ;; Then the log should contain "2 worker"
      ;; And the log should contain "b1"
      ;; And the log should contain "b2"
      (pending "not yet implemented")))

  (context "Format idle log"
    (it "Format idle log"
      ;; Given an idle tick result with reason "all-at-capacity"
      ;; When formatting the idle log
      ;; Then the log should contain "Idle"
      ;; And the log should contain "all-at-capacity"
      (pending "not yet implemented")))

  (context "Format zombie log"
    (it "Format zombie log"
      ;; Given 2 zombie sessions with reasons "bead-closed" and "timeout"
      ;; When formatting the zombie log
      ;; Then the log should contain "2 zombie"
      ;; And the log should contain "bead-closed"
      ;; And the log should contain "timeout"
      (pending "not yet implemented"))))
