(ns braids.features.zombie-detection-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Zombie detection"

  (context "Detect zombie when bead is closed but session still running"
    (it "Detect zombie when bead is closed but session still running"
      (h/reset!)
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should (h/zombie? "s1"))
      (should= "bead-closed" (h/zombie-reason "s1"))))

  (context "Detect zombie when session exceeds timeout"
    (it "Detect zombie when session exceeds timeout"
      (h/reset!)
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s2" {:label "project:proj:proj-def"})
      (h/set-session-status "s2" "running" 7200)
      (h/set-bead-status "proj-def" "open")
      (h/check-zombies!)
      (should (h/zombie? "s2"))
      (should= "timeout" (h/zombie-reason "s2"))))

  (context "No zombie when session is within timeout and bead is open"
    (it "No zombie when session is within timeout and bead is open"
      (h/reset!)
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s3" {:label "project:proj:proj-ghi"})
      (h/set-session-status "s3" "running" 100)
      (h/set-bead-status "proj-ghi" "open")
      (h/check-zombies!)
      (should= [] (h/zombies))))

  (context "Detect zombie when session has ended"
    (it "Detect zombie when session has ended"
      (h/reset!)
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s4" {:label "project:proj:proj-jkl"})
      (h/set-session-status "s4" "completed" 50)
      (h/set-bead-status "proj-jkl" "open")
      (h/check-zombies!)
      (should (h/zombie? "s4"))
      (should= "session-ended" (h/zombie-reason "s4"))))

  (context "Missing bead status defaults to open"
    (it "Missing bead status defaults to open"
      (h/reset!)
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s5" {:label "project:proj:proj-mno"})
      (h/set-session-status "s5" "running" 100)
      (h/check-zombies!)
      (should= [] (h/zombies)))))
