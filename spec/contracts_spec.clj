(ns contracts-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def home (System/getProperty "user.home"))
(def project-root (str (System/getProperty "user.dir")))
(def contracts-path (str project-root "/CONTRACTS.md"))
(def skill-path (str home "/.openclaw/skills/projects/SKILL.md"))

(def content (when (fs/exists? contracts-path) (slurp contracts-path)))
(def skill-content (when (fs/exists? skill-path) (slurp skill-path)))

(describe "CONTRACTS.md"

  (it "exists"
    (should (fs/exists? contracts-path)))

  (context "Required Sections"
    (it "has File Format Contracts"
      (should-contain "## 1. File Format Contracts" content))
    (it "has Orchestrator Invariants"
      (should-contain "## 2. Orchestrator Invariants" content))
    (it "has Worker Invariants"
      (should-contain "## 3. Worker Invariants" content))
    (it "has Cross-Cutting Invariants"
      (should-contain "## 4. Cross-Cutting Invariants" content)))

  (context "File Format Subsections"
    (it "documents all file formats"
      (doseq [fmt ["registry.md" "PROJECT.md" "ITERATION.md" "STATUS.md" ".orchestrator-state.json"]]
        (should-contain fmt content)))
    (it "does not document RETRO.md (removed feature)"
      (should-not (re-find #"### \d+\.\d+ RETRO\.md" content)))
    (it "documents deliverable format"
      (should-contain "Deliverable" content)))

  (context "Key Defaults"
    (it "default MaxWorkers=1"
      (should (re-find #"MaxWorkers.*1" content)))
    (it "default WorkerTimeout=3600"
      (should (re-find #"WorkerTimeout.*3600" content)))
    (it "default Autonomy=full"
      (should (re-find #"Autonomy.*full" content)))
    (it "default Priority=normal"
      (should (re-find #"Priority.*normal" content))))

  (context "Orchestrator Invariants"
    (it "no direct work invariant"
      (should (re-find #"never.*perform[s]? bead work" content)))
    (it "concurrency enforcement"
      (should-contain "MaxWorkers" content))
    (it "zombie detection documented"
      (should (re-find #"(?i)zombie" content)))
    (it "session label convention"
      (should-contain "project:<slug>:<bead-id>" content))
    (it "frequency scaling backoff values"
      (should (re-find #"30\s*min" content))))

  (context "Worker Invariants"
    (it "claim before work"
      (should (re-find #"(?i)claim.*before|Claim.*Before" content)))
    (it "dependency verification"
      (should (re-find #"(?i)dependenc" content)))
    (it "deliverable required"
      (should (re-find #"(?i)deliverable.*required|produces a deliverable" content)))
    (it "git commit on completion"
      (should-contain "git commit" content))
    (it "no .completing lock mechanism (removed with RETRO feature)"
      (should-not (str/includes? content ".completing")))
    (it "no RETRO.md generation (removed feature)"
      (should-not (re-find #"(?i)RETRO\.md" content)))
    (it "format tolerance"
      (should (re-find #"(?i)format tolerance" content)))
    (it "notification discipline"
      (should (re-find #"(?i)notification" content))))

  (context "Cross-Cutting"
    (it "path convention (~)"
      (should-contain "home directory" content))
    (it "immutable completed iterations"
      (should (re-find #"(?i)immutable" content)))
    (it "git as transport"
      (should-contain "git push" content)))

  (context "Notification Events"
    (it "all notification events documented"
      (doseq [event ["iteration-start" "bead-start" "bead-complete" "iteration-complete"
                      "no-ready-beads" "question" "blocker"]]
        (should-contain event content))))

  (context "Channel Agent Guardrail"
    (it "channel agent must not modify project files directly"
      (should (re-find #"(?i)channel.*must not.*modify.*project files.*directly|channel.*only.*create beads|beads only" content)))
    (it "SKILL.md documents channel convention"
      (should (re-find #"(?i)channel.*convention|channel.*planning.*notifications.*only|channel agent.*must not" skill-content))))

  (context "Valid Statuses"
    (it "registry statuses: active, paused, blocked"
      (should (re-find #"active.*paused.*blocked" content)))
    (it "iteration statuses: planning, active, complete"
      (should (re-find #"planning.*active.*complete" content)))))
