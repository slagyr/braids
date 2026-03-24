(ns braids.features.steps.zombie-detection
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [speclj.core :refer :all]))

(defgiven project-config "a project \"{slug}\" with worker-timeout {worker-timeout:int}"
  [slug worker-timeout]
  (h/add-project-config slug {:worker-timeout worker-timeout}))

(defgiven session "a session \"{session-id}\" with label \"{label}\""
  [session-id label]
  (h/add-session session-id {:label label}))

(defgiven session-status "session \"{session-id}\" has status \"{status}\" and age {age-seconds:int} seconds"
  [session-id status age-seconds]
  (h/set-session-status session-id status age-seconds))

(defgiven bead-status "bead \"{bead-id}\" has status \"{status}\""
  [bead-id status]
  (h/set-bead-status bead-id status))

(defgiven bead-no-status "bead \"{bead-id}\" has no recorded status"
  [bead-id]
  nil)

(defwhen check-zombies "checking for zombies"
  []
  (h/check-zombies!))

(defthen assert-zombie "session \"{session-id}\" should be a zombie with reason \"{reason}\""
  [session-id reason]
  (should (h/zombie? session-id))
  (should= reason (h/zombie-reason session-id)))

(defthen assert-no-zombies "no zombies should be detected"
  []
  (should= [] (h/zombies)))
