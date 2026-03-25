(ns braids.features.steps.zombie-detection
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.orch :as orch]
            [speclj.core :refer :all]))

(defn- check-zombies* []
  (let [sessions (g/get :sessions)
        configs (g/get :configs)
        bead-statuses (g/get :bead-statuses)
        session-list (mapv (fn [[sid attrs]]
                             (merge {:session-id sid} attrs))
                           sessions)
        result (orch/detect-zombies session-list configs bead-statuses)]
    (g/assoc! :zombies result)))

(defgiven project-config "a project {slug:string} with worker-timeout {worker-timeout:int}"
  [slug worker-timeout]
  (g/assoc-in! [:configs slug] {:worker-timeout worker-timeout}))

(defgiven session "a session {session-id:string} with label {label:string}"
  [session-id label]
  (g/assoc-in! [:sessions session-id] {:label label}))

(defgiven session-status "session {session-id:string} has status {status:string} and age {age-seconds:int} seconds"
  [session-id status age-seconds]
  (g/update-in! [:sessions session-id] merge {:status status :age-seconds age-seconds}))

(defgiven bead-status #"^bead \"([^\"]+)\" has status \"([^\"]+)\"$"
  [bead-id status]
  (g/assoc-in! [:bead-statuses bead-id] status))

(defgiven bead-no-status "bead {bead-id:string} has no recorded status"
  [bead-id]
  nil)

(defwhen check-zombies "checking for zombies"
  []
  (check-zombies*))

(defthen assert-zombie "session {session-id:string} should be a zombie with reason {reason:string}"
  [session-id reason]
  (let [session (get (g/get :sessions) session-id)
        label (:label session)
        zombies (g/get :zombies)]
    (should (some #(= label (:label %)) zombies))
    (should= reason (:reason (first (filter #(= label (:label %)) zombies))))))

(defthen assert-no-zombies "no zombies should be detected"
  []
  (should= [] (g/get :zombies)))
