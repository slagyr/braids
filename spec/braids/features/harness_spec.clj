(ns braids.features.harness-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Feature harness"

  (before (h/reset!))

  (context "state management"

    (it "starts with empty state after reset"
      (should= {} (h/sessions))
      (should= {} (h/configs))
      (should= {} (h/bead-statuses))
      (should= [] (h/zombies))))

  (context "add-project-config"

    (it "adds a project config with worker-timeout"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (should= {:worker-timeout 3600} (get (h/configs) "proj"))))

  (context "add-session"

    (it "adds a session with label"
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (should= {:label "project:proj:proj-abc"} (get (h/sessions) "s1"))))

  (context "set-session-status"

    (it "sets session status and age"
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (let [session (get (h/sessions) "s1")]
        (should= "running" (:status session))
        (should= 100 (:age-seconds session)))))

  (context "set-bead-status"

    (it "sets bead status"
      (h/set-bead-status "proj-abc" "closed")
      (should= "closed" (get (h/bead-statuses) "proj-abc"))))

  (context "check-zombies!"

    (it "detects zombie when bead is closed"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should= 1 (count (h/zombies))))

    (it "stores empty zombies when none detected"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "open")
      (h/check-zombies!)
      (should= [] (h/zombies))))

  (context "zombie?"

    (it "returns true when session is a zombie"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should (h/zombie? "s1")))

    (it "returns false when session is not a zombie"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "open")
      (h/check-zombies!)
      (should-not (h/zombie? "s1"))))

  (context "zombie-reason"

    (it "returns the zombie reason for a session"
      (h/add-project-config "proj" {:worker-timeout 3600})
      (h/add-session "s1" {:label "project:proj:proj-abc"})
      (h/set-session-status "s1" "running" 100)
      (h/set-bead-status "proj-abc" "closed")
      (h/check-zombies!)
      (should= "bead-closed" (h/zombie-reason "s1"))))

  ;; --- Orch spawning harness ---

  (context "add-project"

    (it "adds a project to registry with active status and sets config"
      (h/add-project "alpha" {:max-workers 2})
      (should= "spawn" (do
                          (h/set-active-iteration "alpha" "003")
                          (h/set-ready-beads "alpha" 1)
                          (h/set-active-workers "alpha" 0)
                          (h/orch-tick!)
                          (h/tick-action)))))

  (context "set-active-iteration"

    (it "sets an active iteration for a project"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))))

  (context "remove-iteration"

    (it "removes iteration for a project"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/remove-iteration "alpha")
      (h/orch-tick!)
      (should= "idle" (h/tick-action))))

  (context "set-ready-beads"

    (it "sets ready beads count for a project"
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 3)
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= 2 (h/spawn-count))))

  (context "set-ready-bead-with-id"

    (it "sets a specific ready bead by id"
      (h/add-project "alpha" {:max-workers 2})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-bead-with-id "alpha" "alpha-abc")
      (h/set-active-workers "alpha" 0)
      (h/orch-tick!)
      (should= "project:alpha:alpha-abc" (h/spawn-label))))

  (context "set-active-workers"

    (it "sets the active worker count"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/set-active-workers "alpha" 1)
      (h/orch-tick!)
      (should= "idle" (h/tick-action))
      (should= "all-at-capacity" (h/idle-reason))))

  (context "orch-tick!"

    (it "runs orch/tick with accumulated state"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 1)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action))))

  (context "orch-tick-project!"

    (it "runs orch/tick for a single project only"
      (h/add-project "alpha" {:max-workers 1})
      (h/set-active-iteration "alpha" "003")
      (h/set-ready-beads "alpha" 1)
      (h/add-project "beta" {:max-workers 1})
      ;; beta has no iteration — should be idle
      (h/orch-tick-project! "beta")
      (should= "idle" (h/tick-action))
      (should= "no-active-iterations" (h/idle-reason))))

  (context "result accessors"

    (it "tick-action returns the action from tick result"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 1)
      (h/orch-tick!)
      (should= "spawn" (h/tick-action)))

    (it "spawn-count returns number of spawns"
      (h/add-project "proj" {:max-workers 2})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 3)
      (h/orch-tick!)
      (should= 2 (h/spawn-count)))

    (it "idle-reason returns the idle reason"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-beads "proj" 0)
      (h/orch-tick!)
      (should= "no-ready-beads" (h/idle-reason)))

    (it "spawn-label returns the first spawn label"
      (h/add-project "proj" {:max-workers 1})
      (h/set-active-iteration "proj" "001")
      (h/set-ready-bead-with-id "proj" "proj-abc")
      (h/orch-tick!)
      (should= "project:proj:proj-abc" (h/spawn-label))))

  ;; --- Worker session tracking harness ---

  (context "set-bead-id"

    (it "stores a bead id for session generation"
      (h/set-bead-id "proj-abc")
      (h/generate-session-id!)
      (should= "braids-proj-abc-worker" (h/session-id-result))))

  (context "set-session-id-literal"

    (it "stores a literal session ID string"
      (h/set-session-id-literal "braids-proj-abc-worker")
      (h/parse-session-id!)
      (should= "proj-abc" (h/parsed-bead-id))))

  (context "generate-session-id!"

    (it "generates session ID from stored bead id"
      (h/set-bead-id "proj-xyz")
      (h/generate-session-id!)
      (should= "braids-proj-xyz-worker" (h/session-id-result))))

  (context "generate-session-id-twice!"

    (it "generates session ID twice and stores both results"
      (h/set-bead-id "proj-xyz")
      (h/generate-session-id-twice!)
      (should (h/session-ids-identical?))))

  (context "generate-session-ids-both!"

    (it "generates session IDs for two bead ids"
      (h/set-bead-id "proj-aaa")
      (h/set-bead-id "proj-bbb")
      (h/generate-session-ids-both!)
      (should (h/session-ids-different?))))

  (context "parse-session-id!"

    (it "parses stored session ID to extract bead id"
      (h/set-session-id-literal "braids-proj-abc-worker")
      (h/parse-session-id!)
      (should= "proj-abc" (h/parsed-bead-id))))

  (context "session tracking result accessors"

    (it "session-id-result returns the generated session ID"
      (h/set-bead-id "proj-abc")
      (h/generate-session-id!)
      (should= "braids-proj-abc-worker" (h/session-id-result)))

    (it "session-ids-identical? returns true for same bead"
      (h/set-bead-id "proj-xyz")
      (h/generate-session-id-twice!)
      (should (h/session-ids-identical?)))

    (it "session-ids-different? returns true for different beads"
      (h/set-bead-id "proj-aaa")
      (h/set-bead-id "proj-bbb")
      (h/generate-session-ids-both!)
      (should (h/session-ids-different?)))

    (it "parsed-bead-id returns the extracted bead id"
      (h/set-session-id-literal "braids-proj-abc-worker")
      (h/parse-session-id!)
      (should= "proj-abc" (h/parsed-bead-id))))

  ;; --- Ready beads harness ---

  (context "set-registry-from-table"

    (it "builds registry from table headers and rows"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["alpha" "active" "normal"]
         ["beta" "paused" "normal"]])
      (h/set-project-config "alpha" {:max-workers 1})
      (h/set-project-ready-beads "alpha"
        ["id" "title" "priority"]
        [["alpha-aaa" "Task A" "P1"]])
      (h/compute-ready-beads!)
      (should (h/result-contains-bead? "alpha-aaa"))))

  (context "set-project-config"

    (it "sets config for a project"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:max-workers 1})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task" "P1"]])
      (h/compute-ready-beads!)
      (should (h/result-contains-bead? "proj-abc")))

    (it "respects paused status in config"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:status "paused" :max-workers 1})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task" "P1"]])
      (h/compute-ready-beads!)
      (should (empty? (h/ready-result)))))

  (context "set-project-ready-beads"

    (it "builds bead list from table data"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:max-workers 2})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task A" "P0"]
         ["proj-def" "Task B" "P1"]])
      (h/compute-ready-beads!)
      (should= 2 (count (h/ready-result)))))

  (context "compute-ready-beads!"

    (it "filters to active projects only"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["alpha" "active" "normal"]
         ["beta" "paused" "normal"]])
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
      (should-not (h/result-contains-bead? "beta-bbb")))

    (it "respects worker capacity"
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

  (context "result-contains-bead?"

    (it "returns true when bead is in result"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:max-workers 1})
      (h/set-project-ready-beads "proj"
        ["id" "title" "priority"]
        [["proj-abc" "Task" "P1"]])
      (h/compute-ready-beads!)
      (should (h/result-contains-bead? "proj-abc")))

    (it "returns false when bead is not in result"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["proj" "active" "normal"]])
      (h/set-project-config "proj" {:max-workers 1})
      (h/compute-ready-beads!)
      (should-not (h/result-contains-bead? "proj-abc"))))

  (context "ready-result ordering"

    (it "orders by project priority"
      (h/set-registry-from-table
        ["slug" "status" "priority"]
        [["low" "active" "low"]
         ["high" "active" "high"]
         ["norm" "active" "normal"]])
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

  (context "set-ready-beads-to-format"

    (it "builds beads for formatting from table data"
      (h/set-ready-beads-to-format
        ["project" "id" "title" "priority"]
        [["proj" "proj-abc" "Do stuff" "P0"]])
      (h/format-ready-output!)
      (should (clojure.string/includes? (h/ready-output) "proj-abc"))
      (should (clojure.string/includes? (h/ready-output) "Do stuff"))))

  (context "set-no-ready-beads-to-format"

    (it "sets empty beads for formatting"
      (h/set-no-ready-beads-to-format)
      (h/format-ready-output!)
      (should= "No ready beads." (h/ready-output))))

  (context "format-ready-output!"

    (it "formats beads using ready/format-ready-output"
      (h/set-ready-beads-to-format
        ["project" "id" "title" "priority"]
        [["proj" "proj-abc" "Do stuff" "P0"]])
      (h/format-ready-output!)
      (should (clojure.string/includes? (h/ready-output) "proj"))))

  (context "ready-output"

    (it "returns the formatted output"
      (h/set-no-ready-beads-to-format)
      (h/format-ready-output!)
      (should= "No ready beads." (h/ready-output))))

  ;; --- Iteration management harness ---

  (context "set-iteration-edn"

    (it "builds iteration EDN string and stores it"
      (h/set-iteration-edn "003" "active" 1)
      (h/parse-iteration-edn!)
      (should= "003" (h/iteration-number))))

  (context "parse-iteration-edn!"

    (it "parses stored EDN and stores the result"
      (h/set-iteration-edn "003" "active" 1)
      (h/parse-iteration-edn!)
      (should= "003" (h/iteration-number))
      (should= "active" (h/iteration-status))
      (should= [] (h/iteration-guardrails))
      (should= [] (h/iteration-notes))))

  (context "set-iteration-with-status"

    (it "stores an iteration map with status for validation"
      (h/set-iteration-with-status "001" "bogus")
      (h/validate-iteration!)
      (should-not (empty? (h/validation-errors)))
      (should (some #(clojure.string/includes? % "Invalid status") (h/validation-errors)))))

  (context "set-iteration-no-number"

    (it "stores an iteration with no number for validation"
      (h/set-iteration-no-number)
      (h/validate-iteration!)
      (should-not (empty? (h/validation-errors)))
      (should (some #(clojure.string/includes? % "Missing :number") (h/validation-errors)))))

  (context "set-iteration-stories"

    (it "stores story ids for annotation"
      (h/set-iteration-stories ["proj-abc" "proj-def"])
      (h/add-iter-bead "proj-abc" "open" 1)
      (h/add-iter-bead "proj-def" "closed" 2)
      (h/annotate-stories!)
      (should= "open" (h/story-status "proj-abc"))
      (should= "closed" (h/story-status "proj-def"))))

  (context "annotate-stories!"

    (it "annotates with unknown when no bead data"
      (h/set-iteration-stories ["proj-xyz"])
      (h/annotate-stories!)
      (should= "unknown" (h/story-status "proj-xyz"))))

  (context "set-annotated-stories"

    (it "builds annotated stories for completion stats"
      (h/set-annotated-stories 2 2 4)
      (h/calculate-completion-stats!)
      (should= 4 (h/stats-total))
      (should= 2 (h/stats-closed))
      (should= 50 (h/stats-percent))))

  (context "calculate-completion-stats!"

    (it "handles empty stories"
      (h/set-annotated-stories 0 0 0)
      (h/calculate-completion-stats!)
      (should= 0 (h/stats-total))
      (should= 0 (h/stats-closed))
      (should= 0 (h/stats-percent))))

  (context "format-iteration!"

    (it "formats iteration for human display"
      (h/set-iteration-number-status "009" "active")
      (h/add-story-with-status "proj-abc" "open")
      (h/add-story-with-status "proj-def" "closed")
      (h/set-completion-stats 1 2)
      (h/format-iteration!)
      (should (clojure.string/includes? (h/output) "Iteration 009"))
      (should (clojure.string/includes? (h/output) "active"))
      (should (clojure.string/includes? (h/output) "50%"))))

  (context "format-iteration-json!"

    (it "formats iteration as JSON"
      (h/set-iteration-number-status "001" "active")
      (h/add-story-with-status "a" "open")
      (h/set-completion-stats 0 1)
      (h/format-iteration-json!)
      (should (clojure.string/includes? (h/iter-json-output) "number"))
      (should (clojure.string/includes? (h/iter-json-output) "stories"))
      (should (clojure.string/includes? (h/iter-json-output) "percent"))))
  (context "configuration helpers"

    (it "set-config-from-table builds config map from table data"
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "/custom/path"]
         ["bd-bin" "bd"]])
      (should= "/custom/path" (str (get (h/current-config) :braids-home)))
      (should= "bd" (str (get (h/current-config) :bd-bin))))

    (it "list-config! formats config and stores output"
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "~/Projects"]
         ["bd-bin" "bd"]])
      (h/list-config!)
      (should (clojure.string/includes? (h/output) "braids-home = ~/Projects"))
      (should (clojure.string/includes? (h/output) "bd-bin = bd")))

    (it "get-config-key! returns ok for existing key"
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "/custom/path"]])
      (h/get-config-key! "braids-home")
      (should= "/custom/path" (:ok (h/config-result))))

    (it "get-config-key! returns error for missing key"
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "~/Projects"]])
      (h/get-config-key! "nonexistent")
      (should (:error (h/config-result)))
      (should (clojure.string/includes? (:error (h/config-result)) "nonexistent")))

    (it "set-config-key! updates config value"
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "~/Projects"]])
      (h/set-config-key! "braids-home" "/new/path")
      (should= "/new/path" (str (get (h/current-config) :braids-home))))

    (it "set-empty-config sets empty config string"
      (h/set-empty-config)
      (h/parse-config!)
      (should= "~/Projects" (str (get (h/current-config) :braids-home))))

    (it "request-config-help! stores help output"
      (h/request-config-help!)
      (should (clojure.string/includes? (h/output) "Usage: braids config"))))

  ;; --- Project status harness ---

  (context "set-project-configs-from-table"

    (it "builds project configs from table data"
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"]])
      (h/build-dashboard!)
      (should= 2 (:max-workers (h/dashboard-project "alpha")))))

  (context "set-active-iterations-from-table"

    (it "builds iterations from table data"
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"]])
      (h/set-active-iterations-from-table
        ["slug" "number" "total" "closed" "percent"]
        [["alpha" "009" "3" "1" "33"]])
      (h/build-dashboard!)
      (should= "009" (get-in (h/dashboard-project "alpha") [:iteration :number]))))

  (context "set-active-workers-from-table"

    (it "builds workers from table data"
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"]])
      (h/set-active-workers-from-table
        ["slug" "count"]
        [["alpha" "1"]])
      (h/build-dashboard!)
      (should= 1 (:workers (h/dashboard-project "alpha")))))

  (context "build-dashboard!"

    (it "builds dashboard from accumulated state"
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"]
         ["beta" "paused" "normal" "~/Projects/beta"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"] ["beta" "1"]])
      (h/set-active-iterations-from-table
        ["slug" "number" "total" "closed" "percent"]
        [["alpha" "009" "3" "1" "33"]])
      (h/set-active-workers-from-table
        ["slug" "count"]
        [["alpha" "1"]])
      (h/build-dashboard!)
      (should= 2 (count (:projects (h/dashboard))))
      (should= "active" (:status (h/dashboard-project "alpha")))
      (should= "paused" (:status (h/dashboard-project "beta")))
      (should-be-nil (:iteration (h/dashboard-project "beta")))))

  (context "dashboard-project"

    (it "finds a project by slug"
      (h/set-registry-from-table
        ["slug" "status" "priority" "path"]
        [["alpha" "active" "high" "~/Projects/alpha"]])
      (h/set-project-configs-from-table
        ["slug" "max-workers"]
        [["alpha" "2"]])
      (h/build-dashboard!)
      (should= "alpha" (:slug (h/dashboard-project "alpha")))))

  (context "set-dashboard-project"

    (it "builds a project for detail formatting"
      (h/set-dashboard-project "alpha"
        ["status" "active"]
        [["workers" "1"] ["max-workers" "2"]])
      (h/set-project-iteration "alpha"
        ["number" "009"]
        [["total" "3"] ["closed" "1"] ["percent" "33"]])
      (h/set-project-stories "alpha"
        ["id" "title" "status"]
        [["a-001" "Do thing" "closed"]])
      (h/format-project-detail! "alpha")
      (should (clojure.string/includes? (h/output) "alpha"))))

  (context "format-project-detail!"

    (it "formats project detail with iteration"
      (h/set-dashboard-project "alpha"
        ["status" "active"]
        [["workers" "1"] ["max-workers" "2"]])
      (h/set-project-iteration "alpha"
        ["number" "009"]
        [["total" "3"] ["closed" "1"] ["percent" "33"]])
      (h/set-project-stories "alpha"
        ["id" "title" "status"]
        [["a-001" "Do thing" "closed"]])
      (h/format-project-detail! "alpha")
      (should (clojure.string/includes? (h/output) "1/3"))
      (should (clojure.string/includes? (h/output) "33%")))

    (it "formats project detail without iteration"
      (h/set-dashboard-project "beta"
        ["status" "paused"]
        [["workers" "0"] ["max-workers" "1"]])
      (h/clear-project-iteration "beta")
      (h/format-project-detail! "beta")
      (should (clojure.string/includes? (h/output) "no active iteration"))))

  (context "format-dashboard!"

    (it "formats dashboard for human output"
      (h/set-empty-registry)
      (h/build-dashboard!)
      (h/format-dashboard!)
      (should= "No projects registered." (h/output))))

  (context "format-dashboard-json!"

    (it "formats dashboard as JSON"
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
      (should= "active" (get (h/dashboard-json-project "alpha") "status"))))

  (context "set-empty-registry"

    (it "sets an empty registry"
      (h/set-empty-registry)
      (h/build-dashboard!)
      (should= 0 (count (:projects (h/dashboard))))))
)