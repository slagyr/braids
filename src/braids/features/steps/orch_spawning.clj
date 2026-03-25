(ns braids.features.steps.orch-spawning
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.orch :as orch]
            [braids.orch-runner :as orch-runner]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defn- orch-tick-impl! []
  (let [registry (g/get :registry)
        configs (g/get :configs)
        iterations (g/get :iterations)
        beads (g/get :beads)
        workers (g/get :workers)
        open-beads (or (g/get :open-beads) {})
        result (orch/tick registry configs iterations beads workers {})
        debug-output (orch/format-debug-output registry configs iterations open-beads result workers)
        spawn-lines (when (= "spawn" (:action result))
                      (let [spawn-config (reduce (fn [cfg spawn]
                                                   (let [slug (some (fn [[s _]]
                                                                     (when (str/starts-with? (:bead spawn) (str s "-")) s))
                                                                   configs)]
                                                     (merge cfg (get configs slug))))
                                                 {}
                                                 (:spawns result))]
                        (orch-runner/format-spawn-log spawn-config result)))
        output (if spawn-lines
                 (str debug-output "\n" (str/join "\n" spawn-lines))
                 debug-output)]
    (g/assoc! :tick-result result :tick-output output :output output)))

(defn- spawn-includes? [expected]
  (let [spawns (:spawns (g/get :tick-result))]
    (some (fn [spawn]
            (every? (fn [[k v]]
                      (let [spawn-val (get spawn (keyword k))]
                        (= (str v) (str spawn-val))))
                    expected))
          spawns)))

(defn- spawn-excludes-bead? [bead-id]
  (let [spawns (:spawns (g/get :tick-result))]
    (not (some #(= bead-id (:bead %)) spawns))))

(defn- spawn-missing-key? [bead-id key-name]
  (let [spawns (:spawns (g/get :tick-result))
        spawn (first (filter #(= bead-id (:bead %)) spawns))]
    (when spawn
      (not (contains? spawn (keyword key-name))))))

(defgiven active-workers "project {slug:string} has {count:int} active workers"
  [slug count]
  (g/assoc-in! [:workers slug] count))

(defwhen orch-tick "the orchestrator ticks"
  []
  (orch-tick-impl!))

(defthen assert-action "the action should be {expected:string}"
  [expected]
  (should= expected (:action (g/get :tick-result))))

(defthen assert-idle-reason "the idle reason should be {expected:string}"
  [expected]
  (should= expected (:reason (g/get :tick-result))))

(defthen spawns-should-include "the spawns should include"
  [table]
  (let [{:keys [headers rows]} table]
    (doseq [row rows]
      (should (spawn-includes? (zipmap headers row))))))

(defthen spawns-not-include-bead "the spawns should not include bead {bead-id:string}"
  [bead-id]
  (should (spawn-excludes-bead? bead-id)))

(defthen spawn-missing-key "the spawn for {bead-id:string} should not have key {key:string}"
  [bead-id key]
  (should (spawn-missing-key? bead-id key)))

(defthen spawn-at-project "spawn {index:int} should be for project {project:string}"
  [index project]
  (should= project (:project (nth (:spawns (g/get :tick-result)) (dec index) nil))))
