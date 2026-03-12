(ns braids.features.orch-output-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Orchestrator tick output"

  (context "ready, in-progress, and blocked beads are printed"
    (it "ready, in-progress, and blocked beads are printed"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["alpha" "active" "normal" "2" "005" "0"] ["beta" "active" "normal" "1" "003" "0"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task A1" "ready"] ["alpha-aa2" "Task A2" "closed"]])
      (h/set-project-beads "beta"
        ["id" "title" "status"]
        [["beta-bb1" "Task B1" "ready"] ["beta-bb2" "Task B2" "in-progress"] ["beta-bb3" "Task B3" "blocked"] ["beta-bb4" "Task B4" "closed"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "alpha  active  iteration 005  workers:0/2  beads:"))
      (should (h/output-contains-line? "○ aa1 Task A1              ready"))
      (should (h/output-contains-line? "beta  active  iteration 003  workers:0/1  beads:"))
      (should (h/output-contains-line? "○ bb1 Task B1              ready"))
      (should (h/output-contains-line? "● bb2 Task B2              in-progress"))
      (should (h/output-contains-line? "✗ bb3 Task B3              blocked"))
      (should-not (h/output-contains? "aa2"))
      (should-not (h/output-contains? "bb4"))))

  (context "long bead titles are truncated to 20 characters"
    (it "long bead titles are truncated to 20 characters"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["alpha" "active" "normal" "1" "001" "0"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-xx1" "Short" "ready"] ["alpha-xx2" "Exactly Twenty Chars" "ready"] ["alpha-xx3" "This Title Is Way Too Long For Col" "ready"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "○ xx1 Short                ready"))
      (should (h/output-contains-line? "○ xx2 Exactly Twenty Chars ready"))
      (should (h/output-contains-line? "○ xx3 This Title Is Way... ready"))))

  (context "project with no active iteration"
    (it "project with no active iteration"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["gamma" "active" "normal" "1" "" "0"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "gamma  active  (no iteration)  workers:0/1"))))

  (context "project with active workers shows worker count"
    (it "project with active workers shows worker count"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["alpha" "active" "normal" "3" "002" "2"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task A1" "ready"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "alpha  active  iteration 002  workers:2/3  beads:"))
      (should (h/output-contains-line? "○ aa1 Task A1              ready"))))

  (context "project with no open beads shows none"
    (it "project with no open beads shows none"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["alpha" "active" "normal" "1" "001" "0"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "alpha  active  iteration 001  workers:0/1  beads: (none)"))))

  (context "idle tick appends decision line"
    (it "idle tick appends decision line"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["alpha" "active" "normal" "1" "001" "0"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "→ idle: no-ready-beads"))))

  (context "projects are ordered by priority"
    (it "projects are ordered by priority"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["low" "active" "low" "1" "001" "0"] ["high" "active" "high" "1" "001" "0"] ["norm" "active" "normal" "1" "001" "0"]])
      (h/orch-tick!)
      (should (h/output-has-before? "high" "norm"))
      (should (h/output-has-before? "norm" "low"))))

  (context "paused projects are excluded from output"
    (it "paused projects are excluded from output"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers"]
        [["alpha" "active" "normal" "1" "001" "0"] ["beta" "paused" "normal" "1" "001" "0"]])
      (h/orch-tick!)
      (should (h/output-contains-line? "alpha"))
      (should-not (h/output-contains? "beta"))))

  (context "spawn log prints full worker command"
    (it "spawn log prints full worker command"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path" "worker-agent" "worker-timeout" "channel"]
        [["alpha" "active" "normal" "1" "001" "0" "/projects/alpha" "scrapper" "1800" "#alpha"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task 1" "ready"]])
      (h/orch-tick!)
      (should (h/output-contains-line?
        "→ openclaw agent --message <task> --session-id braids-alpha-aa1-worker --thinking high --timeout 1800 --agent scrapper"))
      (should-not (h/output-contains? "→ bead="))
      (should-not (h/output-contains? "Spawned worker:")))))
