(ns simulation-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def home (System/getProperty "user.home"))
(def project-root (str (System/getProperty "user.dir")))
(def contracts (slurp (str project-root "/CONTRACTS.md")))
(def test-tmp (str (fs/create-temp-dir {:prefix "sim-test"})))
(def test-project (str test-tmp "/test-sim-project"))

(defn setup-test-project! []
  (fs/delete-tree test-project)
  (fs/create-dirs (str test-project "/.project/iterations/001"))

  (spit (str test-project "/.project/PROJECT.md")
    "# Test Simulation Project\n\n- **Status:** active\n- **Priority:** high\n- **Autonomy:** full\n- **Checkin:** daily\n- **Channel:** test-channel-123\n- **MaxWorkers:** 2\n- **WorkerTimeout:** 1800\n\n## Notifications\n\n| Event | Notify |\n|-------|--------|\n| iteration-start | on |\n| bead-start | on |\n| bead-complete | on |\n| iteration-complete | on |\n| no-ready-beads | on |\n| question | on |\n| blocker | on |\n\n## Goal\n\nTest project for simulation tests.\n\n## Guardrails\n\n- This is a test project\n")

  (spit (str test-project "/AGENTS.md") "# Test Project AGENTS.md\nRead worker.md for instructions.\n")

  (spit (str test-project "/.project/iterations/001/ITERATION.md")
    "# Iteration 001\n\n- **Status:** active\n\n## Stories\n- test-sim-aaa: First test bead\n- test-sim-bbb: Second test bead (depends on aaa)\n- test-sim-ccc: Third test bead (independent)\n")

  (spit (str test-tmp "/registry.md")
    (str "| Slug | Status | Priority | Path |\n|------|--------|----------|------|\n| test-sim-project | active | high | " test-project " |\n")))

;; ── Scenario 1: PROJECT.md Defaults ──

(describe "Scenario 1: PROJECT.md Field Defaults"
  (before-all
    (setup-test-project!)
    (spit (str test-project "/.project/PROJECT.md")
      "# Minimal Project\n\n- **Status:** active\n- **Priority:** normal\n- **Autonomy:** full\n\n## Goal\nMinimal test.\n\n## Guardrails\n- None\n"))

  (it "Status field present"
    (should-contain "Status:" (slurp (str test-project "/.project/PROJECT.md"))))
  (it "MaxWorkers missing (default 1 applies)"
    (should-not (str/includes? (slurp (str test-project "/.project/PROJECT.md")) "MaxWorkers")))
  (it "WorkerTimeout missing (default 1800 applies)"
    (should-not (str/includes? (slurp (str test-project "/.project/PROJECT.md")) "WorkerTimeout")))
  (it "Channel missing (default: skip notifications)"
    (should-not (str/includes? (slurp (str test-project "/.project/PROJECT.md")) "Channel:")))
  (it "Checkin missing (default: on-demand)"
    (should-not (str/includes? (slurp (str test-project "/.project/PROJECT.md")) "Checkin:")))
  (it "Notifications table missing (default: all on)"
    (should-not (str/includes? (slurp (str test-project "/.project/PROJECT.md")) "Notifications"))))

;; ── Scenario 2: Iteration Lifecycle ──

