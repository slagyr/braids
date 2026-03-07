(ns braids.features.iteration-management-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Iteration management"

  (context "Parse iteration EDN with defaults for missing fields"
    (it "Parse iteration EDN with defaults for missing fields"
      (h/reset!)
      (h/set-iteration-edn "003" "active" 1)
      (h/parse-iteration-edn!)
      (should= "003" (h/iteration-number))
      (should= "active" (h/iteration-status))
      (should (empty? (h/iteration-guardrails)))
      (should (empty? (h/iteration-notes)))))

  (context "Validate rejects invalid iteration status"
    (it "Validate rejects invalid iteration status"
      (h/reset!)
      (h/set-iteration-with-status "001" "bogus")
      (h/validate-iteration!)
      (should-not (empty? (h/validation-errors)))
      (should (some #(clojure.string/includes? % "Invalid status") (h/validation-errors)))))

  (context "Validate rejects missing required fields"
    (it "Validate rejects missing required fields"
      (h/reset!)
      (h/set-iteration-no-number)
      (h/validate-iteration!)
      (should-not (empty? (h/validation-errors)))
      (should (some #(clojure.string/includes? % "Missing :number") (h/validation-errors)))))

  (context "Annotate stories with bead data"
    (it "Annotate stories with bead data"
      (h/reset!)
      (h/set-iteration-stories ["proj-abc" "proj-def"])
      (h/add-iter-bead "proj-abc" "open" 1)
      (h/add-iter-bead "proj-def" "closed" 2)
      (h/annotate-stories!)
      (should= "open" (h/story-status "proj-abc"))
      (should= "closed" (h/story-status "proj-def"))))

  (context "Annotate marks missing beads as unknown"
    (it "Annotate marks missing beads as unknown"
      (h/reset!)
      (h/set-iteration-stories ["proj-xyz"])
      (h/annotate-stories!)
      (should= "unknown" (h/story-status "proj-xyz"))))

  (context "Completion stats calculation"
    (it "Completion stats calculation"
      (h/reset!)
      (h/set-annotated-stories 2 2 4)
      (h/calculate-completion-stats!)
      (should= 4 (h/stats-total))
      (should= 2 (h/stats-closed))
      (should= 50 (h/stats-percent))))

  (context "Completion stats for empty iteration"
    (it "Completion stats for empty iteration"
      (h/reset!)
      (h/set-iteration-stories [])
      (h/calculate-completion-stats!)
      (should= 0 (h/stats-total))
      (should= 0 (h/stats-closed))
      (should= 0 (h/stats-percent))))

  (context "Format iteration with status icons"
    (it "Format iteration with status icons"
      (h/reset!)
      (h/set-iteration-number-status "009" "active")
      (h/add-story-with-status "proj-abc" "open")
      (h/add-story-with-status "proj-def" "closed")
      (h/set-completion-stats 1 2)
      (h/format-iteration!)
      (should (clojure.string/includes? (h/output) "Iteration 009"))
      (should (clojure.string/includes? (h/output) "active"))
      (should (clojure.string/includes? (h/output) "50%"))))

  (context "Format iteration as JSON"
    (it "Format iteration as JSON"
      (h/reset!)
      (h/set-iteration-number-status "001" "active")
      (h/add-story-with-status "a" "open")
      (h/set-completion-stats 0 1)
      (h/format-iteration-json!)
      (should (clojure.string/includes? (h/iter-json-output) "number"))
      (should (clojure.string/includes? (h/iter-json-output) "stories"))
      (should (clojure.string/includes? (h/iter-json-output) "percent")))))
