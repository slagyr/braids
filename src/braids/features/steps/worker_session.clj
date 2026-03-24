(ns braids.features.steps.worker-session
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [speclj.core :refer :all]))

(defgiven bead "a bead with id \"{bead-id}\""
  [bead-id]
  (h/set-bead-id bead-id))

(defgiven another-bead "another bead with id \"{bead-id}\""
  [bead-id]
  (h/set-bead-id bead-id))

(defgiven session-id-literal "a session ID \"{session-id}\""
  [session-id]
  (h/set-session-id-literal session-id))

(defwhen generate-session-id "generating the session ID"
  []
  (h/generate-session-id!))

(defwhen generate-session-id-twice "generating the session ID twice"
  []
  (h/generate-session-id-twice!))

(defwhen generate-session-ids-both "generating session IDs for both"
  []
  (h/generate-session-ids-both!))

(defwhen parse-session-id "parsing the session ID"
  []
  (h/parse-session-id!))

(defthen assert-session-id "the session ID should be \"{expected}\""
  [expected]
  (should= expected (h/session-id-result)))

(defthen assert-ids-identical "both session IDs should be identical"
  []
  (should (h/session-ids-identical?)))

(defthen assert-ids-different "the session IDs should be different"
  []
  (should (h/session-ids-different?)))

(defthen assert-bead-id "the extracted bead ID should be \"{expected}\""
  [expected]
  (should= expected (h/parsed-bead-id)))
