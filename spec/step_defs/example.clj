(ns braids.step-defs.example)

(def step-defs
  {"Given a step" (fn [] (println "Given a step"))
   "When another step" (fn [] (println "When another step"))
   "Then final step" (fn [] (println "Then final step"))})