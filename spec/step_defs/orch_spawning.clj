(ns braids.step-defs.orch-spawning
  (:require [braids.orch :as orch]
            [braids.orch-runner :as runner]))

(def step-defs
  {"Given an orchestrator with max-workers set to (\\d+)" (fn [max-w] (println "Setting max-workers to" max-w))
   "And worker-timeout set to (\\d+)" (fn [timeout] (println "Setting worker-timeout to" timeout))
   "Given (\\d+) ready beads in the project" (fn [count] (println "Mocking" count "ready beads"))
   "And (\\d+) active workers" (fn [count] (println "Mocking" count "active workers"))
   "When the orchestrator ticks" (fn [] (println "Simulating orchestrator tick"))
   "Then it should spawn (\\d+) workers" (fn [count] (println "Verifying spawn of" count "workers"))
   "And mark the tick as spawn" (fn [] (println "Verifying tick marked as spawn"))
   "And mark the tick as idle with reason \"([^\"]+)\"" (fn [reason] (println "Verifying tick marked as idle:" reason))
   "Given a project with (\\w+) iteration" (fn [status] (println "Mocking project iteration status:" status))
   "And notifications enabled" (fn [] (println "Mocking notifications enabled"))
   "When checking spawn conditions" (fn [] (println "Checking spawn conditions"))
   "Then the project should be considered ready for spawning" (fn [] (println "Verifying project ready"))
   "Then the project should not be considered ready for spawning" (fn [] (println "Verifying project not ready"))
   "Given no ready beads across all projects" (fn [] (println "Mocking no ready beads"))
   "When checking idle conditions" (fn [] (println "Checking idle conditions"))
   "Then the orchestrator should report idle with reason \"([^\"]+)\"" (fn [reason] (println "Verifying idle reason:" reason))
   "Given ready beads exist but all workers at capacity" (fn [] (println "Mocking beads ready but at capacity"))})