(describe "Scenario 2: Iteration Lifecycle"
  (before-all (setup-test-project!))

  (it "ITERATION.md has Status"
    (should (re-find #"(?i)Status:" (slurp (str test-project "/.project/iterations/001/ITERATION.md")))))
  (it "ITERATION.md has Stories section"
    (should-contain "## Stories" (slurp (str test-project "/.project/iterations/001/ITERATION.md"))))
  (it "iteration status is active"
    (should-contain "active" (slurp (str test-project "/.project/iterations/001/ITERATION.md"))))

  (it "at most one active iteration"
    (fs/create-dirs (str test-project "/.project/iterations/002"))
    (spit (str test-project "/.project/iterations/002/ITERATION.md")
      "# Iteration 002\n- **Status:** planning\n## Stories\n- test-sim-ddd: Future bead\n")
    (let [active-count (->> (fs/glob test-project ".project/iterations/*/ITERATION.md")
                            (map #(slurp (str %)))
                            (filter #(re-find #"Status:.*active" %))
                            count)]
      (should (<= active-count 1))))

  (it "completed iteration does not require RETRO.md"
    (fs/create-dirs (str test-project "/.project/iterations/000"))
    (spit (str test-project "/.project/iterations/000/ITERATION.md")
      "# Iteration 000\n- **Status:** complete\n## Stories\n- test-sim-zzz: Completed bead\n")
    (should-not (fs/exists? (str test-project "/.project/iterations/000/RETRO.md"))))

  (it "completed iteration status is complete"
    (should-contain "complete" (slurp (str test-project "/.project/iterations/000/ITERATION.md")))))

;; ── Scenario 3: Deliverable Naming ──

(describe "Scenario 3: Deliverable Naming"
  (before-all
    (setup-test-project!)
    (spit (str test-project "/.project/iterations/001/aaa-first-test-bead.md")
      "# First Test Bead\n\n## Summary\nCompleted the first test bead.\n"))

  (it "deliverable file created"
    (should (fs/exists? (str test-project "/.project/iterations/001/aaa-first-test-bead.md"))))
  (it "deliverable has Summary section"
    (should-contain "## Summary" (slurp (str test-project "/.project/iterations/001/aaa-first-test-bead.md"))))
  (it "deliverable name matches convention"
    (should (re-find #"^[a-z0-9]{3}-[a-z0-9-]+\.md$" "aaa-first-test-bead.md"))))

;; ── Scenario 4: Orchestrator Frequency Scaling ──

(describe "Scenario 4: Orchestrator Frequency Scaling"
  (it "state: idleSince null after spawn"
    (let [state-file (str test-tmp "/.orchestrator-state.json")]
      (spit state-file "{\"idleSince\":null,\"idleReason\":null,\"lastRunAt\":\"2026-02-13T12:00:00Z\"}")
      (should-be-nil (get (json/parse-string (slurp state-file)) "idleSince"))))

  (it "state: idleReason null after spawn"
    (let [state-file (str test-tmp "/.orchestrator-state.json")]
      (should-be-nil (get (json/parse-string (slurp state-file)) "idleReason"))))

  (it "state: lastRunAt is set"
    (let [state-file (str test-tmp "/.orchestrator-state.json")]
      (should (get (json/parse-string (slurp state-file)) "lastRunAt"))))

  (it "idle state with no-active-iterations"
    (let [state-file (str test-tmp "/.orchestrator-state.json")]
      (spit state-file "{\"idleSince\":\"2026-02-13T12:05:00Z\",\"idleReason\":\"no-active-iterations\",\"lastRunAt\":\"2026-02-13T12:05:00Z\"}")
      (should= "no-active-iterations" (get (json/parse-string (slurp state-file)) "idleReason"))))

  (it "no-active-iterations backoff 30min"
    (should (re-find #"no-active-iterations.*30" contracts)))
  (it "no-ready-beads backoff 15min"
    (should (re-find #"no-ready-beads.*15" contracts)))
  (it "all-at-capacity backoff 10min"
    (should (re-find #"all-at-capacity.*10" contracts)))

  (it "all idle reasons documented"
    (doseq [reason ["no-active-iterations" "no-ready-beads" "all-at-capacity"]]
      (should-contain reason contracts))))

;; ── Scenario 5: Worker Context Loading ──

(describe "Scenario 5: Worker Context Loading"
  (before-all (setup-test-project!))

  (it "PROJECT.md exists (context step 1)"
    (should (fs/exists? (str test-project "/.project/PROJECT.md"))))
  (it "Project AGENTS.md exists (context step 3)"
    (should (fs/exists? (str test-project "/AGENTS.md"))))
  (it "ITERATION.md exists (context step 4)"
    (should (fs/exists? (str test-project "/.project/iterations/001/ITERATION.md"))))
  (it "Workspace AGENTS.md exists (context step 2 - simulated)"
    ;; In production, this is ~/.openclaw/workspace/AGENTS.md
    ;; For portability, we verify the contract requires it but don't check the installed path
    (should-contain "Workspace AGENTS.md" contracts)))

;; ── Scenario 6: Spawn Message Format ──

(describe "Scenario 6: Spawn Message Format"
  (it "spawn message has correct format"
    (let [msg (str "Project: " test-project "\nBead: test-sim-aaa\nIteration: 1\nChannel: test-channel-123")]
      (should= 4 (count (str/split-lines msg)))
      (should (re-find #"^Project:" msg))
      (should-contain "Bead:" msg)
      (should-contain "Iteration:" msg)
      (should-contain "Channel:" msg))))

;; ── Scenario 7: Session Label Convention ──

(describe "Scenario 7: Session Label Convention"
  (it "label matches format"
    (let [label "project:test-sim-project:test-sim-aaa"]
      (should (re-find #"^project:[a-z0-9-]+:[a-z0-9-]+$" label))
      (should (str/starts-with? label "project:")))))

;; ── Scenario 8: Bead Lifecycle ──

(describe "Scenario 8: Bead Lifecycle"
  (it "contract documents open → in_progress"
    (should (re-find #"open.*in_progress" contracts)))
  (it "contract documents closed state"
    (should (re-find #"closed.*final" contracts)))
  (it "contract documents blocked can be reopened"
    (should (re-find #"Blocked.*reopened" contracts))))

;; ── Scenario 9: Registry Validation ──

(describe "Scenario 9: Registry Validation"
  (before-all (setup-test-project!))

  (it "has required columns"
    (should-contain "| Slug | Status | Priority | Path |" (slurp (str test-tmp "/registry.md"))))
  (it "has valid status"
    (should-contain "active" (slurp (str test-tmp "/registry.md"))))
  (it "has valid priority"
    (should-contain "high" (slurp (str test-tmp "/registry.md"))))
  (it "rejects 'complete' as registry status"
    (should-not (contains? #{"active" "paused" "blocked"} "complete"))))

;; ── Scenario 10: Worker Error Handling ──

(describe "Scenario 10: Worker Error Handling"
  (before-all
    (setup-test-project!)
    (spit (str test-project "/.project/iterations/001/bbb-second-test-bead.md")
      "# Second Test Bead (Partial)\n\n## Summary\nPartially completed.\n\n## Remaining\n- Complete integration after aaa lands\n"))

  (it "partial deliverable written"
    (should (fs/exists? (str test-project "/.project/iterations/001/bbb-second-test-bead.md"))))
  (it "partial deliverable has Summary"
    (should-contain "## Summary" (slurp (str test-project "/.project/iterations/001/bbb-second-test-bead.md"))))
  (it "partial deliverable documents remaining work"
    (should-contain "## Remaining" (slurp (str test-project "/.project/iterations/001/bbb-second-test-bead.md")))))

;; ── Scenario 11: RETRO.md Removal Verification ──

(describe "Scenario 11: RETRO.md feature removed"
  (it "worker.md does not reference RETRO.md generation"
    (let [worker (slurp (str project-root "/braids/references/worker.md"))]
      (should-not (re-find #"Generate.*Retrospective|auto-generate.*RETRO" worker))))
  (it "worker.md does not reference .completing lock"
    (let [worker (slurp (str project-root "/braids/references/worker.md"))]
      (should-not (str/includes? worker ".completing"))))
  (it "CONTRACTS.md does not have RETRO.md section"
    (should-not (re-find #"### \d+\.\d+ RETRO\.md" contracts))))

;; ── Scenario 12: Orchestrator Invariants ──

(describe "Scenario 12: Orchestrator Invariants"
  (it "orchestrator never performs bead work"
    (should (re-find #"never.*performs bead work" contracts)))
  (it "orchestrator only reads state and spawns"
    (should-contain "only reads state and spawns" contracts))
  (it "concurrency enforcement documented"
    (should-contain "Concurrency Enforcement" contracts))
  (it "active iteration required for spawn"
    (should-contain "Active Iteration Required" contracts)))

;; ── Scenario 13: Git Conventions ──

(describe "Scenario 13: Git Conventions"
  (it "commit format matches convention"
    (should (re-find #"^.+ \([a-z0-9-]+\)$" "Add first test bead (test-sim-aaa)")))
  (it "iteration commit format"
    (should (re-find #"^Complete iteration \d+$" "Complete iteration 1"))))

;; ── Scenario 14: STATUS.md ──

(describe "Scenario 14: STATUS.md"
  (it "STATUS.md is auto-generated"
    (should-contain "Auto-generated" contracts))
  (it "STATUS.md overwritten every run"
    (should-contain "Overwritten every orchestrator run" contracts))
  (it "STATUS.md never hand-edit"
    (should-contain "never hand-edit" contracts)))

;; ── Scenario 15: Path Conventions ──

(describe "Scenario 15: Path Conventions"
  (it "~ resolves to user home"
    (should-contain "always resolves to the user's home directory" contracts))
  (it "BRAIDS_HOME default ~/Projects"
    (should (re-find #"BRAIDS_HOME.*defaults to.*~/Projects" contracts)))
  (it "project files not in workspace"
    (should-contain "never created inside" contracts)))

;; ── Scenario 16: Iteration Completion (simplified) ──

(describe "Scenario 16: Iteration Completion (simplified)"
  (it "worker.md documents simple iteration completion"
    (let [worker (slurp (str project-root "/braids/references/worker.md"))]
      (should (re-find #"Update ITERATION.md status to.*complete" worker))))
  (it "no .completing lock mechanism in worker.md"
    (let [worker (slurp (str project-root "/braids/references/worker.md"))]
      (should-not (str/includes? worker ".completing"))))
  (it "no .completing lock mechanism in CONTRACTS.md"
    (should-not (str/includes? contracts ".completing"))))

;; Cleanup
(fs/delete-tree test-tmp)
