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
      (should= "project:alpha:alpha-abc" (h/spawn-label)))))
