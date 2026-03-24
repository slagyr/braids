(ns braids.features.steps.orch-spawning
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [speclj.core :refer :all]))

(defgiven active-workers "project \"{slug}\" has {count:int} active workers"
  [slug count]
  (h/set-active-workers slug count))

(defwhen orch-tick "the orchestrator ticks"
  []
  (h/orch-tick!))

(defthen assert-action "the action should be \"{expected}\""
  [expected]
  (should= expected (h/tick-action)))

(defthen assert-idle-reason "the idle reason should be \"{expected}\""
  [expected]
  (should= expected (h/idle-reason)))

(defthen spawns-should-include "the spawns should include"
  [table]
  (let [{:keys [headers rows]} table]
    (doseq [row rows]
      (should (h/spawn-includes? (zipmap headers row))))))

(defthen spawns-not-include-bead "the spawns should not include bead \"{bead-id}\""
  [bead-id]
  (should (h/spawn-excludes-bead? bead-id)))

(defthen spawn-missing-key "the spawn for \"{bead-id}\" should not have key \"{key}\""
  [bead-id key]
  (should (h/spawn-missing-key? bead-id key)))

(defthen spawn-at-project "spawn {index:int} should be for project \"{project}\""
  [index project]
  (should= project (:project (h/spawn-at index))))
