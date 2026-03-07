(ns braids.features.ready-beads-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Ready beads"

  (context "Ready beads filters to active projects only"
    (it "Ready beads filters to active projects only"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["alpha" "active" "normal"] ["beta" "paused" "normal"]])
      (h/set-project-config "alpha" {:max-workers 1})
      (h/set-project-config "beta" {:max-workers 1})
      (h/set-project-ready-beads "alpha"
        ["id" "title" "priority"]
        [["alpha-aaa" "Task A" "P1"]])
      (h/set-project-ready-beads "beta"
        ["id" "title" "priority"]
        [["beta-bbb" "Task B" "P1"]])
      (h/compute-ready-beads!)
      (should (h/result-contains-bead? "alpha-aaa"))
      (should-not (h/result-contains-bead? "beta-bbb"))))

  (context "Ready beads respects worker capacity"
    (it "Ready beads respects worker capacity"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:max-workers 1})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task A" "P1"]])
      (h/set-active-workers "proj" 1)
      (h/compute-ready-beads!)
      (should (empty? (h/ready-result)))))

  (context "Ready beads returns beads when under capacity"
    (it "Ready beads returns beads when under capacity"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:max-workers 3})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task A" "P0"] ["proj-def" "Task B" "P1"]])
      (h/set-active-workers "proj" 2)
      (h/compute-ready-beads!)
      (should (h/result-contains-bead? "proj-abc"))
      (should (h/result-contains-bead? "proj-def"))))

  (context "Ready beads orders by project priority"
    (it "Ready beads orders by project priority"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["low" "active" "low"] ["high" "active" "high"] ["norm" "active" "normal"]])
      (h/set-project-config "low" {:max-workers 1})
      (h/set-project-config "high" {:max-workers 1})
      (h/set-project-config "norm" {:max-workers 1})
      (h/set-project-ready-beads "low"
        ["id" "title" "priority"]
        [["low-aaa" "Low task" "P2"]])
      (h/set-project-ready-beads "high"
        ["id" "title" "priority"]
        [["high-bbb" "High task" "P0"]])
      (h/set-project-ready-beads "norm"
        ["id" "title" "priority"]
        [["norm-ccc" "Norm task" "P1"]])
      (h/compute-ready-beads!)
      (should= "high" (:project (nth (h/ready-result) 0)))
      (should= "norm" (:project (nth (h/ready-result) 1)))
      (should= "low" (:project (nth (h/ready-result) 2)))))

  (context "Ready beads skips project paused in config"
    (it "Ready beads skips project paused in config"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:status "paused" :max-workers 1})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task A" "P1"]])
      (h/compute-ready-beads!)
      (should (empty? (h/ready-result)))))

  (context "Format ready output shows numbered list"
    (it "Format ready output shows numbered list"
      (h/reset!)
      (h/set-ready-beads-to-format
        ["project" "id" "title" "priority"]
        [["proj" "proj-abc" "Do stuff" "P0"]])
      (h/format-ready-output!)
      (should (clojure.string/includes? (h/output) "proj-abc"))
      (should (clojure.string/includes? (h/output) "Do stuff"))
      (should (clojure.string/includes? (h/output) "proj"))))

  (context "Format ready output for empty beads"
    (it "Format ready output for empty beads"
      (h/reset!)
      (h/set-no-ready-beads-to-format)
      (h/format-ready-output!)
      (should= "No ready beads." (h/output)))))
