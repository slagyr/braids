(ns braids.features.steps.iteration
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defgiven iteration-edn #"^iteration EDN with number \"([^\"]+)\" and status \"([^\"]+)\" and (\d+) stor(?:y|ies)$"
  [number status count]
  (h/set-iteration-edn number status (parse-long count)))

(defgiven edn-no-guardrails-or-notes "the EDN has no guardrails or notes"
  []
  nil)

(defgiven iteration-with-status "an iteration with number \"{number}\" and status \"{status}\" and stories"
  [number status]
  (h/set-iteration-with-status number status))

(defgiven iteration-no-number "an iteration with no number"
  []
  (h/set-iteration-no-number))

(defgiven iteration-with-stories "an iteration with stories \"{id1}\" and \"{id2}\""
  [id1 id2]
  (h/set-iteration-stories [id1 id2]))

(defgiven iteration-with-story "an iteration with story \"{story-id}\""
  [story-id]
  (h/set-iteration-stories [story-id]))

(defgiven iter-bead-status "bead \"{bead-id}\" has status \"{status}\" and priority {priority:int}"
  [bead-id status priority]
  (h/add-iter-bead bead-id status priority))

(defgiven no-bead-data "no bead data exists"
  []
  nil)

(defgiven annotated-stories "annotated stories with {closed:int} closed and {open:int} open out of {total:int} total"
  [closed open total]
  (h/set-annotated-stories closed open total))

(defgiven iteration-no-stories "an iteration with no stories"
  []
  (h/set-iteration-stories []))

(defgiven iteration-number-status "an iteration \"{number}\" with status \"{status}\""
  [number status]
  (h/set-iteration-number-status number status))

(defgiven story-with-status "a story \"{story-id}\" with status \"{status}\""
  [story-id status]
  (h/add-story-with-status story-id status))

(defgiven completion-stats "completion stats of {closed:int} closed out of {total:int}"
  [closed total]
  (h/set-completion-stats closed total))

(defwhen parse-iteration-edn "parsing the iteration EDN"
  []
  (h/parse-iteration-edn!))

(defwhen validate-iteration "validating the iteration"
  []
  (h/validate-iteration!))

(defwhen annotate-stories "annotating stories with bead data"
  []
  (h/annotate-stories!))

(defwhen calculate-completion-stats "calculating completion stats"
  []
  (h/calculate-completion-stats!))

(defwhen format-iteration "formatting the iteration"
  []
  (h/format-iteration!))

(defwhen format-iteration-json "formatting the iteration as JSON"
  []
  (h/format-iteration-json!))

(defthen assert-iteration-number "the iteration number should be \"{expected}\""
  [expected]
  (should= expected (h/iteration-number)))

(defthen assert-iteration-status "the iteration status should be \"{expected}\""
  [expected]
  (should= expected (h/iteration-status)))

(defthen assert-iteration-guardrails-empty "the iteration guardrails should be empty"
  []
  (should (empty? (h/iteration-guardrails))))

(defthen assert-iteration-notes-empty "the iteration notes should be empty"
  []
  (should (empty? (h/iteration-notes))))

(defthen assert-story-status "story \"{story-id}\" should have status \"{expected}\""
  [story-id expected]
  (should= expected (h/story-status story-id)))

(defthen assert-total "the total should be {expected:int}"
  [expected]
  (should= expected (h/stats-total)))

(defthen assert-closed-count "the closed count should be {expected:int}"
  [expected]
  (should= expected (h/stats-closed)))

(defthen assert-completion-percent "the completion percent should be {expected:int}"
  [expected]
  (should= expected (h/stats-percent)))

(defthen assert-json-contains "the JSON should contain \"{expected}\""
  [expected]
  (should (str/includes? (h/iter-json-output) expected)))
