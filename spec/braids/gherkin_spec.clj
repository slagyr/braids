(ns braids.gherkin-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin :as gherkin]))

(describe "Gherkin Parser"

  (context "classify-step"

    (it "classifies project config with worker-timeout"
      (should= {:type :project-config :slug "proj" :worker-timeout 3600}
               (gherkin/classify-step "a project \"proj\" with worker-timeout 3600")))

    (it "classifies session with label"
      (should= {:type :session :session-id "s1" :label "project:proj:proj-abc"}
               (gherkin/classify-step "a session \"s1\" with label \"project:proj:proj-abc\"")))

    (it "classifies session status and age"
      (should= {:type :session-status :session-id "s1" :status "running" :age-seconds 100}
               (gherkin/classify-step "session \"s1\" has status \"running\" and age 100 seconds")))

    (it "classifies bead status"
      (should= {:type :bead-status :bead-id "proj-abc" :status "closed"}
               (gherkin/classify-step "bead \"proj-abc\" has status \"closed\"")))

    (it "classifies bead with no recorded status"
      (should= {:type :bead-no-status :bead-id "proj-mno"}
               (gherkin/classify-step "bead \"proj-mno\" has no recorded status")))

    (it "classifies checking for zombies"
      (should= {:type :check-zombies}
               (gherkin/classify-step "checking for zombies")))

    (it "classifies zombie assertion with reason"
      (should= {:type :assert-zombie :session-id "s1" :reason "bead-closed"}
               (gherkin/classify-step "session \"s1\" should be a zombie with reason \"bead-closed\"")))

    (it "classifies no zombies assertion"
      (should= {:type :assert-no-zombies}
               (gherkin/classify-step "no zombies should be detected")))

    (it "returns unrecognized for unknown step text"
      (should= {:type :unrecognized :text "something totally unknown"}
               (gherkin/classify-step "something totally unknown")))

    ;; --- Orch spawning step patterns ---

    (it "classifies project config with max-workers"
      (should= {:type :project-config :slug "alpha" :max-workers 2}
               (gherkin/classify-step "a project \"alpha\" with max-workers 2")))

    (it "classifies active iteration"
      (should= {:type :active-iteration :slug "alpha" :iteration "003"}
               (gherkin/classify-step "project \"alpha\" has an active iteration \"003\"")))

    (it "classifies no active iteration"
      (should= {:type :no-active-iteration :slug "beta"}
               (gherkin/classify-step "project \"beta\" has no active iteration")))

    (it "classifies ready beads with count"
      (should= {:type :ready-beads :slug "alpha" :count 3}
               (gherkin/classify-step "project \"alpha\" has 3 ready beads")))

    (it "classifies ready bead singular"
      (should= {:type :ready-beads :slug "alpha" :count 1}
               (gherkin/classify-step "project \"alpha\" has 1 ready bead")))

    (it "classifies ready bead with specific id"
      (should= {:type :ready-bead-with-id :slug "alpha" :bead-id "alpha-abc"}
               (gherkin/classify-step "project \"alpha\" has 1 ready bead with id \"alpha-abc\"")))

    (it "classifies active workers"
      (should= {:type :active-workers :slug "alpha" :count 0}
               (gherkin/classify-step "project \"alpha\" has 0 active workers")))

    (it "classifies orch tick"
      (should= {:type :orch-tick}
               (gherkin/classify-step "the orchestrator ticks")))

    (it "classifies orch tick for specific project"
      (should= {:type :orch-tick-project :slug "beta"}
               (gherkin/classify-step "the orchestrator ticks for project \"beta\" only")))

    (it "classifies assert action"
      (should= {:type :assert-action :expected "spawn"}
               (gherkin/classify-step "the action should be \"spawn\"")))

    (it "classifies assert spawn count"
      (should= {:type :assert-spawn-count :count 2}
               (gherkin/classify-step "2 workers should be spawned")))

    (it "classifies assert spawn count singular"
      (should= {:type :assert-spawn-count :count 1}
               (gherkin/classify-step "1 worker should be spawned")))

    (it "classifies assert idle reason"
      (should= {:type :assert-idle-reason :expected "no-ready-beads"}
               (gherkin/classify-step "the idle reason should be \"no-ready-beads\"")))

    (it "classifies assert spawn label"
      (should= {:type :assert-spawn-label :expected "project:alpha:alpha-abc"}
               (gherkin/classify-step "the spawn label should be \"project:alpha:alpha-abc\"")))

    ;; --- Worker session tracking step patterns ---

    (it "classifies bead with id"
      (should= {:type :bead :bead-id "proj-abc"}
               (gherkin/classify-step "a bead with id \"proj-abc\"")))

    (it "classifies another bead with id"
      (should= {:type :bead :bead-id "proj-bbb"}
               (gherkin/classify-step "another bead with id \"proj-bbb\"")))

    (it "classifies session ID literal"
      (should= {:type :session-id-literal :session-id "braids-proj-abc-worker"}
               (gherkin/classify-step "a session ID \"braids-proj-abc-worker\"")))

    (it "classifies generating the session ID"
      (should= {:type :generate-session-id}
               (gherkin/classify-step "generating the session ID")))

    (it "classifies generating the session ID twice"
      (should= {:type :generate-session-id-twice}
               (gherkin/classify-step "generating the session ID twice")))

    (it "classifies generating session IDs for both"
      (should= {:type :generate-session-ids-both}
               (gherkin/classify-step "generating session IDs for both")))

    (it "classifies parsing the session ID"
      (should= {:type :parse-session-id}
               (gherkin/classify-step "parsing the session ID")))

    (it "classifies assert session ID"
      (should= {:type :assert-session-id :expected "braids-proj-abc-worker"}
               (gherkin/classify-step "the session ID should be \"braids-proj-abc-worker\"")))

    (it "classifies assert both session IDs identical"
      (should= {:type :assert-ids-identical}
               (gherkin/classify-step "both session IDs should be identical")))

    (it "classifies assert session IDs different"
      (should= {:type :assert-ids-different}
               (gherkin/classify-step "the session IDs should be different")))

    (it "classifies assert extracted bead ID"
      (should= {:type :assert-bead-id :expected "proj-abc"}
               (gherkin/classify-step "the extracted bead ID should be \"proj-abc\"")))

    ;; --- Project lifecycle step patterns ---

    (it "classifies bd-not-available"
      (should= {:type :bd-not-available}
               (gherkin/classify-step "bd is not available")))

    (it "classifies bd-available"
      (should= {:type :bd-available}
               (gherkin/classify-step "bd is available")))

    (it "classifies no-registry"
      (should= {:type :no-registry}
               (gherkin/classify-step "no registry exists")))

    (it "classifies registry-exists"
      (should= {:type :registry-exists}
               (gherkin/classify-step "a registry already exists")))

    (it "classifies force-not-set"
      (should= {:type :force-not-set}
               (gherkin/classify-step "force is not set")))

    (it "classifies force-set"
      (should= {:type :force-set}
               (gherkin/classify-step "force is set")))

    (it "classifies braids-dir-not-exists"
      (should= {:type :braids-dir-not-exists}
               (gherkin/classify-step "braids dir does not exist")))

    (it "classifies braids-dir-exists"
      (should= {:type :braids-dir-exists}
               (gherkin/classify-step "braids dir already exists")))

    (it "classifies braids-home-not-exists"
      (should= {:type :braids-home-not-exists}
               (gherkin/classify-step "braids home does not exist")))

    (it "classifies braids-home-exists"
      (should= {:type :braids-home-exists}
               (gherkin/classify-step "braids home already exists")))

    (it "classifies checking-prerequisites"
      (should= {:type :check-prerequisites}
               (gherkin/classify-step "checking prerequisites")))

    (it "classifies planning-init"
      (should= {:type :plan-init}
               (gherkin/classify-step "planning init")))

    (it "classifies prereq-fail assertion"
      (should= {:type :assert-prereq-fail :expected "bd (beads) is not installed"}
               (gherkin/classify-step "prerequisites should fail with \"bd (beads) is not installed\"")))

    (it "classifies prereq-pass assertion"
      (should= {:type :assert-prereq-pass}
               (gherkin/classify-step "prerequisites should pass")))

    (it "classifies plan-include assertion"
      (should= {:type :assert-plan-include :action "create-braids-dir"}
               (gherkin/classify-step "the plan should include \"create-braids-dir\"")))

    (it "classifies plan-not-include assertion"
      (should= {:type :assert-plan-not-include :action "create-braids-dir"}
               (gherkin/classify-step "the plan should not include \"create-braids-dir\"")))

    (it "classifies new-project-with-slug"
      (should= {:type :new-project-slug :slug "Bad Slug"}
               (gherkin/classify-step "a new project with slug \"Bad Slug\"")))

    (it "classifies new-project-with-name"
      (should= {:type :new-project-name :name "My Project"}
               (gherkin/classify-step "a new project with name \"My Project\"")))

    (it "classifies name param"
      (should= {:type :set-name :name "My Project"}
               (gherkin/classify-step "name \"My Project\"")))

    (it "classifies goal param"
      (should= {:type :set-goal :goal "Build something"}
               (gherkin/classify-step "goal \"Build something\"")))

    (it "classifies registry-with-project"
      (should= {:type :registry-with-project :slug "my-project"}
               (gherkin/classify-step "a registry with project \"my-project\"")))

    (it "classifies new-registry-entry"
      (should= {:type :new-registry-entry :slug "my-project"}
               (gherkin/classify-step "a new registry entry with slug \"my-project\"")))

    (it "classifies validating-new-project"
      (should= {:type :validate-new-project}
               (gherkin/classify-step "validating new project params")))

    (it "classifies adding-entry-to-registry"
      (should= {:type :add-to-registry}
               (gherkin/classify-step "adding the entry to the registry")))

    (it "classifies building-project-config"
      (should= {:type :build-project-config}
               (gherkin/classify-step "building the project config")))

    (it "classifies validation-fail assertion"
      (should= {:type :assert-validation-fail :expected "Invalid slug"}
               (gherkin/classify-step "validation should fail with \"Invalid slug\"")))

    (it "classifies should-fail-with assertion"
      (should= {:type :assert-should-fail :expected "already exists"}
               (gherkin/classify-step "it should fail with \"already exists\"")))

    (it "classifies config-string-value assertion"
      (should= {:type :assert-config-value :key "status" :expected "active"}
               (gherkin/classify-step "the config status should be \"active\"")))

    (it "classifies config-number-value assertion"
      (should= {:type :assert-config-number :key "max-workers" :expected 1}
               (gherkin/classify-step "the config max-workers should be 1")))

    ;; --- Project listing step patterns ---

    (it "classifies project-list-with-table"
      (should= {:type :project-list-with-table}
               (gherkin/classify-step "a project list with the following projects:")))

    (it "classifies empty-project-list"
      (should= {:type :empty-project-list}
               (gherkin/classify-step "an empty project list")))

    (it "classifies format-list"
      (should= {:type :format-list}
               (gherkin/classify-step "formatting the project list")))

    (it "classifies format-list-json"
      (should= {:type :format-list-json}
               (gherkin/classify-step "formatting the project list as JSON")))

    (it "classifies assert-output-contains-slug"
      (should= {:type :assert-output-contains-slug :slug "alpha"}
               (gherkin/classify-step "the output should contain slug \"alpha\"")))

    (it "classifies assert-dash-placeholder"
      (should= {:type :assert-dash-placeholder :slug "beta" :field "iteration"}
               (gherkin/classify-step "the line for \"beta\" should contain a dash for iteration")))

    (it "classifies assert-output-equals"
      (should= {:type :assert-output-equals :expected "No projects registered."}
               (gherkin/classify-step "the output should be \"No projects registered.\"")))

    (it "classifies assert-status-color"
      (should= {:type :assert-status-color :status "active" :color "green"}
               (gherkin/classify-step "\"active\" status should be colorized green")))

    (it "classifies assert-json-project-exists"
      (should= {:type :assert-json-project-exists :slug "alpha"}
               (gherkin/classify-step "the JSON output should contain a project with slug \"alpha\"")))

    (it "classifies assert-json-project-string"
      (should= {:type :assert-json-project-string :slug "alpha" :key "status" :expected "active"}
               (gherkin/classify-step "the JSON project \"alpha\" should have status \"active\"")))

    (it "classifies assert-json-project-number"
      (should= {:type :assert-json-project-number :slug "alpha" :key "workers" :expected 1}
               (gherkin/classify-step "the JSON project \"alpha\" should have workers 1")))

    (it "classifies assert-json-iteration-number"
      (should= {:type :assert-json-iteration-number :slug "alpha" :number "009"}
               (gherkin/classify-step "the JSON project \"alpha\" should have iteration number \"009\""))))

  (context "parse-feature"

    (it "parses a minimal feature with one scenario"
      (let [text "Feature: Simple feature\n\n  Scenario: Basic test\n    Given a step\n    When another step\n    Then final step"
            result (gherkin/parse-feature text)]
        (should= "Simple feature" (:feature result))
        (should= 1 (count (:scenarios result)))
        (should= "Basic test" (-> result :scenarios first :scenario))
        (should= [{:type :unrecognized :text "a step"}] (-> result :scenarios first :givens))
        (should= [{:type :unrecognized :text "another step"}] (-> result :scenarios first :whens))
        (should= [{:type :unrecognized :text "final step"}] (-> result :scenarios first :thens))))

    (it "strips Given/When/Then keywords from step text"
      (let [text "Feature: Keyword stripping\n\n  Scenario: Strip keywords\n    Given the first step\n    When the second step\n    Then the third step"
            result (gherkin/parse-feature text)]
        (should= [{:type :unrecognized :text "the first step"}] (-> result :scenarios first :givens))
        (should= [{:type :unrecognized :text "the second step"}] (-> result :scenarios first :whens))
        (should= [{:type :unrecognized :text "the third step"}] (-> result :scenarios first :thens))))

    (it "appends And steps to the previous phase"
      (let [text "Feature: And steps\n\n  Scenario: And handling\n    Given first given\n    And second given\n    When first when\n    And second when\n    Then first then\n    And second then"
            result (gherkin/parse-feature text)]
        (should= [{:type :unrecognized :text "first given"}
                  {:type :unrecognized :text "second given"}] (-> result :scenarios first :givens))
        (should= [{:type :unrecognized :text "first when"}
                  {:type :unrecognized :text "second when"}] (-> result :scenarios first :whens))
        (should= [{:type :unrecognized :text "first then"}
                  {:type :unrecognized :text "second then"}] (-> result :scenarios first :thens))))

    (it "appends But steps to the previous phase"
      (let [text "Feature: But steps\n\n  Scenario: But handling\n    Given a condition\n    But not another condition\n    When an action\n    But not another action\n    Then a result\n    But not another result"
            result (gherkin/parse-feature text)]
        (should= [{:type :unrecognized :text "a condition"}
                  {:type :unrecognized :text "not another condition"}] (-> result :scenarios first :givens))
        (should= [{:type :unrecognized :text "an action"}
                  {:type :unrecognized :text "not another action"}] (-> result :scenarios first :whens))
        (should= [{:type :unrecognized :text "a result"}
                  {:type :unrecognized :text "not another result"}] (-> result :scenarios first :thens))))

    (it "parses background givens separately from scenarios"
      (let [text "Feature: Feature with background\n\n  Background:\n    Given a common setup\n    And another common setup\n\n  Scenario: First scenario\n    Given a specific given\n    When something happens\n    Then a result"
            result (gherkin/parse-feature text)]
        (should= {:givens [{:type :unrecognized :text "a common setup"}
                           {:type :unrecognized :text "another common setup"}]}
                 (:background result))
        (should= [{:type :unrecognized :text "a specific given"}] (-> result :scenarios first :givens))
        (should= [{:type :unrecognized :text "something happens"}] (-> result :scenarios first :whens))
        (should= [{:type :unrecognized :text "a result"}] (-> result :scenarios first :thens))))

    (it "does NOT merge background givens into scenarios"
      (let [text "Feature: Background separation\n\n  Background:\n    Given background step\n\n  Scenario: Test scenario\n    Given scenario step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should= {:givens [{:type :unrecognized :text "background step"}]} (:background result))
        (should= [{:type :unrecognized :text "scenario step"}] (-> result :scenarios first :givens))))

    (it "parses multiple scenarios"
      (let [text "Feature: Multiple scenarios\n\n  Scenario: First\n    Given step A\n    When action A\n    Then result A\n\n  Scenario: Second\n    Given step B\n    When action B\n    Then result B"
            result (gherkin/parse-feature text)]
        (should= 2 (count (:scenarios result)))
        (should= "First" (-> result :scenarios first :scenario))
        (should= "Second" (-> result :scenarios second :scenario))))

    (it "preserves feature description text"
      (let [text "Feature: Described feature\n\n  This is the description of the feature.\n  It can span multiple lines.\n\n  Scenario: A test\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should= "This is the description of the feature.\nIt can span multiple lines."
                 (:description result))))

    (it "marks @wip tagged scenarios with :wip true"
      (let [text "Feature: WIP feature\n\n  Scenario: Normal scenario\n    Given a step\n    When action\n    Then result\n\n  @wip\n  Scenario: WIP scenario\n    Given a wip step\n    When wip action\n    Then wip result"
            result (gherkin/parse-feature text)]
        (should-be-nil (:wip (-> result :scenarios first)))
        (should= true (:wip (-> result :scenarios second)))))

    (it "handles scenario with no given steps"
      (let [text "Feature: No givens\n\n  Scenario: When/then only\n    When something happens\n    Then a result"
            result (gherkin/parse-feature text)]
        (should= [] (-> result :scenarios first :givens))
        (should= [{:type :unrecognized :text "something happens"}] (-> result :scenarios first :whens))
        (should= [{:type :unrecognized :text "a result"}] (-> result :scenarios first :thens))))

    (it "has no background key when feature has no background"
      (let [text "Feature: No background\n\n  Scenario: Simple\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should-not-contain :background result)))

    (it "attaches data table to the preceding step"
      (let [text (str "Feature: Table support\n\n"
                      "  Scenario: With table\n"
                      "    Given a list of items:\n"
                      "      | name  | value |\n"
                      "      | alpha | 1     |\n"
                      "      | beta  | 2     |\n"
                      "    When action\n"
                      "    Then result")
            result (gherkin/parse-feature text)
            given (-> result :scenarios first :givens first)]
        (should= {:headers ["name" "value"]
                  :rows [["alpha" "1"] ["beta" "2"]]}
                 (:table given))))

    (it "step without table has no :table key"
      (let [text "Feature: No table\n\n  Scenario: Simple\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)
            given (-> result :scenarios first :givens first)]
        (should-not-contain :table given))))

  (context "parse-feature-file"

    (it "includes source filename in the result"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= "orch_spawning.feature" (:source result))
        (should= "Orchestrator spawning behavior" (:feature result))))

    (it "parses orch_spawning.feature correctly"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= {:givens [{:type :project-config :slug "alpha" :max-workers 2}
                           {:type :active-iteration :slug "alpha" :iteration "003"}]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        (let [first-scenario (first (:scenarios result))]
          (should= "Spawn workers when beads ready and capacity available" (:scenario first-scenario))
          (should= [{:type :ready-beads :slug "alpha" :count 3}
                    {:type :active-workers :slug "alpha" :count 0}]
                   (:givens first-scenario))
          (should= [{:type :orch-tick}] (:whens first-scenario))
          (should= [{:type :assert-action :expected "spawn"}
                    {:type :assert-spawn-count :count 2}]
                   (:thens first-scenario)))))

    (it "parses worker_session_tracking.feature with typed IR and @wip tags"
      (let [result (gherkin/parse-feature-file "features/worker_session_tracking.feature")]
        (should= "Worker session tracking" (:feature result))
        (should-be-nil (:background result))
        (should= 6 (count (:scenarios result)))
        ;; First scenario: Generate deterministic session ID from bead ID
        (let [s (first (:scenarios result))]
          (should= "Generate deterministic session ID from bead ID" (:scenario s))
          (should= [{:type :bead :bead-id "proj-abc"}] (:givens s))
          (should= [{:type :generate-session-id}] (:whens s))
          (should= [{:type :assert-session-id :expected "braids-proj-abc-worker"}] (:thens s)))
        ;; Fourth scenario: Session ID can be parsed back to bead ID
        (let [s (nth (:scenarios result) 3)]
          (should= "Session ID can be parsed back to bead ID" (:scenario s))
          (should= [{:type :session-id-literal :session-id "braids-proj-abc-worker"}] (:givens s))
          (should= [{:type :parse-session-id}] (:whens s))
          (should= [{:type :assert-bead-id :expected "proj-abc"}] (:thens s)))
        ;; The last two scenarios have @wip tags
        (should-be-nil (:wip (nth (:scenarios result) 0)))
        (should-be-nil (:wip (nth (:scenarios result) 3)))
        (should= true (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))))

    (it "parses zombie_detection.feature with background and @wip"
      (let [result (gherkin/parse-feature-file "features/zombie_detection.feature")]
        (should= "Zombie detection" (:feature result))
        (should= {:givens [{:type :project-config :slug "proj" :worker-timeout 3600}]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        ;; First scenario should have typed IR nodes
        (let [first-scenario (first (:scenarios result))]
          (should= [{:type :session :session-id "s1" :label "project:proj:proj-abc"}
                    {:type :session-status :session-id "s1" :status "running" :age-seconds 100}
                    {:type :bead-status :bead-id "proj-abc" :status "closed"}]
                   (:givens first-scenario))
          (should= [{:type :check-zombies}] (:whens first-scenario))
          (should= [{:type :assert-zombie :session-id "s1" :reason "bead-closed"}]
                   (:thens first-scenario)))
        ;; Last two are @wip
        (should-be-nil (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))
        (should= true (:wip (nth (:scenarios result) 6)))))

    (it "preserves feature description text from files"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= "The orchestrator tick examines project state and decides whether\nto spawn workers or remain idle. It respects max-workers capacity,\nrequires active iterations, and reports idle reasons."
                 (:description result)))))

  (context "parse-features-dir"

    (it "parses all .feature files in a directory"
      (let [results (gherkin/parse-features-dir "features")]
        (should= 10 (count results))
        (should (every? :source results))
        (should (every? :feature results))
        (should (every? :scenarios results)))))

  (context "write-edn"

    (it "writes parsed feature to .edn file"
      (let [ir {:source "test.feature"
                :feature "Test"
                :scenarios [{:scenario "S1"
                             :givens [{:type :unrecognized :text "a"}]
                             :whens [{:type :unrecognized :text "b"}]
                             :thens [{:type :unrecognized :text "c"}]}]}
            tmp-file (str "/tmp/test-gherkin-" (System/currentTimeMillis) ".edn")]
        (gherkin/write-edn tmp-file ir)
        (let [content (slurp tmp-file)
              parsed (read-string content)]
          (should= ir parsed))
        (clojure.java.io/delete-file tmp-file true)))))
