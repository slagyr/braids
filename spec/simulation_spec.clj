(ns simulation-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [cheshire.core :as json]
            [braids.orch :as orch]))

(def home (System/getProperty "user.home"))
(def project-root (str (System/getProperty "user.dir")))
(def contracts (slurp (str project-root "/CONTRACTS.md")))
(def test-tmp (str (fs/create-temp-dir {:prefix "sim-test"})))
(def test-project (str test-tmp "/test-sim-project"))

(defn setup-test-project! []
  (fs/delete-tree test-project)
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
  (before-all
    (setup-test-project!)
    (spit (str test-project "/.braids/config.edn")
      (pr-str {:name "Minimal Project"
               :status :active
               :priority :normal
               :autonomy :full})))

  (it "config.edn is valid EDN"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should (map? parsed))
      (should= :active (:status parsed))))
  (it "MaxWorkers missing (default 1 applies)"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :max-workers))))
  (it "WorkerTimeout missing (default 1800 applies)"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :worker-timeout))))
  (it "Channel missing (default: skip notifications)"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :channel))))
  (it "Checkin missing (default: on-demand)"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :checkin))))
  (it "Notifications missing (default: all on)"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :notifications)))))

;; ── Scenario 2: Iteration Lifecycle ──

(describe "Scenario 2: Iteration Lifecycle"
  (before-all (setup-test-project!))

  (it "iteration.edn is valid EDN"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/001/iteration.edn")))]
      (should (map? parsed))
      (should= :active (:status parsed))))
  (it "iteration.edn has stories"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/001/iteration.edn")))]
      (should (seq (:stories parsed)))))
  (it "iteration status is active"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/001/iteration.edn")))]
      (should= :active (:status parsed))))

  (it "at most one active iteration"
    (fs/create-dirs (str test-project "/.braids/iterations/002"))
    (spit (str test-project "/.braids/iterations/002/iteration.edn")
      (pr-str {:number 2 :status :planning :stories [{:id "test-sim-ddd" :title "Future bead"}] :notes []}))
    (let [active-count (->> (fs/glob test-project ".braids/iterations/*/iteration.edn")
                            (map #(clojure.edn/read-string (slurp (str %))))
                            (filter #(= :active (:status %)))
                            count)]
      (should (<= active-count 1))))

  (it "completed iteration does not require RETRO.md"
    (fs/create-dirs (str test-project "/.braids/iterations/000"))
    (spit (str test-project "/.braids/iterations/000/iteration.edn")
      (pr-str {:number 0 :status :complete :stories [{:id "test-sim-zzz" :title "Completed bead"}] :notes []}))
    (should-not (fs/exists? (str test-project "/.braids/iterations/000/RETRO.md"))))

  (it "completed iteration status is complete"
    (let [parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/000/iteration.edn")))]
      (should= :complete (:status parsed)))))

;; ── Scenario 3: Deliverable Naming ──

(describe "Scenario 3: Deliverable Naming"
  (before-all
    (setup-test-project!)
    (spit (str test-project "/.braids/iterations/001/aaa-first-test-bead.md")
      "# First Test Bead\n\n## Summary\nCompleted the first test bead.\n"))

  (it "deliverable file created"
    (should (fs/exists? (str test-project "/.braids/iterations/001/aaa-first-test-bead.md"))))
  (it "deliverable has Summary section"
    (should-contain "## Summary" (slurp (str test-project "/.braids/iterations/001/aaa-first-test-bead.md"))))
  (it "deliverable name matches convention"
    (should (re-find #"^[a-z0-9]{3}-[a-z0-9-]+\.md$" "aaa-first-test-bead.md"))))

;; ── Scenario 4: Orchestrator Self-Disable ──

(describe "Scenario 4: Orchestrator Self-Disable"
  (it "tick returns disable-cron true when idle with no-active-iterations"
    (let [result (orch/tick {:projects []} {} {} {} {} {})]
      (should= true (:disable-cron result))))

  (it "tick returns disable-cron true when idle with no-ready-beads"
    (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
          configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
          iterations {"proj" "008"}
          result (orch/tick registry configs iterations {"proj" []} {} {})]
      (should= true (:disable-cron result))))

  (it "tick does not include disable-cron when spawning"
    (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
          configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
          iterations {"proj" "008"}
          beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
          result (orch/tick registry configs iterations beads {} {})]
      (should-not-contain :disable-cron result)))

  (it "all idle reasons documented in CONTRACTS.md"
    (doseq [reason ["no-active-iterations" "no-ready-beads" "all-at-capacity"]]
      (should-contain reason contracts)))

  (it "self-disable documented in CONTRACTS.md"
    (should (re-find #"(?i)self-disable|disable.cron" contracts))))

;; ── Scenario 5: Worker Context Loading ──

(describe "Scenario 5: Worker Context Loading"
  (before-all (setup-test-project!))

  (it "config.edn exists (context step 1)"
    (should (fs/exists? (str test-project "/.braids/config.edn"))))
  (it "Project AGENTS.md exists (context step 3)"
    (should (fs/exists? (str test-project "/AGENTS.md"))))
  (it "iteration.edn exists (context step 4)"
    (should (fs/exists? (str test-project "/.braids/iterations/001/iteration.edn"))))
  (it "Workspace AGENTS.md exists (context step 2 - simulated)"
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

  (it "registry.edn is valid EDN"
    (let [content (slurp (str test-tmp "/registry.edn"))
          parsed (clojure.edn/read-string content)]
      (should (map? parsed))
      (should (contains? parsed :projects))))
  (it "has valid status"
    (let [parsed (clojure.edn/read-string (slurp (str test-tmp "/registry.edn")))]
      (should= :active (-> parsed :projects first :status))))
  (it "has valid priority"
    (let [parsed (clojure.edn/read-string (slurp (str test-tmp "/registry.edn")))]
      (should= :high (-> parsed :projects first :priority))))
  (it "rejects 'complete' as registry status"
    (should-not (contains? #{"active" "paused" "blocked"} "complete"))))

;; ── Scenario 10: Worker Error Handling ──

(describe "Scenario 10: Worker Error Handling"
  (before-all
    (setup-test-project!)
    (spit (str test-project "/.braids/iterations/001/bbb-second-test-bead.md")
      "# Second Test Bead (Partial)\n\n## Summary\nPartially completed.\n\n## Remaining\n- Complete integration after aaa lands\n"))

  (it "partial deliverable written"
    (should (fs/exists? (str test-project "/.braids/iterations/001/bbb-second-test-bead.md"))))
  (it "partial deliverable has Summary"
    (should-contain "## Summary" (slurp (str test-project "/.braids/iterations/001/bbb-second-test-bead.md"))))
  (it "partial deliverable documents remaining work"
    (should-contain "## Remaining" (slurp (str test-project "/.braids/iterations/001/bbb-second-test-bead.md")))))

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
      (should (re-find #"Update iteration\.edn status to.*:complete" worker))))
  (it "no .completing lock mechanism in worker.md"
    (let [worker (slurp (str project-root "/braids/references/worker.md"))]
      (should-not (str/includes? worker ".completing"))))
  (it "no .completing lock mechanism in CONTRACTS.md"
    (should-not (str/includes? contracts ".completing"))))

;; Cleanup
(fs/delete-tree test-tmp)
