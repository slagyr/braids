(ns braids.features.harness-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Feature harness"

  (before (h/reset!))

  (describe "state management"

    (it "starts with empty state after reset"
      (should= {} (h/sessions))
      (should= {} (h/configs))
      (should= {} (h/bead-statuses))
      (should= [] (h/zombies))))

  (describe "add-project-config"

    (it "adds a project config with worker-timeout"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (should= {:worker-timeout 3600} (get (h/configs) "proj"))))

  (describe "add-session"

    (it "adds a session with label"
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (should= {:label "project:proj:proj-abc"} (get (h/sessions) "s1"))))

  (describe "set-session-status"

    (it "sets session status and age"
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (let [session (get (h/sessions) "s1")]
        (should= "running" (:status session))
        (should= 100 (:age-seconds session)))))

  (describe "set-bead-status"

    (it "sets bead status"
      (h/set-bead-status "proj-abc" "closed")
      (should= "closed" (get (h/bead-statuses) "proj-abc"))))

  (describe "check-zombies!"

    (it "detects zombie when bead is closed"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should= 1 (count (h/zombies))))

    (it "stores empty zombies when none detected"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "open")
      (h/check-zombies!)
      (should= [] (h/zombies))))

  (describe "zombie?"

    (it "returns true when session is a zombie"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should (h/zombie? "s1")))

    (it "returns false when session is not a zombie"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "open")
      (h/check-zombies!)
      (should-not (h/zombie? "s1"))))

  (describe "zombie-reason"

    (it "returns the zombie reason for a session"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should= "bead-closed" (h/zombie-reason "s1"))))

  ;; --- Orch spawning harness ---

  (describe "add-project"

    (it "adds a project to registry with active status and sets config"
      (h/add-project "alpha" {:max-workers 2})
      (should= "spawn" (do
                          (h/set-active-iteration "alpha" "003")
                          (h/set-ready-beads "alpha" 1)
                          (h/set-active-workers "alpha" 0)
                          (h/orch-tick!)
                          (h/tick-action)))))

  (describe "set-active-iteration"

    (it "sets an active iteration for a project"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))))

  (describe "remove-iteration"

    (it "removes iteration for a project"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/remove-iteration "alpha")
      (h/orch-tick!)
      (should= "idle" (h/tick-action))))

  (describe "set-ready-beads"

    (it "sets ready beads count for a project"
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 3)
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= 2 (h/spawn-count))))

  (describe "set-ready-bead-with-id"

    (it "sets a specific ready bead by id"
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-bead-with-id "alpha" "alpha-abc")
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= "project:alpha:alpha-abc" (h/spawn-label))))

  (describe "set-active-workers"

    (it "sets the active worker count"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/set-active-workers "alpha" 1)
      (h/orch-tick!)
      (should= "idle" (h/tick-action))
      (should= "all-at-capacity" (h/idle-reason))))

  (describe "orch-tick!"

    (it "runs orch/tick with accumulated state"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 1)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))))

  (describe "orch-tick-project!"

    (it "runs orch/tick for a single project only"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/add-project "beta" {:max-workers 1})
      ;; beta has no iteration — should be idle
      (h/orch-tick-project! "beta")
      (should= "idle" (h/tick-action))
      (should= "no-active-iterations" (h/idle-reason))))

  (describe "result accessors"

    (it "tick-action returns the action from tick result"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 1)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action)))

    (it "spawn-count returns number of spawns"
      (h/add-project "proj" {:max-workers 2})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 3)
      (h/orch-tick!)
      (should= 2 (h/spawn-count)))

    (it "idle-reason returns the idle reason"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 0)
      (h/orch-tick!)
      (should= "no-ready-beads" (h/idle-reason)))

    (it "spawn-label returns the first spawn label"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-bead-with-id "proj" "proj-abc")
      (h/orch-tick!)
      (should= "project:proj:proj-abc" (h/spawn-label)))))

