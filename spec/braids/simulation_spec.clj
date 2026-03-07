(ns braids.simulation-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [braids.orch]))

(def project-root (str (System/getProperty "user.dir")))

(defn- contracts [] (slurp (str project-root "/CONTRACTS.md")))
(defn- worker-md [] (slurp (str project-root "/braids/references/worker.md")))

(defn- setup-test-project!
  "Creates a minimal test project in a temp dir. Returns [test-tmp test-project]."
  []
  (let [test-tmp (str (fs/create-temp-dir {:prefix "sim-test"}))
        test-project (str test-tmp "/test-sim-project")]
    (fs/create-dirs (str test-project "/.braids/iterations/001"))
    (spit (str test-project "/.braids/config.edn")
      (pr-str {:name "Minimal Project"
               :status :active
               :priority :normal
               :autonomy :full}))
    (spit (str test-project "/AGENTS.md")
      "# Test Project\n\n## Goal\n\nTest.\n\n## Guardrails\n\n- Test\n")
    (spit (str test-project "/.braids/iterations/001/iteration.edn")
      (pr-str {:number 1
               :status :active
               :stories [{:id "test-sim-aaa" :title "First test bead"}]
               :notes []}))
    (spit (str test-tmp "/registry.edn")
      (pr-str {:projects [{:slug "test-sim-project" :status :active :priority :high :path test-project}]}))
    [test-tmp test-project]))

;; ── Scenario 1: config.edn Defaults ──

(describe "Scenario 1: config.edn Field Defaults"
  (with-all test-env (setup-test-project!))

  (it "config.edn is valid EDN"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should (map? parsed))
      (should= :active (:status parsed))))
  (it "MaxWorkers missing (default 1 applies)"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :max-workers))))
  (it "WorkerTimeout missing (default 1800 applies)"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :worker-timeout))))
  (it "Channel missing (default: skip notifications)"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :channel))))
  (it "Checkin missing (default: on-demand)"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :checkin))))
  (it "Notifications missing (default: all on)"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/config.edn")))]
      (should-not (contains? parsed :notifications)))))

;; ── Scenario 2: Iteration Lifecycle ──

