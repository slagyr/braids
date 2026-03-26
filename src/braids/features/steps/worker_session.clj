(ns braids.features.steps.worker-session
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.orch :as orch]
            [speclj.core :refer :all]))

(defn- generate-session-id* []
  (let [bead-id (first (g/get :bead-ids))
        result (orch/worker-session-id bead-id)]
    (g/assoc! :session-id-result result)))

(defn- generate-session-id-twice* []
  (let [bead-id (first (g/get :bead-ids))
        id1 (orch/worker-session-id bead-id)
        id2 (orch/worker-session-id bead-id)]
    (g/assoc! :session-ids [id1 id2])))

(defn- generate-session-ids-both* []
  (let [ids (mapv orch/worker-session-id (g/get :bead-ids))]
    (g/assoc! :session-ids ids)))

(defn- parse-session-id* []
  (let [session-id (g/get :session-id-literal)
        result (orch/parse-worker-session-id session-id)]
    (g/assoc! :parsed-bead-id result)))

(defgiven bead "a bead with id {bead-id:string}"
  [bead-id]
  (g/update! :bead-ids (fnil conj []) bead-id))

(defgiven another-bead "another bead with id {bead-id:string}"
  [bead-id]
  (g/update! :bead-ids (fnil conj []) bead-id))

(defgiven session-id-literal "a session ID {session-id:string}"
  [session-id]
  (g/assoc! :session-id-literal session-id))

(defwhen generate-session-id "generating the session ID"
  []
  (generate-session-id*))

(defwhen generate-session-id-twice "generating the session ID twice"
  []
  (generate-session-id-twice*))

(defwhen generate-session-ids-both "generating session IDs for both"
  []
  (generate-session-ids-both*))

(defwhen parse-session-id "parsing the session ID"
  []
  (parse-session-id*))

(defthen assert-session-id "the session ID should be {expected:string}"
  [expected]
  (should= expected (g/get :session-id-result)))

(defthen assert-ids-identical "both session IDs should be identical"
  []
  (let [[a b] (g/get :session-ids)]
    (should (= a b))))

(defthen assert-ids-different "the session IDs should be different"
  []
  (should (apply distinct? (g/get :session-ids))))

(defthen assert-bead-id "the extracted bead ID should be {expected:string}"
  [expected]
  (should= expected (g/get :parsed-bead-id)))

(defgiven session-already-active "a session {session-id:string} is already active"
  [session-id]
  (g/update! :active-session-ids (fnil conj #{}) session-id))

(defgiven session-with-id "a session with id {session-id:string}"
  [session-id]
  (g/assoc! :session-id-literal session-id))

(defgiven no-bead-exists "no bead exists with id {bead-id:string}"
  [bead-id]
  nil)

(defwhen consider-spawning "the orchestrator considers spawning for bead {bead-id:string}"
  [bead-id]
  (let [active-ids (g/get :active-session-ids)
        result (orch/check-spawn-allowed bead-id active-ids)]
    (g/assoc! :spawn-check-result result)))

(defwhen check-session-validity "checking session validity"
  []
  (let [session-id (g/get :session-id-literal)
        bead-ids (or (g/get :known-bead-ids) #{})
        result (orch/check-session-validity session-id bead-ids)]
    (g/assoc! :validity-result result)))

(defthen spawning-prevented "spawning should be prevented with reason {reason:string}"
  [reason]
  (let [result (g/get :spawn-check-result)]
    (should= false (:allowed result))
    (should= reason (:reason result))))

(defthen flagged-for-cleanup "the session should be flagged for cleanup"
  []
  (let [result (g/get :validity-result)]
    (should= false (:valid result))
    (should= true (:cleanup result))))
