(ns braids.features.orch-spawning-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Orchestrator spawning behavior"

  (context "Spawn workers when beads ready and capacity available"
    (it "Spawn workers when beads ready and capacity available"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 3)
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))
      (should= 2 (h/spawn-count))))

  (context "Spawn fewer workers when fewer beads than capacity"
    (it "Spawn fewer workers when fewer beads than capacity"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))
      (should= 1 (h/spawn-count))))

  (context "Idle when no ready beads"
    (it "Idle when no ready beads"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 0)
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= "idle" (h/tick-action))
      (should= "no-ready-beads" (h/idle-reason))))

  (context "Idle when at capacity"
    (it "Idle when at capacity"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 3)
      (h/set-active-workers "alpha" 2)
      (h/orch-tick!)
      (should= "idle" (h/tick-action))
      (should= "all-at-capacity" (h/idle-reason))))

  (context "Idle when no active iterations"
    (it "Idle when no active iterations"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/add-project "beta" {:max-workers 1})
      (h/remove-iteration "beta")
      (h/set-ready-beads "beta" 3)
      (h/set-active-workers "beta" 0)
      (h/orch-tick-project! "beta")
      (should= "idle" (h/tick-action))
      (should= "no-active-iterations" (h/idle-reason))))

  (context "Spawn respects per-project capacity independently"
    (it "Spawn respects per-project capacity independently"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/add-project "beta" {:max-workers 1})
      (h/set-active-iteration "beta" "001")
      (h/set-ready-beads "alpha" 2)
      (h/set-active-workers "alpha" 0)
      (h/set-ready-beads "beta" 1)
      (h/set-active-workers "beta" 0)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))
      (should= 3 (h/spawn-count))))

  (context "Spawn includes correct label format"
    (it "Spawn includes correct label format"
      (h/reset!)
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-bead-with-id "alpha" "alpha-abc")
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))
      (should= "project:alpha:alpha-abc" (h/spawn-label))))

  ;; --- Feature-driven specs using configure-projects-from-table ---

  (context "configure-projects-from-table handles extended config columns"
    (it "passes worker-timeout, worker-agent, worker-model, worker-thinking, channel to config"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path"]
        [["alpha" "active" "normal" "2" "003" "0" "/projects/alpha"]])
      (h/configure-projects-from-table
        ["slug" "worker-timeout" "worker-agent" "worker-model" "worker-thinking" "channel"]
        [["alpha" "7200" "agent-abc" "opus" "high" "#alpha"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Implement auth" "ready"]])
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))
      (should (h/spawn-includes? {"project" "alpha" "bead" "alpha-aa1"
                                  "worker-timeout" "7200" "worker-agent" "agent-abc"
                                  "worker-model" "opus" "worker-thinking" "high"
                                  "channel" "#alpha"}))))

  (context "spawn-includes? checks that spawn entries match expected rows"
    (it "returns true when spawn contains matching entry"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path"]
        [["alpha" "active" "normal" "2" "003" "0" "/projects/alpha"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task 1" "ready"]])
      (h/orch-tick!)
      (should (h/spawn-includes? {"project" "alpha" "bead" "alpha-aa1"}))))

  (context "spawn-excludes-bead? checks bead not in spawns"
    (it "returns true when bead is not in any spawn"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path"]
        [["alpha" "active" "normal" "2" "003" "0" "/projects/alpha"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task 1" "ready"]])
      (h/orch-tick!)
      (should (h/spawn-excludes-bead? "alpha-aa2"))))

  (context "spawn-missing-key? checks key absence on specific spawn"
    (it "returns true when key is absent from spawn entry"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path"]
        [["alpha" "active" "normal" "2" "003" "0" "/projects/alpha"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task 1" "ready"]])
      (h/orch-tick!)
      (should (h/spawn-missing-key? "alpha-aa1" "worker-agent"))))

  (context "spawn-at returns nth spawn entry (1-indexed)"
    (it "returns the correct spawn by index"
      (h/reset!)
      (h/configure-projects-from-table
        ["slug" "status" "priority" "max-workers" "active-iteration" "active-workers" "path"]
        [["high" "active" "high" "1" "001" "0" "/projects/high"]
         ["alpha" "active" "normal" "1" "003" "0" "/projects/alpha"]])
      (h/set-project-beads "high"
        ["id" "title" "status"]
        [["high-h1" "Task 1" "ready"]])
      (h/set-project-beads "alpha"
        ["id" "title" "status"]
        [["alpha-aa1" "Task 1" "ready"]])
      (h/orch-tick!)
      (should= "high" (:project (h/spawn-at 1)))
      (should= "alpha" (:project (h/spawn-at 2))))))
