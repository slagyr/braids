(ns simulation-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.string :as str]
            [cheshire.core :as json]
            [braids.orch :as orch]))

;; NOTE: All tests in this file are PENDING (integration/simulation tests).
;; They create temp directories, shell out to external tools, and read
;; project files — causing hangs or failures when tools are unavailable.
;; Move to a separate `bb test:integration` task when ready.
;; See: braids-kog
;;
;; Fixed top-level side effects:
;;   - Removed: (def contracts (slurp ...)) — slurp at load time
;;   - Removed: (def test-tmp (fs/create-temp-dir ...)) — creates dir at load time
;;   - Removed: (proc/shell "rm" "-rf" test-tmp) — rm at load time (was line 291)

(def home (System/getProperty "user.home"))
(def project-root (str (System/getProperty "user.dir")))

(defn- contracts [] (slurp (str project-root "/CONTRACTS.md")))

(defn setup-test-project! [test-tmp test-project]
  (proc/shell {:continue true} "rm" "-rf" test-project)
  (fs/create-dirs (str test-project "/.braids/iterations/001"))

  (spit (str test-project "/.braids/config.edn")
    (pr-str {:name "Test Simulation Project"
             :status :active
             :priority :high
             :autonomy :full
             :checkin :daily
             :channel "test-channel-123"
             :max-workers 2
             :worker-timeout 1800
             :notifications {:iteration-start true
                             :bead-start true
                             :bead-complete true
                             :iteration-complete true
                             :no-ready-beads true
                             :question true
                             :blocker true}}))

  (spit (str test-project "/AGENTS.md") "# Test Project AGENTS.md\nRead worker.md for instructions.\n\n## Goal\n\nTest project for simulation tests.\n\n## Guardrails\n\n- This is a test project\n")

  (spit (str test-project "/.braids/iterations/001/iteration.edn")
    (pr-str {:number 1
             :status :active
             :stories [{:id "test-sim-aaa" :title "First test bead"}
                       {:id "test-sim-bbb" :title "Second test bead (depends on aaa)"}
                       {:id "test-sim-ccc" :title "Third test bead (independent)"}]
             :notes []}))

  (spit (str test-tmp "/registry.edn")
    (pr-str {:projects [{:slug "test-sim-project" :status :active :priority :high :path test-project}]})))

;; ── Scenario 1: config.edn Defaults ──

(describe "Scenario 1: config.edn Field Defaults"
  (xit "config.edn is valid EDN")
  (xit "MaxWorkers missing (default 1 applies)")
  (xit "WorkerTimeout missing (default 1800 applies)")
  (xit "Channel missing (default: skip notifications)")
  (xit "Checkin missing (default: on-demand)")
  (xit "Notifications missing (default: all on)"))

;; ── Scenario 2: Iteration Lifecycle ──

(describe "Scenario 2: Iteration Lifecycle"
  (xit "iteration.edn is valid EDN")
  (xit "iteration.edn has stories")
  (xit "iteration status is active")
  (xit "at most one active iteration")
  (xit "completed iteration does not require RETRO.md")
  (xit "completed iteration status is complete"))

;; ── Scenario 3: Deliverable Naming ──

(describe "Scenario 3: Deliverable Naming"
  (xit "deliverable file created")
  (xit "deliverable has Summary section")
  (xit "deliverable name matches convention"))

;; ── Scenario 4: Orchestrator Self-Disable ──

(describe "Scenario 4: Orchestrator Self-Disable"
  (xit "tick returns disable-cron true when idle with no-active-iterations")
  (xit "tick returns disable-cron false when idle with no-ready-beads (active iterations exist)")
  (xit "tick does not include disable-cron when spawning")
  (xit "all idle reasons documented in CONTRACTS.md")
  (xit "self-disable documented in CONTRACTS.md"))

;; ── Scenario 5: Worker Context Loading ──

(describe "Scenario 5: Worker Context Loading"
  (xit "config.edn exists (context step 1)")
  (xit "Project AGENTS.md exists (context step 3)")
  (xit "iteration.edn exists (context step 4)")
  (xit "Workspace AGENTS.md exists (context step 2 - simulated)"))

;; ── Scenario 6: Spawn Message Format ──

(describe "Scenario 6: Spawn Message Format"
  (xit "spawn message has correct format"))

;; ── Scenario 7: Session Label Convention ──

(describe "Scenario 7: Session Label Convention"
  (xit "label matches format"))

;; ── Scenario 8: Bead Lifecycle ──

(describe "Scenario 8: Bead Lifecycle"
  (xit "contract documents open -> in_progress")
  (xit "contract documents closed state")
  (xit "contract documents blocked can be reopened"))

;; ── Scenario 9: Registry Validation ──

(describe "Scenario 9: Registry Validation"
  (xit "registry.edn is valid EDN")
  (xit "has valid status")
  (xit "has valid priority")
  (xit "rejects 'complete' as registry status"))

;; ── Scenario 10: Worker Error Handling ──

(describe "Scenario 10: Worker Error Handling"
  (xit "partial deliverable written")
  (xit "partial deliverable has Summary")
  (xit "partial deliverable documents remaining work"))

;; ── Scenario 11: RETRO.md Removal Verification ──

(describe "Scenario 11: RETRO.md feature removed"
  (xit "worker.md does not reference RETRO.md generation")
  (xit "worker.md does not reference .completing lock")
  (xit "CONTRACTS.md does not have RETRO.md section"))

;; ── Scenario 12: Orchestrator Invariants ──

(describe "Scenario 12: Orchestrator Invariants"
  (xit "orchestrator never performs bead work")
  (xit "orchestrator only reads state and spawns")
  (xit "concurrency enforcement documented")
  (xit "active iteration required for spawn"))

;; ── Scenario 13: Git Conventions ──

(describe "Scenario 13: Git Conventions"
  (xit "commit format matches convention")
  (xit "iteration commit format"))

;; ── Scenario 14: Path Conventions ──

(describe "Scenario 14: Path Conventions"
  (xit "~ resolves to user home")
  (xit "BRAIDS_HOME default ~/Projects")
  (xit "project files not in workspace"))

;; ── Scenario 15: Iteration Completion (simplified) ──

(describe "Scenario 15: Iteration Completion (simplified)"
  (xit "worker.md documents simple iteration completion")
  (xit "no .completing lock mechanism in worker.md")
  (xit "no .completing lock mechanism in CONTRACTS.md"))