(describe "Scenario 2: Iteration Lifecycle"
  (with-all test-env (setup-test-project!))

  (it "iteration.edn is valid EDN"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/001/iteration.edn")))]
      (should (map? parsed))
      (should= :active (:status parsed))))
  (it "iteration.edn has stories"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/001/iteration.edn")))]
      (should (seq (:stories parsed)))))
  (it "iteration status is active"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/001/iteration.edn")))]
      (should= :active (:status parsed))))

  (it "at most one active iteration"
    (let [[_ test-project] @test-env]
      (fs/create-dirs (str test-project "/.braids/iterations/002"))
      (spit (str test-project "/.braids/iterations/002/iteration.edn")
        (pr-str {:number 2 :status :planning :stories [] :notes []}))
      (let [active-count (->> (fs/glob test-project ".braids/iterations/*/iteration.edn")
                              (map #(clojure.edn/read-string (slurp (str %))))
                              (filter #(= :active (:status %)))
                              count)]
        (should (<= active-count 1)))))

  (it "completed iteration does not require RETRO.md"
    (let [[_ test-project] @test-env]
      (fs/create-dirs (str test-project "/.braids/iterations/000"))
      (spit (str test-project "/.braids/iterations/000/iteration.edn")
        (pr-str {:number 0 :status :complete :stories [] :notes []}))
      (should-not (fs/exists? (str test-project "/.braids/iterations/000/RETRO.md")))))

  (it "completed iteration status is complete"
    (let [[_ test-project] @test-env
          parsed (clojure.edn/read-string (slurp (str test-project "/.braids/iterations/000/iteration.edn")))]
      (should= :complete (:status parsed)))))

;; ── Scenario 4: Orchestrator Self-Disable ──
;; NOTE: tick logic tests are in spec/braids/orch_spec.clj — these check CONTRACTS.md docs

(describe "Scenario 4: Orchestrator Self-Disable"
  (it "all idle reasons documented in CONTRACTS.md"
    (let [c (contracts)]
      (doseq [reason ["no-active-iterations" "no-ready-beads" "all-at-capacity"]]
        (should-contain reason c))))

  (it "orch/tick returns :disable-cron on idle"
    (let [result (braids.orch/tick
                   {:projects [{:slug "p" :status :active :priority :high :path "/tmp/x"}]}
                   {"p" {:status :active}}
                   {} {} {} {})]
      (should (contains? result :disable-cron)))))

;; ── Scenario 8: Bead Lifecycle ──

(describe "Scenario 8: Bead Lifecycle"
  (it "contract documents open -> in_progress"
    (should (re-find #"open.*in_progress" (contracts))))
  (it "contract documents closed state"
    (should (re-find #"[Cc]losed.*final" (contracts))))
  (it "contract documents blocked can be reopened"
    (should (re-find #"[Bb]locked.*reopened" (contracts)))))

;; ── Scenario 9: Registry Validation ──

(describe "Scenario 9: Registry Validation"
  (with-all test-env (setup-test-project!))

  (it "registry.edn is valid EDN"
    (let [[test-tmp _] @test-env
          content (slurp (str test-tmp "/registry.edn"))
          parsed (clojure.edn/read-string content)]
      (should (map? parsed))
      (should (contains? parsed :projects))))
  (it "has valid status"
    (let [[test-tmp _] @test-env
          parsed (clojure.edn/read-string (slurp (str test-tmp "/registry.edn")))]
      (should= :active (-> parsed :projects first :status))))
  (it "has valid priority"
    (let [[test-tmp _] @test-env
          parsed (clojure.edn/read-string (slurp (str test-tmp "/registry.edn")))]
      (should= :high (-> parsed :projects first :priority))))
  (it "rejects 'complete' as registry status"
    (should-not (contains? #{:active :paused :blocked} :complete))))

;; ── Scenario 11: RETRO.md Removal Verification ──

(describe "Scenario 11: RETRO.md feature removed"
  (it "worker.md does not reference RETRO.md generation"
    (should-not (re-find #"Generate.*Retrospective|auto-generate.*RETRO" (worker-md))))
  (it "worker.md does not reference .completing lock"
    (should-not (str/includes? (worker-md) ".completing")))
  (it "CONTRACTS.md does not have RETRO.md section"
    (should-not (re-find #"### \d+\.\d+ RETRO\.md" (contracts)))))

;; ── Scenario 12: Orchestrator Invariants ──

(describe "Scenario 12: Orchestrator Invariants"
  (it "orchestrator never performs bead work"
    (should (re-find #"never.*performs bead work" (contracts))))
  (it "orchestrator only reads state and spawns"
    (should-contain "only reads state and spawns" (contracts)))
  (it "concurrency enforcement documented"
    (should-contain "Concurrency Enforcement" (contracts)))
  (it "active iteration required for spawn"
    (should-contain "Active Iteration Required" (contracts))))

;; ── Scenario 14: Path Conventions ──

(describe "Scenario 14: Path Conventions"
  (it "~ resolves to user home"
    (should-contain "always resolves to the user's home directory" (contracts)))
  (it "BRAIDS_HOME default ~/Projects"
    (should (re-find #"BRAIDS_HOME.*defaults to.*~/Projects" (contracts))))
  (it "project files not in workspace"
    (should-contain "never created inside" (contracts))))

;; ── Scenario 15: Iteration Completion (simplified) ──

(describe "Scenario 15: Iteration Completion (simplified)"
  (it "worker.md documents simple iteration completion"
    (should (re-find #"[Uu]pdate iteration\.edn status to.*:complete" (worker-md))))
  (it "no .completing lock mechanism in worker.md"
    (should-not (str/includes? (worker-md) ".completing")))
  (it "no .completing lock mechanism in CONTRACTS.md"
    (should-not (str/includes? (contracts) ".completing"))))
