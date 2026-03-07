(ns braids.gherkin-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin :as gherkin]))

(describe "Gherkin Parser"

  (context "classify-step"

    (it "classifies project config with worker-timeout"
      (should= {:pattern :project-config :slug "proj" :worker-timeout 3600}
               (gherkin/classify-step "a project \"proj\" with worker-timeout 3600")))

    (it "classifies session with label"
      (should= {:pattern :session :session-id "s1" :label "project:proj:proj-abc"}
               (gherkin/classify-step "a session \"s1\" with label \"project:proj:proj-abc\"")))

    (it "classifies session status and age"
      (should= {:pattern :session-status :session-id "s1" :status "running" :age-seconds 100}
               (gherkin/classify-step "session \"s1\" has status \"running\" and age 100 seconds")))

    (it "classifies bead status"
      (should= {:pattern :bead-status :bead-id "proj-abc" :status "closed"}
               (gherkin/classify-step "bead \"proj-abc\" has status \"closed\"")))

    (it "classifies bead with no recorded status"
      (should= {:pattern :bead-no-status :bead-id "proj-mno"}
               (gherkin/classify-step "bead \"proj-mno\" has no recorded status")))

    (it "classifies checking for zombies"
      (should= {:pattern :check-zombies}
               (gherkin/classify-step "checking for zombies")))

    (it "classifies zombie assertion with reason"
      (should= {:pattern :assert-zombie :session-id "s1" :reason "bead-closed"}
               (gherkin/classify-step "session \"s1\" should be a zombie with reason \"bead-closed\"")))

    (it "classifies no zombies assertion"
      (should= {:pattern :assert-no-zombies}
               (gherkin/classify-step "no zombies should be detected")))

    (it "returns unrecognized for unknown step text"
      (should= {:pattern :unrecognized :text "something totally unknown"}
               (gherkin/classify-step "something totally unknown")))

    ;; --- Orch spawning step patterns ---

    (it "classifies project config with max-workers"
      (should= {:pattern :project-config :slug "alpha" :max-workers 2}
               (gherkin/classify-step "a project \"alpha\" with max-workers 2")))

    (it "classifies active iteration"
      (should= {:pattern :active-iteration :slug "alpha" :iteration "003"}
               (gherkin/classify-step "project \"alpha\" has an active iteration \"003\"")))

    (it "classifies no active iteration"
      (should= {:pattern :no-active-iteration :slug "beta"}
               (gherkin/classify-step "project \"beta\" has no active iteration")))

    (it "classifies ready beads with count"
      (should= {:pattern :ready-beads :slug "alpha" :count 3}
               (gherkin/classify-step "project \"alpha\" has 3 ready beads")))

    (it "classifies ready bead singular"
      (should= {:pattern :ready-beads :slug "alpha" :count 1}
               (gherkin/classify-step "project \"alpha\" has 1 ready bead")))

    (it "classifies ready bead with specific id"
      (should= {:pattern :ready-bead-with-id :slug "alpha" :bead-id "alpha-abc"}
               (gherkin/classify-step "project \"alpha\" has 1 ready bead with id \"alpha-abc\"")))

    (it "classifies active workers"
      (should= {:pattern :active-workers :slug "alpha" :count 0}
               (gherkin/classify-step "project \"alpha\" has 0 active workers")))

    (it "classifies orch tick"
      (should= {:pattern :orch-tick}
               (gherkin/classify-step "the orchestrator ticks")))

    (it "classifies orch tick for specific project"
      (should= {:pattern :orch-tick-project :slug "beta"}
               (gherkin/classify-step "the orchestrator ticks for project \"beta\" only")))

    (it "classifies assert action"
      (should= {:pattern :assert-action :expected "spawn"}
               (gherkin/classify-step "the action should be \"spawn\"")))

    (it "classifies assert spawn count"
      (should= {:pattern :assert-spawn-count :count 2}
               (gherkin/classify-step "2 workers should be spawned")))

    (it "classifies assert spawn count singular"
      (should= {:pattern :assert-spawn-count :count 1}
               (gherkin/classify-step "1 worker should be spawned")))

    (it "classifies assert idle reason"
      (should= {:pattern :assert-idle-reason :expected "no-ready-beads"}
               (gherkin/classify-step "the idle reason should be \"no-ready-beads\"")))

    (it "classifies assert spawn label"
      (should= {:pattern :assert-spawn-label :expected "project:alpha:alpha-abc"}
               (gherkin/classify-step "the spawn label should be \"project:alpha:alpha-abc\"")))

    ;; --- Worker session tracking step patterns ---

    (it "classifies bead with id"
      (should= {:pattern :bead :bead-id "proj-abc"}
               (gherkin/classify-step "a bead with id \"proj-abc\"")))

    (it "classifies another bead with id"
      (should= {:pattern :bead :bead-id "proj-bbb"}
               (gherkin/classify-step "another bead with id \"proj-bbb\"")))

    (it "classifies session ID literal"
      (should= {:pattern :session-id-literal :session-id "braids-proj-abc-worker"}
               (gherkin/classify-step "a session ID \"braids-proj-abc-worker\"")))

    (it "classifies generating the session ID"
      (should= {:pattern :generate-session-id}
               (gherkin/classify-step "generating the session ID")))

    (it "classifies generating the session ID twice"
      (should= {:pattern :generate-session-id-twice}
               (gherkin/classify-step "generating the session ID twice")))

    (it "classifies generating session IDs for both"
      (should= {:pattern :generate-session-ids-both}
               (gherkin/classify-step "generating session IDs for both")))

    (it "classifies parsing the session ID"
      (should= {:pattern :parse-session-id}
               (gherkin/classify-step "parsing the session ID")))

    (it "classifies assert session ID"
      (should= {:pattern :assert-session-id :expected "braids-proj-abc-worker"}
               (gherkin/classify-step "the session ID should be \"braids-proj-abc-worker\"")))

    (it "classifies assert both session IDs identical"
      (should= {:pattern :assert-ids-identical}
               (gherkin/classify-step "both session IDs should be identical")))

    (it "classifies assert session IDs different"
      (should= {:pattern :assert-ids-different}
               (gherkin/classify-step "the session IDs should be different")))

    (it "classifies assert extracted bead ID"
      (should= {:pattern :assert-bead-id :expected "proj-abc"}
               (gherkin/classify-step "the extracted bead ID should be \"proj-abc\"")))

    ;; --- Project lifecycle step patterns ---

    (it "classifies bd-not-available"
      (should= {:pattern :bd-not-available}
               (gherkin/classify-step "bd is not available")))

    (it "classifies bd-available"
      (should= {:pattern :bd-available}
               (gherkin/classify-step "bd is available")))

    (it "classifies no-registry"
      (should= {:pattern :no-registry}
               (gherkin/classify-step "no registry exists")))

    (it "classifies registry-exists"
      (should= {:pattern :registry-exists}
               (gherkin/classify-step "a registry already exists")))

    (it "classifies force-not-set"
      (should= {:pattern :force-not-set}
               (gherkin/classify-step "force is not set")))

    (it "classifies force-set"
      (should= {:pattern :force-set}
               (gherkin/classify-step "force is set")))

    (it "classifies braids-dir-not-exists"
      (should= {:pattern :braids-dir-not-exists}
               (gherkin/classify-step "braids dir does not exist")))

    (it "classifies braids-dir-exists"
      (should= {:pattern :braids-dir-exists}
               (gherkin/classify-step "braids dir already exists")))

    (it "classifies braids-home-not-exists"
      (should= {:pattern :braids-home-not-exists}
               (gherkin/classify-step "braids home does not exist")))

    (it "classifies braids-home-exists"
      (should= {:pattern :braids-home-exists}
               (gherkin/classify-step "braids home already exists")))

    (it "classifies checking-prerequisites"
      (should= {:pattern :check-prerequisites}
               (gherkin/classify-step "checking prerequisites")))

    (it "classifies planning-init"
      (should= {:pattern :plan-init}
               (gherkin/classify-step "planning init")))

    (it "classifies prereq-fail assertion"
      (should= {:pattern :assert-prereq-fail :expected "bd (beads) is not installed"}
               (gherkin/classify-step "prerequisites should fail with \"bd (beads) is not installed\"")))

    (it "classifies prereq-pass assertion"
      (should= {:pattern :assert-prereq-pass}
               (gherkin/classify-step "prerequisites should pass")))

    (it "classifies plan-include assertion"
      (should= {:pattern :assert-plan-include :action "create-braids-dir"}
               (gherkin/classify-step "the plan should include \"create-braids-dir\"")))

    (it "classifies plan-not-include assertion"
      (should= {:pattern :assert-plan-not-include :action "create-braids-dir"}
               (gherkin/classify-step "the plan should not include \"create-braids-dir\"")))

    (it "classifies new-project-with-slug"
      (should= {:pattern :new-project-slug :slug "Bad Slug"}
               (gherkin/classify-step "a new project with slug \"Bad Slug\"")))

    (it "classifies new-project-with-name"
      (should= {:pattern :new-project-name :name "My Project"}
               (gherkin/classify-step "a new project with name \"My Project\"")))

    (it "classifies name param"
      (should= {:pattern :set-name :name "My Project"}
               (gherkin/classify-step "name \"My Project\"")))

    (it "classifies goal param"
      (should= {:pattern :set-goal :goal "Build something"}
               (gherkin/classify-step "goal \"Build something\"")))

    (it "classifies registry-with-project"
      (should= {:pattern :registry-with-project :slug "my-project"}
               (gherkin/classify-step "a registry with project \"my-project\"")))

    (it "classifies new-registry-entry"
      (should= {:pattern :new-registry-entry :slug "my-project"}
               (gherkin/classify-step "a new registry entry with slug \"my-project\"")))

    (it "classifies validating-new-project"
      (should= {:pattern :validate-new-project}
               (gherkin/classify-step "validating new project params")))

    (it "classifies adding-entry-to-registry"
      (should= {:pattern :add-to-registry}
               (gherkin/classify-step "adding the entry to the registry")))

    (it "classifies building-project-config"
      (should= {:pattern :build-project-config}
               (gherkin/classify-step "building the project config")))

    (it "classifies validation-fail assertion"
      (should= {:pattern :assert-validation-fail :expected "Invalid slug"}
               (gherkin/classify-step "validation should fail with \"Invalid slug\"")))

    (it "classifies should-fail-with assertion"
      (should= {:pattern :assert-should-fail :expected "already exists"}
               (gherkin/classify-step "it should fail with \"already exists\"")))

    (it "classifies config-string-value assertion"
      (should= {:pattern :assert-config-value :key "status" :expected "active"}
               (gherkin/classify-step "the config status should be \"active\"")))

    (it "classifies config-number-value assertion"
      (should= {:pattern :assert-config-number :key "max-workers" :expected 1}
               (gherkin/classify-step "the config max-workers should be 1")))

    ;; --- Ready beads step patterns ---

    (it "classifies registry-with-projects-table"
      (should= {:pattern :registry-with-projects-table}
               (gherkin/classify-step "a registry with projects:")))

    (it "classifies project-config-max-workers"
      (should= {:pattern :project-config-max-workers :slug "alpha" :max-workers 1}
               (gherkin/classify-step "project \"alpha\" has config with max-workers 1")))

    (it "classifies project-config-status-and-max-workers"
      (should= {:pattern :project-config-status-and-max-workers :slug "proj" :status "paused" :max-workers 1}
               (gherkin/classify-step "project \"proj\" has config with status \"paused\" and max-workers 1")))

    (it "classifies project-ready-beads-table"
      (should= {:pattern :project-ready-beads-table :slug "alpha"}
               (gherkin/classify-step "project \"alpha\" has ready beads:")))

    (it "classifies no-active-workers"
      (should= {:pattern :no-active-workers}
               (gherkin/classify-step "no active workers")))

    (it "classifies compute-ready-beads"
      (should= {:pattern :compute-ready-beads}
               (gherkin/classify-step "computing ready beads")))

    (it "classifies assert-result-contains-bead"
      (should= {:pattern :assert-result-contains-bead :bead-id "alpha-aaa"}
               (gherkin/classify-step "the result should contain bead \"alpha-aaa\"")))

    (it "classifies assert-result-not-contains-bead"
      (should= {:pattern :assert-result-not-contains-bead :bead-id "beta-bbb"}
               (gherkin/classify-step "the result should not contain bead \"beta-bbb\"")))

    (it "classifies assert-result-empty"
      (should= {:pattern :assert-result-empty}
               (gherkin/classify-step "the result should be empty")))

    (it "classifies assert-first-result-project"
      (should= {:pattern :assert-nth-result-project :position 1 :slug "high"}
               (gherkin/classify-step "the first result should be from project \"high\"")))

    (it "classifies assert-second-result-project"
      (should= {:pattern :assert-nth-result-project :position 2 :slug "norm"}
               (gherkin/classify-step "the second result should be from project \"norm\"")))

    (it "classifies assert-third-result-project"
      (should= {:pattern :assert-nth-result-project :position 3 :slug "low"}
               (gherkin/classify-step "the third result should be from project \"low\"")))

    (it "classifies ready-beads-to-format"
      (should= {:pattern :ready-beads-to-format}
               (gherkin/classify-step "ready beads to format:")))

    (it "classifies no-ready-beads-to-format"
      (should= {:pattern :no-ready-beads-to-format}
               (gherkin/classify-step "no ready beads to format")))

    (it "classifies format-ready-output"
      (should= {:pattern :format-ready-output}
               (gherkin/classify-step "formatting ready output")))

    (it "classifies assert-output-contains"
      (should= {:pattern :assert-output-contains :expected "proj-abc"}
               (gherkin/classify-step "the output should contain \"proj-abc\"")))

    ;; --- Project listing step patterns ---

    (it "classifies project-list-with-table"
      (should= {:pattern :project-list-with-table}
               (gherkin/classify-step "a project list with the following projects:")))

    (it "classifies empty-project-list"
      (should= {:pattern :empty-project-list}
               (gherkin/classify-step "an empty project list")))

    (it "classifies format-list"
      (should= {:pattern :format-list}
               (gherkin/classify-step "formatting the project list")))

    (it "classifies format-list-json"
      (should= {:pattern :format-list-json}
               (gherkin/classify-step "formatting the project list as JSON")))

    (it "classifies assert-output-contains-slug"
      (should= {:pattern :assert-output-contains-slug :slug "alpha"}
               (gherkin/classify-step "the output should contain slug \"alpha\"")))

    (it "classifies assert-dash-placeholder"
      (should= {:pattern :assert-dash-placeholder :slug "beta" :field "iteration"}
               (gherkin/classify-step "the line for \"beta\" should contain a dash for iteration")))

    (it "classifies assert-output-equals"
      (should= {:pattern :assert-output-equals :expected "No projects registered."}
               (gherkin/classify-step "the output should be \"No projects registered.\"")))

    (it "classifies assert-status-color"
      (should= {:pattern :assert-status-color :status "active" :color "green"}
               (gherkin/classify-step "\"active\" status should be colorized green")))

    (it "classifies assert-json-project-exists"
      (should= {:pattern :assert-json-project-exists :slug "alpha"}
               (gherkin/classify-step "the JSON output should contain a project with slug \"alpha\"")))

    (it "classifies assert-json-project-string"
      (should= {:pattern :assert-json-project-string :slug "alpha" :key "status" :expected "active"}
               (gherkin/classify-step "the JSON project \"alpha\" should have status \"active\"")))

    (it "classifies assert-json-project-number"
      (should= {:pattern :assert-json-project-number :slug "alpha" :key "workers" :expected 1}
               (gherkin/classify-step "the JSON project \"alpha\" should have workers 1")))

    (it "classifies assert-json-iteration-number"
      (should= {:pattern :assert-json-iteration-number :slug "alpha" :number "009"}
               (gherkin/classify-step "the JSON project \"alpha\" should have iteration number \"009\"")))

    ;; --- Iteration management step patterns ---

    (it "classifies iteration-edn-with-stories"
      (should= {:pattern :iteration-edn :number "003" :status "active" :story-count 1}
               (gherkin/classify-step "iteration EDN with number \"003\" and status \"active\" and 1 story")))

    (it "classifies edn-no-guardrails-or-notes"
      (should= {:pattern :edn-no-guardrails-or-notes}
               (gherkin/classify-step "the EDN has no guardrails or notes")))

    (it "classifies iteration-with-status-and-stories"
      (should= {:pattern :iteration-with-status :number "001" :status "bogus"}
               (gherkin/classify-step "an iteration with number \"001\" and status \"bogus\" and stories")))

    (it "classifies iteration-with-no-number"
      (should= {:pattern :iteration-no-number}
               (gherkin/classify-step "an iteration with no number")))

    (it "classifies iteration-with-two-stories"
      (should= {:pattern :iteration-with-stories :story-ids ["proj-abc" "proj-def"]}
               (gherkin/classify-step "an iteration with stories \"proj-abc\" and \"proj-def\"")))

    (it "classifies iteration-with-single-story"
      (should= {:pattern :iteration-with-story :story-id "proj-xyz"}
               (gherkin/classify-step "an iteration with story \"proj-xyz\"")))

    (it "classifies bead-status-with-priority"
      (should= {:pattern :iter-bead-status :bead-id "proj-abc" :status "open" :priority 1}
               (gherkin/classify-step "bead \"proj-abc\" has status \"open\" and priority 1")))

    (it "classifies no-bead-data-exists"
      (should= {:pattern :no-bead-data}
               (gherkin/classify-step "no bead data exists")))

    (it "classifies annotated-stories-with-counts"
      (should= {:pattern :annotated-stories :closed 2 :open 2 :total 4}
               (gherkin/classify-step "annotated stories with 2 closed and 2 open out of 4 total")))

    (it "classifies iteration-with-no-stories"
      (should= {:pattern :iteration-no-stories}
               (gherkin/classify-step "an iteration with no stories")))

    (it "classifies iteration-number-with-status"
      (should= {:pattern :iteration-number-status :number "009" :status "active"}
               (gherkin/classify-step "an iteration \"009\" with status \"active\"")))

    (it "classifies story-with-status"
      (should= {:pattern :story-with-status :story-id "proj-abc" :status "open"}
               (gherkin/classify-step "a story \"proj-abc\" with status \"open\"")))

    (it "classifies completion-stats"
      (should= {:pattern :completion-stats :closed 1 :total 2}
               (gherkin/classify-step "completion stats of 1 closed out of 2")))

    (it "classifies parsing-iteration-edn"
      (should= {:pattern :parse-iteration-edn}
               (gherkin/classify-step "parsing the iteration EDN")))

    (it "classifies validating-iteration"
      (should= {:pattern :validate-iteration}
               (gherkin/classify-step "validating the iteration")))

    (it "classifies annotating-stories"
      (should= {:pattern :annotate-stories}
               (gherkin/classify-step "annotating stories with bead data")))

    (it "classifies calculating-completion-stats"
      (should= {:pattern :calculate-completion-stats}
               (gherkin/classify-step "calculating completion stats")))

    (it "classifies formatting-iteration"
      (should= {:pattern :format-iteration}
               (gherkin/classify-step "formatting the iteration")))

    (it "classifies formatting-iteration-json"
      (should= {:pattern :format-iteration-json}
               (gherkin/classify-step "formatting the iteration as JSON")))

    (it "classifies assert-iteration-number"
      (should= {:pattern :assert-iteration-number :expected "003"}
               (gherkin/classify-step "the iteration number should be \"003\"")))

    (it "classifies assert-iteration-status"
      (should= {:pattern :assert-iteration-status :expected "active"}
               (gherkin/classify-step "the iteration status should be \"active\"")))

    (it "classifies assert-iteration-guardrails-empty"
      (should= {:pattern :assert-iteration-guardrails-empty}
               (gherkin/classify-step "the iteration guardrails should be empty")))

    (it "classifies assert-iteration-notes-empty"
      (should= {:pattern :assert-iteration-notes-empty}
               (gherkin/classify-step "the iteration notes should be empty")))

    (it "classifies assert-story-status"
      (should= {:pattern :assert-story-status :story-id "proj-abc" :expected "open"}
               (gherkin/classify-step "story \"proj-abc\" should have status \"open\"")))

    (it "classifies assert-total"
      (should= {:pattern :assert-total :expected 4}
               (gherkin/classify-step "the total should be 4")))

    (it "classifies assert-closed-count"
      (should= {:pattern :assert-closed-count :expected 2}
               (gherkin/classify-step "the closed count should be 2")))

    (it "classifies assert-completion-percent"
      (should= {:pattern :assert-completion-percent :expected 50}
               (gherkin/classify-step "the completion percent should be 50")))

    (it "classifies assert-json-contains"
      (should= {:pattern :assert-json-contains :expected "number"}
               (gherkin/classify-step "the JSON should contain \"number\"")))

    ;; --- Project status step patterns ---

    (it "classifies project-configs-table"
      (should= {:pattern :project-configs-table}
               (gherkin/classify-step "project configs:")))

    (it "classifies active-iterations-table"
      (should= {:pattern :active-iterations-table}
               (gherkin/classify-step "active iterations:")))

    (it "classifies active-workers-table"
      (should= {:pattern :active-workers-table}
               (gherkin/classify-step "active workers:")))

    (it "classifies no-active-iterations"
      (should= {:pattern :no-active-iterations}
               (gherkin/classify-step "no active iterations")))

    (it "classifies no-active-workers for status feature"
      (should= {:pattern :no-active-workers}
               (gherkin/classify-step "no active workers")))

    (it "classifies building-the-dashboard"
      (should= {:pattern :build-dashboard}
               (gherkin/classify-step "building the dashboard")))

    (it "classifies assert-dashboard-project-count"
      (should= {:pattern :assert-dashboard-project-count :count 3}
               (gherkin/classify-step "the dashboard should have 3 projects")))

    (it "classifies assert-project-status"
      (should= {:pattern :assert-project-status :slug "alpha" :expected "active"}
               (gherkin/classify-step "project \"alpha\" should have status \"active\"")))

    (it "classifies assert-project-iteration-number"
      (should= {:pattern :assert-project-iteration-number :slug "alpha" :expected "009"}
               (gherkin/classify-step "project \"alpha\" should have iteration number \"009\"")))

    (it "classifies assert-project-workers"
      (should= {:pattern :assert-project-workers :slug "alpha" :workers 1 :max-workers 2}
               (gherkin/classify-step "project \"alpha\" should have workers 1 of 2")))

    (it "classifies assert-project-no-iteration"
      (should= {:pattern :assert-project-no-iteration :slug "beta"}
               (gherkin/classify-step "project \"beta\" should have no iteration")))

    (it "classifies dashboard-project-with-table"
      (should= {:pattern :dashboard-project :slug "alpha"}
               (gherkin/classify-step "a dashboard project \"alpha\" with:")))

    (it "classifies project-has-iteration-table"
      (should= {:pattern :project-has-iteration :slug "alpha"}
               (gherkin/classify-step "project \"alpha\" has iteration:")))

    (it "classifies project-has-stories-table"
      (should= {:pattern :project-has-stories :slug "alpha"}
               (gherkin/classify-step "project \"alpha\" has stories:")))

    (it "classifies project-has-no-iteration"
      (should= {:pattern :project-has-no-iteration :slug "beta"}
               (gherkin/classify-step "project \"beta\" has no iteration")))

    (it "classifies formatting-project-detail"
      (should= {:pattern :format-project-detail :slug "alpha"}
               (gherkin/classify-step "formatting project detail for \"alpha\"")))

    (it "classifies formatting-dashboard-json"
      (should= {:pattern :format-dashboard-json}
               (gherkin/classify-step "formatting the dashboard as JSON")))

    (it "classifies formatting-dashboard"
      (should= {:pattern :format-dashboard}
               (gherkin/classify-step "formatting the dashboard")))

    (it "classifies assert-json-project-count"
      (should= {:pattern :assert-json-project-count :count 1}
               (gherkin/classify-step "the JSON should contain 1 project")))

    (it "classifies assert-json-project-iteration-percent"
      (should= {:pattern :assert-json-project-iteration-percent :slug "alpha" :percent 33}
               (gherkin/classify-step "the JSON project \"alpha\" should have iteration percent 33")))

    (it "classifies empty-registry"
      (should= {:pattern :empty-registry}
               (gherkin/classify-step "an empty registry"))))

  (context "parse-feature"

    (it "parses a minimal feature with one scenario"
      (let [text "Feature: Simple feature\n\n  Scenario: Basic test\n    Given a step\n    When another step\n    Then final step"
            result (gherkin/parse-feature text)]
        (should= "Simple feature" (:feature result))
        (should= 1 (count (:scenarios result)))
        (should= "Basic test" (-> result :scenarios first :scenario))
        (should= [{:type :given :pattern :unrecognized :text "a step"}
                  {:type :when  :pattern :unrecognized :text "another step"}
                  {:type :then  :pattern :unrecognized :text "final step"}]
                 (-> result :scenarios first :steps))))

    (it "strips Given/When/Then keywords from step text"
      (let [text "Feature: Keyword stripping\n\n  Scenario: Strip keywords\n    Given the first step\n    When the second step\n    Then the third step"
            result (gherkin/parse-feature text)]
        (should= [{:type :given :pattern :unrecognized :text "the first step"}
                  {:type :when  :pattern :unrecognized :text "the second step"}
                  {:type :then  :pattern :unrecognized :text "the third step"}]
                 (-> result :scenarios first :steps))))

    (it "appends And steps to the previous phase"
      (let [text "Feature: And steps\n\n  Scenario: And handling\n    Given first given\n    And second given\n    When first when\n    And second when\n    Then first then\n    And second then"
            result (gherkin/parse-feature text)]
        (should= [{:type :given :pattern :unrecognized :text "first given"}
                  {:type :and   :pattern :unrecognized :text "second given"}
                  {:type :when  :pattern :unrecognized :text "first when"}
                  {:type :and   :pattern :unrecognized :text "second when"}
                  {:type :then  :pattern :unrecognized :text "first then"}
                  {:type :and   :pattern :unrecognized :text "second then"}]
                 (-> result :scenarios first :steps))))

    (it "appends But steps to the previous phase"
      (let [text "Feature: But steps\n\n  Scenario: But handling\n    Given a condition\n    But not another condition\n    When an action\n    But not another action\n    Then a result\n    But not another result"
            result (gherkin/parse-feature text)]
        (should= [{:type :given :pattern :unrecognized :text "a condition"}
                  {:type :but   :pattern :unrecognized :text "not another condition"}
                  {:type :when  :pattern :unrecognized :text "an action"}
                  {:type :but   :pattern :unrecognized :text "not another action"}
                  {:type :then  :pattern :unrecognized :text "a result"}
                  {:type :but   :pattern :unrecognized :text "not another result"}]
                 (-> result :scenarios first :steps))))

    (it "parses background givens separately from scenarios"
      (let [text "Feature: Feature with background\n\n  Background:\n    Given a common setup\n    And another common setup\n\n  Scenario: First scenario\n    Given a specific given\n    When something happens\n    Then a result"
            result (gherkin/parse-feature text)]
        (should= {:steps [{:type :given :pattern :unrecognized :text "a common setup"}
                          {:type :and   :pattern :unrecognized :text "another common setup"}]}
                 (:background result))
        (should= [{:type :given :pattern :unrecognized :text "a specific given"}
                  {:type :when  :pattern :unrecognized :text "something happens"}
                  {:type :then  :pattern :unrecognized :text "a result"}]
                 (-> result :scenarios first :steps))))

    (it "does NOT merge background givens into scenarios"
      (let [text "Feature: Background separation\n\n  Background:\n    Given background step\n\n  Scenario: Test scenario\n    Given scenario step\n    When action\n    Then result"
            result (gherkin/parse-feature text)]
        (should= {:steps [{:type :given :pattern :unrecognized :text "background step"}]} (:background result))
        (should= [{:type :given :pattern :unrecognized :text "scenario step"}
                  {:type :when  :pattern :unrecognized :text "action"}
                  {:type :then  :pattern :unrecognized :text "result"}]
                 (-> result :scenarios first :steps))))

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
        (should= [{:type :when :pattern :unrecognized :text "something happens"}
                  {:type :then :pattern :unrecognized :text "a result"}]
                 (-> result :scenarios first :steps))))

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
            first-step (-> result :scenarios first :steps first)]
        (should= {:headers ["name" "value"]
                  :rows [["alpha" "1"] ["beta" "2"]]}
                 (:table first-step))))

    (it "step without table has no :table key"
      (let [text "Feature: No table\n\n  Scenario: Simple\n    Given a step\n    When action\n    Then result"
            result (gherkin/parse-feature text)
            first-step (-> result :scenarios first :steps first)]
        (should-not-contain :table first-step))))

  (context "parse-feature-file"

    (it "includes source filename in the result"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= "orch_spawning.feature" (:source result))
        (should= "Orchestrator spawning behavior" (:feature result))))

    (it "parses orch_spawning.feature correctly"
      (let [result (gherkin/parse-feature-file "features/orch_spawning.feature")]
        (should= {:steps [{:type :given :pattern :project-config :slug "alpha" :max-workers 2}
                          {:type :and   :pattern :active-iteration :slug "alpha" :iteration "003"}]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        (let [first-scenario (first (:scenarios result))]
          (should= "Spawn workers when beads ready and capacity available" (:scenario first-scenario))
          (should= [{:type :given :pattern :ready-beads :slug "alpha" :count 3}
                    {:type :and   :pattern :active-workers :slug "alpha" :count 0}]
                   (take 2 (:steps first-scenario)))
          (should= {:type :when :pattern :orch-tick}
                   (nth (:steps first-scenario) 2))
          (should= [{:type :then :pattern :assert-action :expected "spawn"}
                    {:type :and  :pattern :assert-spawn-count :count 2}]
                   (drop 3 (:steps first-scenario))))))

    (it "parses worker_session_tracking.feature with typed IR and @wip tags"
      (let [result (gherkin/parse-feature-file "features/worker_session_tracking.feature")]
        (should= "Worker session tracking" (:feature result))
        (should-be-nil (:background result))
        (should= 6 (count (:scenarios result)))
        ;; First scenario: Generate deterministic session ID from bead ID
        (let [s (first (:scenarios result))]
          (should= "Generate deterministic session ID from bead ID" (:scenario s))
          (should= [{:type :given :pattern :bead :bead-id "proj-abc"}
                    {:type :when  :pattern :generate-session-id}
                    {:type :then  :pattern :assert-session-id :expected "braids-proj-abc-worker"}]
                   (:steps s)))
        ;; Fourth scenario: Session ID can be parsed back to bead ID
        (let [s (nth (:scenarios result) 3)]
          (should= "Session ID can be parsed back to bead ID" (:scenario s))
          (should= [{:type :given :pattern :session-id-literal :session-id "braids-proj-abc-worker"}
                    {:type :when  :pattern :parse-session-id}
                    {:type :then  :pattern :assert-bead-id :expected "proj-abc"}]
                   (:steps s)))
        ;; The last two scenarios have @wip tags
        (should-be-nil (:wip (nth (:scenarios result) 0)))
        (should-be-nil (:wip (nth (:scenarios result) 3)))
        (should= true (:wip (nth (:scenarios result) 4)))
        (should= true (:wip (nth (:scenarios result) 5)))))

    (it "parses zombie_detection.feature with background and @wip"
      (let [result (gherkin/parse-feature-file "features/zombie_detection.feature")]
        (should= "Zombie detection" (:feature result))
        (should= {:steps [{:type :given :pattern :project-config :slug "proj" :worker-timeout 3600}]}
                 (:background result))
        (should= 7 (count (:scenarios result)))
        ;; First scenario should have typed IR nodes
        (let [first-scenario (first (:scenarios result))]
          (should= [{:type :given :pattern :session :session-id "s1" :label "project:proj:proj-abc"}
                    {:type :and   :pattern :session-status :session-id "s1" :status "running" :age-seconds 100}
                    {:type :and   :pattern :bead-status :bead-id "proj-abc" :status "closed"}]
                   (take 3 (:steps first-scenario)))
          (should= {:type :when :pattern :check-zombies}
                   (nth (:steps first-scenario) 3))
          (should= [{:type :then :pattern :assert-zombie :session-id "s1" :reason "bead-closed"}]
                   (drop 4 (:steps first-scenario))))
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
                             :steps [{:type :given :pattern :unrecognized :text "a"}
                                     {:type :when  :pattern :unrecognized :text "b"}
                                     {:type :then  :pattern :unrecognized :text "c"}]}]}
            tmp-file (str "/tmp/test-gherkin-" (System/currentTimeMillis) ".edn")]
        (gherkin/write-edn tmp-file ir)
        (let [content (slurp tmp-file)
              parsed (read-string content)]
          (should= ir parsed))
        (clojure.java.io/delete-file tmp-file true)))))
