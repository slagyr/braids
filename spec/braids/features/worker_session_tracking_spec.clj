(ns braids.features.worker-session-tracking-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Worker session tracking"

  (context "Generate deterministic session ID from bead ID"
    (it "Generate deterministic session ID from bead ID"
      (h/reset!)
      (h/set-bead-id "proj-abc")
      (h/generate-session-id!)
      (should= "braids-proj-abc-worker" (h/session-id-result))))

  (context "Same bead always generates same session ID"
    (it "Same bead always generates same session ID"
      (h/reset!)
      (h/set-bead-id "proj-xyz")
      (h/generate-session-id-twice!)
      (should (h/session-ids-identical?))))

  (context "Different beads generate different session IDs"
    (it "Different beads generate different session IDs"
      (h/reset!)
      (h/set-bead-id "proj-aaa")
      (h/set-bead-id "proj-bbb")
      (h/generate-session-ids-both!)
      (should (h/session-ids-different?))))

  (context "Session ID can be parsed back to bead ID"
    (it "Session ID can be parsed back to bead ID"
      (h/reset!)
      (h/set-session-id-literal "braids-proj-abc-worker")
      (h/parse-session-id!)
      (should= "proj-abc" (h/parsed-bead-id)))))
