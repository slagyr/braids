(ns contracts-spec
  (:require [spec-helper :refer [describe context it should should-contain should-match]]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def home (System/getProperty "user.home"))
(def project-root (str (System/getProperty "user.dir")))
(def contracts (str project-root "/CONTRACTS.md"))
(def skill (str home "/.openclaw/skills/projects/SKILL.md"))

(def content (when (fs/exists? contracts) (slurp contracts)))

(describe "CONTRACTS.md Tests"

  (it "CONTRACTS.md exists"
    (fs/exists? contracts))

  ;; Required sections
  (context "Required Sections"
    (it "Section: File Format Contracts"
      (should-contain "## 1. File Format Contracts" content))
    (it "Section: Orchestrator Invariants"
      (should-contain "## 2. Orchestrator Invariants" content))
    (it "Section: Worker Invariants"
      (should-contain "## 3. Worker Invariants" content))
    (it "Section: Cross-Cutting Invariants"
      (should-contain "## 4. Cross-Cutting Invariants" content)))

  ;; File format subsections
  (context "File Format Subsections"
    (doseq [fmt ["registry.md" "PROJECT.md" "ITERATION.md" "RETRO.md" "STATUS.md" ".orchestrator-state.json"]]
      (it (str "documents format: " fmt)
        (should-contain fmt content)))
    (it "documents deliverable format"
      (should-contain "Deliverable" content)))

  ;; Key defaults
  (context "Key Defaults"
    (it "default MaxWorkers=1"
      (should-match #"MaxWorkers.*1" content))
    (it "default WorkerTimeout=1800"
      (should-match #"WorkerTimeout.*3600" content))
    (it "default Autonomy=full"
      (should-match #"Autonomy.*full" content))
    (it "default Priority=normal"
      (should-match #"Priority.*normal" content)))

  ;; Orchestrator invariants
  (context "Orchestrator Invariants"
    (it "no direct work invariant"
      (should-match #"never.*perform[s]? bead work" content))
    (it "concurrency enforcement"
      (should-contain "MaxWorkers" content))
    (it "zombie detection documented"
      (should-match #"(?i)zombie" content))
    (it "session label convention"
      (should-contain "project:<slug>:<bead-id>" content))
    (it "frequency scaling backoff values"
      (should-match #"30\s*min" content)))

  ;; Worker invariants
  (context "Worker Invariants"
    (it "claim before work"
      (should-match #"(?i)claim.*before|Claim.*Before" content))
    (it "dependency verification"
      (should-match #"(?i)dependenc" content))
    (it "deliverable required"
      (should-match #"(?i)deliverable.*required|produces a deliverable" content))
    (it "git commit on completion"
      (should-contain "git commit" content))
    (it "iteration completion lock (.completing)"
      (should-contain ".completing" content))
    (it "RETRO.md generation on iteration completion"
      (should-match #"(?i)RETRO\.md.*generat|generat.*RETRO\.md" content))
    (it "format tolerance"
      (should-match #"(?i)format tolerance" content))
    (it "notification discipline"
      (should-match #"(?i)notification" content)))

  ;; Cross-cutting
  (context "Cross-Cutting"
    (it "path convention (~)"
      (should-contain "home directory" content))
    (it "immutable completed iterations"
      (should-match #"(?i)immutable" content))
    (it "git as transport"
      (should-contain "git push" content)))

  ;; Notification events
  (context "Notification Events"
    (doseq [event ["iteration-start" "bead-start" "bead-complete" "iteration-complete"
                    "no-ready-beads" "question" "blocker"]]
      (it (str "event '" event "' documented")
        (should-contain event content))))

  ;; Channel agent guardrail
  (context "Channel Agent Guardrail"
    (it "channel agent must not modify project files directly"
      (should-match #"(?i)channel.*must not.*modify.*project files.*directly|channel.*only.*create beads|beads only" content))
    (it "SKILL.md documents channel convention"
      (should-match #"(?i)channel.*convention|channel.*planning.*notifications.*only|channel agent.*must not" (slurp skill))))

  ;; Valid statuses
  (context "Valid Statuses"
    (it "registry statuses: active, paused, blocked"
      (should-match #"active.*paused.*blocked" content))
    (it "iteration statuses: planning, active, complete"
      (should-match #"planning.*active.*complete" content))))
