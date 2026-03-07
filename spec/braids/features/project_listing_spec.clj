(ns braids.features.project-listing-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Project listing"

  (context "List shows projects with all columns populated"
    (it "List shows projects with all columns populated"
      (h/reset!)
      (h/set-project-list-from-table
        ["slug" "status" "priority" "iteration" "closed" "total" "percent" "workers" "max-workers" "path"]
        [["alpha" "active" "high" "009" "1" "3" "33" "1" "2" "~/Projects/alpha"] ["gamma" "active" "low" "002" "2" "2" "100" "0" "1" "~/Projects/gamma"]])
      (h/format-list!)
      (should (clojure.string/includes? (h/list-output) "SLUG"))
      (should (clojure.string/includes? (h/list-output) "STATUS"))
      (should (clojure.string/includes? (h/list-output) "PRIORITY"))
      (should (clojure.string/includes? (h/list-output) "ITERATION"))
      (should (clojure.string/includes? (h/list-output) "PROGRESS"))
      (should (clojure.string/includes? (h/list-output) "WORKERS"))
      (should (clojure.string/includes? (h/list-output) "PATH"))
      (should (clojure.string/includes? (h/list-output) "alpha"))
      (should (clojure.string/includes? (h/list-output) "gamma"))
      (should (clojure.string/includes? (h/list-output) "009"))
      (should (clojure.string/includes? (h/list-output) "1/3 (33%)"))
      (should (clojure.string/includes? (h/list-output) "2/2 (100%)"))
      (should (clojure.string/includes? (h/list-output) "1/2"))
      (should (clojure.string/includes? (h/list-output) "0/1"))))

  (context "List shows dash placeholders for missing data"
    (it "List shows dash placeholders for missing data"
      (h/reset!)
      (h/set-project-list-from-table
        ["slug" "status" "priority" "iteration" "closed" "total" "percent" "workers" "max-workers" "path"]
        [["beta" "paused" "normal" "" "" "" "" "0" "1" "~/Projects/beta"]])
      (h/format-list!)
      (should (h/line-contains-dash? "beta"))
      (should (h/line-contains-dash? "beta"))))

  (context "List handles empty registry"
    (it "List handles empty registry"
      (h/reset!)
      (h/set-empty-project-list)
      (h/format-list!)
      (should= "No projects registered." (h/output))))

  (context "List colorizes status and priority"
    (it "List colorizes status and priority"
      (h/reset!)
      (h/set-project-list-from-table
        ["slug" "status" "priority" "iteration" "closed" "total" "percent" "workers" "max-workers" "path"]
        [["alpha" "active" "high" "009" "1" "3" "33" "1" "2" "~/Projects/alpha"] ["beta" "paused" "normal" "" "" "" "" "0" "1" "~/Projects/beta"] ["gamma" "active" "low" "002" "2" "2" "100" "0" "1" "~/Projects/gamma"]])
      (h/format-list!)
      (should (h/colorized? (h/list-output) "active" "green"))
      (should (h/colorized? (h/list-output) "paused" "yellow"))
      (should (h/colorized? (h/list-output) "high" "red"))
      (should (h/colorized? (h/list-output) "low" "yellow"))
      (should (h/colorized? (h/list-output) "100%" "green"))))

  (context "List JSON output includes all project data"
    (it "List JSON output includes all project data"
      (h/reset!)
      (h/set-project-list-from-table
        ["slug" "status" "priority" "iteration" "closed" "total" "percent" "workers" "max-workers" "path"]
        [["alpha" "active" "high" "009" "1" "3" "33" "1" "2" "~/Projects/alpha"]])
      (h/format-list-json!)
      (should (h/json-project "alpha"))
      (should= "active" (get (h/json-project "alpha") "status"))
      (should= "high" (get (h/json-project "alpha") "priority"))
      (should= "009" (get-in (h/json-project "alpha") ["iteration" "number"]))
      (should= 1 (get (h/json-project "alpha") "workers"))
      (should= 2 (get (h/json-project "alpha") "max_workers")))))
