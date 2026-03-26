(ns braids.features.steps.zombie-detection
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.orch :as orch]
            [braids.orch-runner :as runner]
            [clojure.string :as str]
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

(defgiven zombies-detected "zombies have been detected"
  []
  (g/assoc-in! [:sessions "z1"] {:label "project:proj:proj-z1" :status "running" :age-seconds 100})
  (g/assoc-in! [:sessions "z2"] {:label "project:proj:proj-z2" :status "completed" :age-seconds 50})
  (g/assoc-in! [:bead-statuses "proj-z1"] "closed")
  (g/assoc-in! [:bead-statuses "proj-z2"] "open")
  (check-zombies*))

(defwhen cleanup-zombies "cleaning up zombies"
  []
  (let [zombies (g/get :zombies)
        kills (filterv :session-id zombies)
        report (runner/format-zombie-log zombies)]
    (g/assoc! :kills kills)
    (g/assoc! :cleanup-report report)))

(defthen zombie-sessions-killed "the zombie sessions should be killed"
  []
  (let [kills (g/get :kills)
        zombies (g/get :zombies)]
    (should (pos? (count zombies)))
    (should= (count zombies) (count kills))))

(defthen cleanup-report-lists-killed "a cleanup report should list each killed session and its reason"
  []
  (let [report (g/get :cleanup-report)
        zombies (g/get :zombies)]
    (should (seq report))
    (doseq [{:keys [bead reason]} zombies]
      (should (some #(and (str/includes? % bead) (str/includes? % reason)) report)))))
