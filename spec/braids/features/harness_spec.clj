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
      (should= "bead-closed" (h/zombie-reason "s1")))))
