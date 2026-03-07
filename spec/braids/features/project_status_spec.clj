(ns braids.features.project-status-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Project status"

  (context "Dashboard includes all projects with enriched data"
    (it "Dashboard includes all projects with enriched data"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"] ["beta" "paused" "normal" "~/Projects/beta"] ["gamma" "active" "low" "~/Projects/gamma"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"] ["beta" "1"] ["gamma" "1"]])
      (h/set-active-iterations-from-table
        ["slug" "number" "total" "closed" "percent"]
        [["alpha" "009" "3" "1" "33"] ["gamma" "002" "2" "2" "100"]])
      (h/set-active-workers-from-table
        ["slug" "count"]
        [["alpha" "1"] ["gamma" "0"]])
      (h/build-dashboard!)
      (should= 3 (count (:projects (h/dashboard))))
      (should= "active" (:status (h/dashboard-project "alpha")))
      (should= "009" (get-in (h/dashboard-project "alpha") [:iteration :number]))
      (should= 1 (:workers (h/dashboard-project "alpha")))
      (should= 2 (:max-workers (h/dashboard-project "alpha")))
      (should= "paused" (:status (h/dashboard-project "beta")))
      (should-be-nil (:iteration (h/dashboard-project "beta")))))

  (context "Dashboard handles missing iterations"
    (it "Dashboard handles missing iterations"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["proj" "active" "normal" "~/Projects/proj"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["proj" "1"]])
      (h/build-dashboard!)
      (should-be-nil (:iteration (h/dashboard-project "proj")))))

  (context "Project detail shows iteration progress and stories"
    (it "Project detail shows iteration progress and stories"
      (h/reset!)
      (h/set-dashboard-project "alpha"
        ["status" "active"]
        [["workers" "1"] ["max-workers" "2"]])
      (h/set-project-iteration "alpha"
        ["number" "009"]
        [["total" "3"] ["closed" "1"] ["percent" "33"]])
      (h/set-project-stories "alpha"
        ["id" "title" "status"]
        [["a-001" "Do thing" "closed"] ["a-002" "Do other" "in_progress"] ["a-003" "Do last" "open"]])
      (h/format-project-detail! "alpha")
      (should (clojure.string/includes? (h/output) "alpha"))
      (should (clojure.string/includes? (h/output) "1/3"))
      (should (clojure.string/includes? (h/output) "33%"))
      (should (clojure.string/includes? (h/output) "a-001"))
      (should (clojure.string/includes? (h/output) "Do thing"))))

  (context "Project detail shows no-iteration fallback"
    (it "Project detail shows no-iteration fallback"
      (h/reset!)
      (h/set-dashboard-project "beta"
        ["status" "paused"]
        [["workers" "0"] ["max-workers" "1"]])
      (h/clear-project-iteration "beta")
      (h/format-project-detail! "beta")
      (should (clojure.string/includes? (h/output) "beta"))
      (should (clojure.string/includes? (h/output) "no active iteration"))))

  (context "Dashboard JSON output includes all project data"
    (it "Dashboard JSON output includes all project data"
      (h/reset!)
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"]])
      (h/set-active-iterations-from-table
        ["slug" "number" "total" "closed" "percent"]
        [["alpha" "009" "3" "1" "33"]])
      (h/set-active-workers-from-table
        ["slug" "count"]
        [["alpha" "1"]])
      (h/build-dashboard!)
      (h/format-dashboard-json!)
      (should= 1 (count (:projects (h/dashboard-json))))
      (should= "active" (get (h/json-project "alpha") "status"))
      (should= 33 (get-in (h/dashboard-json-project "alpha") ["iteration" "stats" "percent"]))))

  (context "Dashboard handles empty registry"
    (it "Dashboard handles empty registry"
      (h/reset!)
      (h/set-empty-registry)
      (h/build-dashboard!)
      (h/format-dashboard!)
      (should= "No projects registered." (h/output)))))
