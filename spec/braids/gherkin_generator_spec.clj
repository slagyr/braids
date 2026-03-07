(ns braids.gherkin-generator-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin-generator :as gen]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(describe "Gherkin Generator"

  (context "source->ns-name"

    (it "converts feature filename to spec namespace"
      (should= "braids.features.orch-spawning-spec"
               (gen/source->ns-name "orch_spawning.feature")))

    (it "handles hyphens in filenames"
      (should= "braids.features.worker-session-tracking-spec"
               (gen/source->ns-name "worker_session_tracking.feature")))

    (it "handles .edn extension too"
      (should= "braids.features.zombie-detection-spec"
               (gen/source->ns-name "zombie_detection.edn"))))

  (context "generate-ns-form"

    (it "generates a valid ns declaration"
      (let [ns-form (gen/generate-ns-form "orch_spawning.feature")]
        (should-contain "(ns braids.features.orch-spawning-spec" ns-form)
        (should-contain "[speclj.core :refer :all]" ns-form))))

  (context "step-text"

    (it "returns :text for unrecognized steps"
      (should= "a step" (gen/step-text {:pattern :unrecognized :text "a step"})))

    (it "formats project-config step"
      (should= "a project \"proj\" with worker-timeout 3600"
               (gen/step-text {:pattern :project-config :slug "proj" :worker-timeout 3600})))

    (it "formats session step"
      (should= "a session \"s1\" with label \"project:proj:proj-abc\""
               (gen/step-text {:pattern :session :session-id "s1" :label "project:proj:proj-abc"})))

    (it "formats session-status step"
      (should= "session \"s1\" has status \"running\" and age 100 seconds"
               (gen/step-text {:pattern :session-status :session-id "s1" :status "running" :age-seconds 100})))

    (it "formats check-zombies step"
      (should= "checking for zombies"
               (gen/step-text {:pattern :check-zombies})))

    (it "formats assert-zombie step"
      (should= "session \"s1\" should be a zombie with reason \"bead-closed\""
               (gen/step-text {:pattern :assert-zombie :session-id "s1" :reason "bead-closed"})))

    (it "formats assert-no-zombies step"
      (should= "no zombies should be detected"
               (gen/step-text {:pattern :assert-no-zombies})))

    ;; --- Orch spawning step-text ---

    (it "formats project-config with max-workers step"
      (should= "a project \"alpha\" with max-workers 2"
               (gen/step-text {:pattern :project-config :slug "alpha" :max-workers 2})))

    (it "formats active-iteration step"
      (should= "project \"alpha\" has an active iteration \"003\""
               (gen/step-text {:pattern :active-iteration :slug "alpha" :iteration "003"})))

    (it "formats no-active-iteration step"
      (should= "project \"beta\" has no active iteration"
               (gen/step-text {:pattern :no-active-iteration :slug "beta"})))

    (it "formats ready-beads step"
      (should= "project \"alpha\" has 3 ready beads"
               (gen/step-text {:pattern :ready-beads :slug "alpha" :count 3})))

    (it "formats ready-bead-with-id step"
      (should= "project \"alpha\" has 1 ready bead with id \"alpha-abc\""
               (gen/step-text {:pattern :ready-bead-with-id :slug "alpha" :bead-id "alpha-abc"})))

    (it "formats active-workers step"
      (should= "project \"alpha\" has 0 active workers"
               (gen/step-text {:pattern :active-workers :slug "alpha" :count 0})))

    (it "formats orch-tick step"
      (should= "the orchestrator ticks"
               (gen/step-text {:pattern :orch-tick})))

    (it "formats orch-tick-project step"
      (should= "the orchestrator ticks for project \"beta\" only"
               (gen/step-text {:pattern :orch-tick-project :slug "beta"})))

    (it "formats assert-action step"
      (should= "the action should be \"spawn\""
               (gen/step-text {:pattern :assert-action :expected "spawn"})))

    (it "formats assert-spawn-count step"
      (should= "2 workers should be spawned"
               (gen/step-text {:pattern :assert-spawn-count :count 2})))

    (it "formats assert-idle-reason step"
      (should= "the idle reason should be \"no-ready-beads\""
               (gen/step-text {:pattern :assert-idle-reason :expected "no-ready-beads"})))

    (it "formats assert-spawn-label step"
      (should= "the spawn label should be \"project:alpha:alpha-abc\""
               (gen/step-text {:pattern :assert-spawn-label :expected "project:alpha:alpha-abc"})))

    ;; --- Worker session tracking step-text ---

    (it "formats bead step"
      (should= "a bead with id \"proj-abc\""
               (gen/step-text {:pattern :bead :bead-id "proj-abc"})))

    (it "formats session-id-literal step"
      (should= "a session ID \"braids-proj-abc-worker\""
               (gen/step-text {:pattern :session-id-literal :session-id "braids-proj-abc-worker"})))

    (it "formats generate-session-id step"
      (should= "generating the session ID"
               (gen/step-text {:pattern :generate-session-id})))

    (it "formats generate-session-id-twice step"
      (should= "generating the session ID twice"
               (gen/step-text {:pattern :generate-session-id-twice})))

    (it "formats generate-session-ids-both step"
      (should= "generating session IDs for both"
               (gen/step-text {:pattern :generate-session-ids-both})))

    (it "formats parse-session-id step"
      (should= "parsing the session ID"
               (gen/step-text {:pattern :parse-session-id})))

    (it "formats assert-session-id step"
      (should= "the session ID should be \"braids-proj-abc-worker\""
               (gen/step-text {:pattern :assert-session-id :expected "braids-proj-abc-worker"})))

    (it "formats assert-ids-identical step"
      (should= "both session IDs should be identical"
               (gen/step-text {:pattern :assert-ids-identical})))

    (it "formats assert-ids-different step"
      (should= "the session IDs should be different"
               (gen/step-text {:pattern :assert-ids-different})))

    (it "formats assert-bead-id step"
      (should= "the extracted bead ID should be \"proj-abc\""
               (gen/step-text {:pattern :assert-bead-id :expected "proj-abc"})))

    ;; --- Ready beads step-text ---

    (it "formats registry-with-projects-table step"
      (should= "a registry with projects:"
               (gen/step-text {:pattern :registry-with-projects-table})))

    (it "formats project-config-max-workers step"
      (should= "project \"alpha\" has config with max-workers 1"
               (gen/step-text {:pattern :project-config-max-workers :slug "alpha" :max-workers 1})))

    (it "formats project-config-status-and-max-workers step"
      (should= "project \"proj\" has config with status \"paused\" and max-workers 1"
               (gen/step-text {:pattern :project-config-status-and-max-workers :slug "proj" :status "paused" :max-workers 1})))

    (it "formats project-ready-beads-table step"
      (should= "project \"alpha\" has ready beads:"
               (gen/step-text {:pattern :project-ready-beads-table :slug "alpha"})))

    (it "formats no-active-workers step"
      (should= "no active workers"
               (gen/step-text {:pattern :no-active-workers})))

    (it "formats compute-ready-beads step"
      (should= "computing ready beads"
               (gen/step-text {:pattern :compute-ready-beads})))

    (it "formats assert-result-contains-bead step"
      (should= "the result should contain bead \"alpha-aaa\""
               (gen/step-text {:pattern :assert-result-contains-bead :bead-id "alpha-aaa"})))

    (it "formats assert-result-not-contains-bead step"
      (should= "the result should not contain bead \"beta-bbb\""
               (gen/step-text {:pattern :assert-result-not-contains-bead :bead-id "beta-bbb"})))

    (it "formats assert-result-empty step"
      (should= "the result should be empty"
               (gen/step-text {:pattern :assert-result-empty})))

    (it "formats assert-nth-result-project step for first"
      (should= "the first result should be from project \"high\""
               (gen/step-text {:pattern :assert-nth-result-project :position 1 :slug "high"})))

    (it "formats assert-nth-result-project step for second"
      (should= "the second result should be from project \"norm\""
               (gen/step-text {:pattern :assert-nth-result-project :position 2 :slug "norm"})))

    (it "formats assert-nth-result-project step for third"
      (should= "the third result should be from project \"low\""
               (gen/step-text {:pattern :assert-nth-result-project :position 3 :slug "low"})))

    (it "formats ready-beads-to-format step"
      (should= "ready beads to format:"
               (gen/step-text {:pattern :ready-beads-to-format})))

    (it "formats no-ready-beads-to-format step"
      (should= "no ready beads to format"
               (gen/step-text {:pattern :no-ready-beads-to-format})))

    (it "formats format-ready-output step"
      (should= "formatting ready output"
               (gen/step-text {:pattern :format-ready-output})))

    (it "formats assert-output-contains step"
      (should= "the output should contain \"proj-abc\""
               (gen/step-text {:pattern :assert-output-contains :expected "proj-abc"})))

    ;; --- Iteration management step-text ---

    (it "formats iteration-edn step"
      (should= "iteration EDN with number \"003\" and status \"active\" and 1 story"
               (gen/step-text {:pattern :iteration-edn :number "003" :status "active" :story-count 1})))

    (it "formats edn-no-guardrails-or-notes step"
      (should= "the EDN has no guardrails or notes"
               (gen/step-text {:pattern :edn-no-guardrails-or-notes})))

    (it "formats iteration-with-status step"
      (should= "an iteration with number \"001\" and status \"bogus\" and stories"
               (gen/step-text {:pattern :iteration-with-status :number "001" :status "bogus"})))

    (it "formats iteration-no-number step"
      (should= "an iteration with no number"
               (gen/step-text {:pattern :iteration-no-number})))

    (it "formats iteration-with-stories step"
      (should= "an iteration with stories \"proj-abc\" and \"proj-def\""
               (gen/step-text {:pattern :iteration-with-stories :story-ids ["proj-abc" "proj-def"]})))

    (it "formats iteration-with-story step"
      (should= "an iteration with story \"proj-xyz\""
               (gen/step-text {:pattern :iteration-with-story :story-id "proj-xyz"})))

    (it "formats iter-bead-status step"
      (should= "bead \"proj-abc\" has status \"open\" and priority 1"
               (gen/step-text {:pattern :iter-bead-status :bead-id "proj-abc" :status "open" :priority 1})))

    (it "formats no-bead-data step"
      (should= "no bead data exists"
               (gen/step-text {:pattern :no-bead-data})))

    (it "formats annotated-stories step"
      (should= "annotated stories with 2 closed and 2 open out of 4 total"
               (gen/step-text {:pattern :annotated-stories :closed 2 :open 2 :total 4})))

    (it "formats iteration-no-stories step"
      (should= "an iteration with no stories"
               (gen/step-text {:pattern :iteration-no-stories})))

    (it "formats iteration-number-status step"
      (should= "an iteration \"009\" with status \"active\""
               (gen/step-text {:pattern :iteration-number-status :number "009" :status "active"})))

    (it "formats story-with-status step"
      (should= "a story \"proj-abc\" with status \"open\""
               (gen/step-text {:pattern :story-with-status :story-id "proj-abc" :status "open"})))

    (it "formats completion-stats step"
      (should= "completion stats of 1 closed out of 2"
               (gen/step-text {:pattern :completion-stats :closed 1 :total 2})))

    (it "formats parse-iteration-edn step"
      (should= "parsing the iteration EDN"
               (gen/step-text {:pattern :parse-iteration-edn})))

    (it "formats validate-iteration step"
      (should= "validating the iteration"
               (gen/step-text {:pattern :validate-iteration})))

    (it "formats annotate-stories step"
      (should= "annotating stories with bead data"
               (gen/step-text {:pattern :annotate-stories})))

    (it "formats calculate-completion-stats step"
      (should= "calculating completion stats"
               (gen/step-text {:pattern :calculate-completion-stats})))

    (it "formats format-iteration step"
      (should= "formatting the iteration"
               (gen/step-text {:pattern :format-iteration})))

    (it "formats format-iteration-json step"
      (should= "formatting the iteration as JSON"
               (gen/step-text {:pattern :format-iteration-json})))

    (it "formats assert-iteration-number step"
      (should= "the iteration number should be \"003\""
               (gen/step-text {:pattern :assert-iteration-number :expected "003"})))

    (it "formats assert-iteration-status step"
      (should= "the iteration status should be \"active\""
               (gen/step-text {:pattern :assert-iteration-status :expected "active"})))

    (it "formats assert-iteration-guardrails-empty step"
      (should= "the iteration guardrails should be empty"
               (gen/step-text {:pattern :assert-iteration-guardrails-empty})))

    (it "formats assert-iteration-notes-empty step"
      (should= "the iteration notes should be empty"
               (gen/step-text {:pattern :assert-iteration-notes-empty})))

    (it "formats assert-story-status step"
      (should= "story \"proj-abc\" should have status \"open\""
               (gen/step-text {:pattern :assert-story-status :story-id "proj-abc" :expected "open"})))

    (it "formats assert-total step"
      (should= "the total should be 4"
               (gen/step-text {:pattern :assert-total :expected 4})))

    (it "formats assert-closed-count step"
      (should= "the closed count should be 2"
               (gen/step-text {:pattern :assert-closed-count :expected 2})))

    (it "formats assert-completion-percent step"
      (should= "the completion percent should be 50"
               (gen/step-text {:pattern :assert-completion-percent :expected 50})))

    (it "formats assert-json-contains step"
      (should= "the JSON should contain \"number\""
               (gen/step-text {:pattern :assert-json-contains :expected "number"})))

    ;; --- Project status step-text ---

    (it "formats project-configs-table step"
      (should= "project configs:"
               (gen/step-text {:pattern :project-configs-table})))

    (it "formats active-iterations-table step"
      (should= "active iterations:"
               (gen/step-text {:pattern :active-iterations-table})))

    (it "formats active-workers-table step"
      (should= "active workers:"
               (gen/step-text {:pattern :active-workers-table})))

    (it "formats no-active-iterations step"
      (should= "no active iterations"
               (gen/step-text {:pattern :no-active-iterations})))

    (it "formats build-dashboard step"
      (should= "building the dashboard"
               (gen/step-text {:pattern :build-dashboard})))

    (it "formats assert-dashboard-project-count step"
      (should= "the dashboard should have 3 projects"
               (gen/step-text {:pattern :assert-dashboard-project-count :count 3})))

    (it "formats assert-project-status step"
      (should= "project \"alpha\" should have status \"active\""
               (gen/step-text {:pattern :assert-project-status :slug "alpha" :expected "active"})))

    (it "formats assert-project-iteration-number step"
      (should= "project \"alpha\" should have iteration number \"009\""
               (gen/step-text {:pattern :assert-project-iteration-number :slug "alpha" :expected "009"})))

    (it "formats assert-project-workers step"
      (should= "project \"alpha\" should have workers 1 of 2"
               (gen/step-text {:pattern :assert-project-workers :slug "alpha" :workers 1 :max-workers 2})))

    (it "formats assert-project-no-iteration step"
      (should= "project \"beta\" should have no iteration"
               (gen/step-text {:pattern :assert-project-no-iteration :slug "beta"})))

    (it "formats dashboard-project step"
      (should= "a dashboard project \"alpha\" with:"
               (gen/step-text {:pattern :dashboard-project :slug "alpha"})))

    (it "formats project-has-iteration step"
      (should= "project \"alpha\" has iteration:"
               (gen/step-text {:pattern :project-has-iteration :slug "alpha"})))

    (it "formats project-has-stories step"
      (should= "project \"alpha\" has stories:"
               (gen/step-text {:pattern :project-has-stories :slug "alpha"})))

    (it "formats project-has-no-iteration step"
      (should= "project \"beta\" has no iteration"
               (gen/step-text {:pattern :project-has-no-iteration :slug "beta"})))

    (it "formats format-project-detail step"
      (should= "formatting project detail for \"alpha\""
               (gen/step-text {:pattern :format-project-detail :slug "alpha"})))

    (it "formats format-dashboard-json step"
      (should= "formatting the dashboard as JSON"
               (gen/step-text {:pattern :format-dashboard-json})))

    (it "formats format-dashboard step"
      (should= "formatting the dashboard"
               (gen/step-text {:pattern :format-dashboard})))

    (it "formats assert-json-project-count step"
      (should= "the JSON should contain 1 project"
               (gen/step-text {:pattern :assert-json-project-count :count 1})))

    (it "formats assert-json-project-iteration-percent step"
      (should= "the JSON project \"alpha\" should have iteration percent 33"
               (gen/step-text {:pattern :assert-json-project-iteration-percent :slug "alpha" :percent 33})))

    (it "formats empty-registry step"
      (should= "an empty registry"
               (gen/step-text {:pattern :empty-registry})))

    ;; --- Orch runner step-text ---

    (it "formats spawn-entry-path-bead step"
      (should= "a spawn entry with path \"~/Projects/test\" and bead \"test-abc\""
               (gen/step-text {:pattern :spawn-entry-path-bead :path "~/Projects/test" :bead "test-abc"})))

    (it "formats spawn-iteration-channel step"
      (should= "iteration \"001\" and channel \"12345\""
               (gen/step-text {:pattern :spawn-iteration-channel :iteration "001" :channel "12345"})))

    (it "formats spawn-entry-bead step"
      (should= "a spawn entry with bead \"proj-abc\""
               (gen/step-text {:pattern :spawn-entry-bead :bead "proj-abc"})))

    (it "formats no-worker-agent step"
      (should= "no custom worker agent"
               (gen/step-text {:pattern :no-worker-agent})))

    (it "formats worker-agent step"
      (should= "worker agent \"scrapper\""
               (gen/step-text {:pattern :worker-agent :agent "scrapper"})))

    (it "formats no-cli-args step"
      (should= "no CLI arguments"
               (gen/step-text {:pattern :no-cli-args})))

    (it "formats cli-args step"
      (should= "CLI arguments \"--confirmed\""
               (gen/step-text {:pattern :cli-args :args "--confirmed"})))

    (it "formats spawn-tick-result step"
      (should= "a spawn tick result with 2 workers"
               (gen/step-text {:pattern :spawn-tick-result :count 2})))

    (it "formats spawn-beads step"
      (should= "beads \"b1\" and \"b2\""
               (gen/step-text {:pattern :spawn-beads :beads ["b1" "b2"]})))

    (it "formats idle-tick-result step"
      (should= "an idle tick result with reason \"all-at-capacity\""
               (gen/step-text {:pattern :idle-tick-result :reason "all-at-capacity"})))

    (it "formats zombie-sessions step"
      (should= "2 zombie sessions with reasons \"bead-closed\" and \"timeout\""
               (gen/step-text {:pattern :zombie-sessions :count 2 :reasons ["bead-closed" "timeout"]})))

    (it "formats build-worker-task step"
      (should= "building the worker task"
               (gen/step-text {:pattern :build-worker-task})))

    (it "formats build-worker-args step"
      (should= "building the worker args"
               (gen/step-text {:pattern :build-worker-args})))

    (it "formats parse-cli-args step"
      (should= "parsing CLI args"
               (gen/step-text {:pattern :parse-cli-args})))

    (it "formats format-spawn-log step"
      (should= "formatting the spawn log"
               (gen/step-text {:pattern :format-spawn-log})))

    (it "formats format-idle-log step"
      (should= "formatting the idle log"
               (gen/step-text {:pattern :format-idle-log})))

    (it "formats format-zombie-log step"
      (should= "formatting the zombie log"
               (gen/step-text {:pattern :format-zombie-log})))

    (it "formats assert-task-contains step"
      (should= "the task should contain \"~/Projects/test\""
               (gen/step-text {:pattern :assert-task-contains :expected "~/Projects/test"})))

    (it "formats assert-args-include step"
      (should= "the args should include \"--message\""
               (gen/step-text {:pattern :assert-args-include :expected "--message"})))

    (it "formats assert-args-not-include step"
      (should= "the args should not include \"--agent\""
               (gen/step-text {:pattern :assert-args-not-include :expected "--agent"})))

    (it "formats assert-agent-value step"
      (should= "the agent value should be \"scrapper\""
               (gen/step-text {:pattern :assert-agent-value :expected "scrapper"})))

    (it "formats assert-dry-run true step"
      (should= "dry-run should be true"
               (gen/step-text {:pattern :assert-dry-run :expected true})))

    (it "formats assert-dry-run false step"
      (should= "dry-run should be false"
               (gen/step-text {:pattern :assert-dry-run :expected false})))

    (it "formats assert-verbose step"
      (should= "verbose should be false"
               (gen/step-text {:pattern :assert-verbose :expected false})))

    (it "formats assert-parse-error step"
      (should= "parsing should return an error"
               (gen/step-text {:pattern :assert-parse-error})))

    (it "formats assert-error-contains step"
      (should= "the error should contain \"--bogus\""
               (gen/step-text {:pattern :assert-error-contains :expected "--bogus"})))

    (it "formats assert-log-contains step"
      (should= "the log should contain \"2 worker\""
               (gen/step-text {:pattern :assert-log-contains :expected "2 worker"}))))

  (context "generate-step-comments"

    (it "generates Given comments from IR nodes"
      (let [comments (gen/generate-step-comments
                       {:steps [{:type :given :pattern :unrecognized :text "a step"}
                                {:type :and :pattern :unrecognized :text "another step"}]}
                       nil)]
        (should-contain ";; Given a step" comments)
        (should-contain ";; And another step" comments)))

    (it "generates When comments from IR nodes"
      (let [comments (gen/generate-step-comments
                       {:steps [{:type :when :pattern :unrecognized :text "something happens"}]}
                       nil)]
        (should-contain ";; When something happens" comments)))

    (it "generates Then comments from IR nodes"
      (let [comments (gen/generate-step-comments
                       {:steps [{:type :then :pattern :unrecognized :text "a result"}
                                {:type :and :pattern :unrecognized :text "another result"}]}
                       nil)]
        (should-contain ";; Then a result" comments)
        (should-contain ";; And another result" comments)))

    (it "includes background steps as comments before scenario steps"
      (let [background {:steps [{:type :given :pattern :unrecognized :text "common setup"}
                                {:type :and :pattern :unrecognized :text "other setup"}]}
            comments (gen/generate-step-comments
                       {:steps [{:type :given :pattern :unrecognized :text "scenario step"}
                                {:type :when :pattern :unrecognized :text "action"}
                                {:type :then :pattern :unrecognized :text "result"}]}
                       background)]
        (should-contain ";; Background:" comments)
        (should-contain ";; Given common setup" comments)
        (should-contain ";; And other setup" comments)
        ;; Background should come before scenario steps
        (should (< (str/index-of comments "Background:")
                   (str/index-of comments "scenario step"))))))

  (context "generate-scenario"

    (it "generates a context with pending it block for unrecognized steps"
      (let [scenario {:scenario "Simple test"
                      :steps [{:type :given :pattern :unrecognized :text "a step"}
                              {:type :when :pattern :unrecognized :text "action"}
                              {:type :then :pattern :unrecognized :text "result"}]}
            output (gen/generate-scenario scenario nil)]
        (should-contain "(context \"Simple test\"" output)
        (should-contain "(it \"Simple test\"" output)
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "includes step comments in the pending it block"
      (let [scenario {:scenario "Test"
                      :steps [{:type :given :pattern :unrecognized :text "first given"}
                              {:type :when :pattern :unrecognized :text "the action"}
                              {:type :then :pattern :unrecognized :text "expected result"}]}
            output (gen/generate-scenario scenario nil)]
        (should-contain ";; Given first given" output)
        (should-contain ";; When the action" output)
        (should-contain ";; Then expected result" output)))

    (it "includes background comments when present"
      (let [scenario {:scenario "With bg"
                      :steps [{:type :given :pattern :unrecognized :text "scenario step"}
                              {:type :when :pattern :unrecognized :text "action"}
                              {:type :then :pattern :unrecognized :text "result"}]}
            background {:steps [{:type :given :pattern :unrecognized :text "bg step"}]}
            output (gen/generate-scenario scenario background)]
        (should-contain ";; Background:" output)
        (should-contain ";; Given bg step" output)))

    (it "generates executable code for fully recognized scenarios"
      (let [scenario {:scenario "Zombie detected"
                      :steps [{:type :given :pattern :session :session-id "s1" :label "project:proj:proj-abc"}
                              {:type :and :pattern :session-status :session-id "s1" :status "running" :age-seconds 100}
                              {:type :and :pattern :bead-status :bead-id "proj-abc" :status "closed"}
                              {:type :when :pattern :check-zombies}
                              {:type :then :pattern :assert-zombie :session-id "s1" :reason "bead-closed"}]}
            background {:steps [{:type :given :pattern :project-config :slug "proj" :worker-timeout 3600}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/add-project-config \"proj\" {:worker-timeout 3600})" output)
        (should-contain "(h/add-session \"s1\" {:label \"project:proj:proj-abc\"})" output)
        (should-contain "(h/set-session-status \"s1\" \"running\" 100)" output)
        (should-contain "(h/set-bead-status \"proj-abc\" \"closed\")" output)
        (should-contain "(h/check-zombies!)" output)
        (should-contain "(should (h/zombie? \"s1\"))" output)
        (should-contain "(should= \"bead-closed\" (h/zombie-reason \"s1\"))" output)))

    (it "generates executable code for assert-no-zombies"
      (let [scenario {:scenario "No zombies"
                      :steps [{:type :given :pattern :session :session-id "s3" :label "project:proj:proj-ghi"}
                              {:type :and :pattern :session-status :session-id "s3" :status "running" :age-seconds 100}
                              {:type :and :pattern :bead-status :bead-id "proj-ghi" :status "open"}
                              {:type :when :pattern :check-zombies}
                              {:type :then :pattern :assert-no-zombies}]}
            background {:steps [{:type :given :pattern :project-config :slug "proj" :worker-timeout 3600}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(should= [] (h/zombies))" output)))

    (it "generates pending for mixed recognized/unrecognized steps"
      (let [scenario {:scenario "Mixed"
                      :steps [{:type :given :pattern :session :session-id "s1" :label "project:proj:proj-abc"}
                              {:type :and :pattern :unrecognized :text "some unknown step"}
                              {:type :when :pattern :check-zombies}
                              {:type :then :pattern :assert-no-zombies}]}
            output (gen/generate-scenario scenario nil)]
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "generates executable code for orch_spawning scenario"
      (let [scenario {:scenario "Spawn workers"
                      :steps [{:type :given :pattern :ready-beads :slug "alpha" :count 3}
                              {:type :and :pattern :active-workers :slug "alpha" :count 0}
                              {:type :when :pattern :orch-tick}
                              {:type :then :pattern :assert-action :expected "spawn"}
                              {:type :and :pattern :assert-spawn-count :count 2}]}
            background {:steps [{:type :given :pattern :project-config :slug "alpha" :max-workers 2}
                                {:type :and :pattern :active-iteration :slug "alpha" :iteration "003"}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/add-project \"alpha\" {:max-workers 2})" output)
        (should-contain "(h/set-active-iteration \"alpha\" \"003\")" output)
        (should-contain "(h/set-ready-beads \"alpha\" 3)" output)
        (should-contain "(h/set-active-workers \"alpha\" 0)" output)
        (should-contain "(h/orch-tick!)" output)
        (should-contain "(should= \"spawn\" (h/tick-action))" output)
        (should-contain "(should= 2 (h/spawn-count))" output)))

    (it "generates executable code for orch idle scenario"
      (let [scenario {:scenario "Idle when no ready beads"
                      :steps [{:type :given :pattern :ready-beads :slug "alpha" :count 0}
                              {:type :and :pattern :active-workers :slug "alpha" :count 0}
                              {:type :when :pattern :orch-tick}
                              {:type :then :pattern :assert-action :expected "idle"}
                              {:type :and :pattern :assert-idle-reason :expected "no-ready-beads"}]}
            background {:steps [{:type :given :pattern :project-config :slug "alpha" :max-workers 2}
                                {:type :and :pattern :active-iteration :slug "alpha" :iteration "003"}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(should= \"idle\" (h/tick-action))" output)
        (should-contain "(should= \"no-ready-beads\" (h/idle-reason))" output)))

    (it "generates executable code for orch-tick-project"
      (let [scenario {:scenario "Idle for beta only"
                      :steps [{:type :given :pattern :no-active-iteration :slug "beta"}
                              {:type :when :pattern :orch-tick-project :slug "beta"}
                              {:type :then :pattern :assert-action :expected "idle"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/remove-iteration \"beta\")" output)
        (should-contain "(h/orch-tick-project! \"beta\")" output)))

    (it "generates executable code for spawn label assertion"
      (let [scenario {:scenario "Label check"
                      :steps [{:type :given :pattern :ready-bead-with-id :slug "alpha" :bead-id "alpha-abc"}
                              {:type :and :pattern :active-workers :slug "alpha" :count 0}
                              {:type :when :pattern :orch-tick}
                              {:type :then :pattern :assert-action :expected "spawn"}
                              {:type :and :pattern :assert-spawn-label :expected "project:alpha:alpha-abc"}]}
            background {:steps [{:type :given :pattern :project-config :slug "alpha" :max-workers 2}
                                {:type :and :pattern :active-iteration :slug "alpha" :iteration "003"}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-ready-bead-with-id \"alpha\" \"alpha-abc\")" output)
        (should-contain "(should= \"project:alpha:alpha-abc\" (h/spawn-label))" output)))

    (it "generates executable code for session ID generation scenario"
      (let [scenario {:scenario "Generate deterministic session ID"
                      :steps [{:type :given :pattern :bead :bead-id "proj-abc"}
                              {:type :when :pattern :generate-session-id}
                              {:type :then :pattern :assert-session-id :expected "braids-proj-abc-worker"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-bead-id \"proj-abc\")" output)
        (should-contain "(h/generate-session-id!)" output)
        (should-contain "(should= \"braids-proj-abc-worker\" (h/session-id-result))" output)))

    (it "generates executable code for session ID identical scenario"
      (let [scenario {:scenario "Same bead same ID"
                      :steps [{:type :given :pattern :bead :bead-id "proj-xyz"}
                              {:type :when :pattern :generate-session-id-twice}
                              {:type :then :pattern :assert-ids-identical}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-bead-id \"proj-xyz\")" output)
        (should-contain "(h/generate-session-id-twice!)" output)
        (should-contain "(should (h/session-ids-identical?))" output)))

    (it "generates executable code for different session IDs scenario"
      (let [scenario {:scenario "Different beads different IDs"
                      :steps [{:type :given :pattern :bead :bead-id "proj-aaa"}
                              {:type :and :pattern :bead :bead-id "proj-bbb"}
                              {:type :when :pattern :generate-session-ids-both}
                              {:type :then :pattern :assert-ids-different}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-bead-id \"proj-aaa\")" output)
        (should-contain "(h/set-bead-id \"proj-bbb\")" output)
        (should-contain "(h/generate-session-ids-both!)" output)
        (should-contain "(should (h/session-ids-different?))" output)))

    (it "generates executable code for parse session ID scenario"
      (let [scenario {:scenario "Parse session ID"
                      :steps [{:type :given :pattern :session-id-literal :session-id "braids-proj-abc-worker"}
                              {:type :when :pattern :parse-session-id}
                              {:type :then :pattern :assert-bead-id :expected "proj-abc"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-session-id-literal \"braids-proj-abc-worker\")" output)
        (should-contain "(h/parse-session-id!)" output)
        (should-contain "(should= \"proj-abc\" (h/parsed-bead-id))" output)))

    (it "generates executable code for ready beads filters active projects"
      (let [scenario {:scenario "Ready beads filters to active projects only"
                      :steps [{:type :given :pattern :registry-with-projects-table
                               :table {:headers ["slug" "status" "priority"]
                                       :rows [["alpha" "active" "normal"]
                                              ["beta" "paused" "normal"]]}}
                              {:type :and :pattern :project-config-max-workers :slug "alpha" :max-workers 1}
                              {:type :and :pattern :project-config-max-workers :slug "beta" :max-workers 1}
                              {:type :and :pattern :project-ready-beads-table :slug "alpha"
                               :table {:headers ["id" "title" "priority"]
                                       :rows [["alpha-aaa" "Task A" "P1"]]}}
                              {:type :and :pattern :project-ready-beads-table :slug "beta"
                               :table {:headers ["id" "title" "priority"]
                                       :rows [["beta-bbb" "Task B" "P1"]]}}
                              {:type :and :pattern :no-active-workers}
                              {:type :when :pattern :compute-ready-beads}
                              {:type :then :pattern :assert-result-contains-bead :bead-id "alpha-aaa"}
                              {:type :and :pattern :assert-result-not-contains-bead :bead-id "beta-bbb"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-registry-from-table" output)
        (should-contain "(h/set-project-config" output)
        (should-contain "(h/set-project-ready-beads" output)
        (should-contain "(h/compute-ready-beads!)" output)
        (should-contain "(should (h/result-contains-bead? \"alpha-aaa\"))" output)
        (should-contain "(should-not (h/result-contains-bead? \"beta-bbb\"))" output)))

    (it "generates executable code for ready beads result empty"
      (let [scenario {:scenario "Result empty"
                      :steps [{:type :given :pattern :no-active-workers}
                              {:type :when :pattern :compute-ready-beads}
                              {:type :then :pattern :assert-result-empty}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(should (empty? (h/ready-result)))" output)))

    (it "generates executable code for nth result project assertion"
      (let [scenario {:scenario "Ordered by priority"
                      :steps [{:type :given :pattern :no-active-workers}
                              {:type :when :pattern :compute-ready-beads}
                              {:type :then :pattern :assert-nth-result-project :position 1 :slug "high"}
                              {:type :and :pattern :assert-nth-result-project :position 2 :slug "norm"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(should= \"high\" (:project (nth (h/ready-result) 0)))" output)
        (should-contain "(should= \"norm\" (:project (nth (h/ready-result) 1)))" output)))

    (it "generates executable code for format ready output scenario"
      (let [scenario {:scenario "Format output"
                      :steps [{:type :given :pattern :ready-beads-to-format
                               :table {:headers ["project" "id" "title" "priority"]
                                       :rows [["proj" "proj-abc" "Do stuff" "P0"]]}}
                              {:type :when :pattern :format-ready-output}
                              {:type :then :pattern :assert-output-contains :expected "proj-abc"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-ready-beads-to-format" output)
        (should-contain "(h/format-ready-output!)" output)
        (should-contain "(should (clojure.string/includes? (h/output) \"proj-abc\"))" output)))

    (it "generates executable code for no ready beads to format"
      (let [scenario {:scenario "Empty format"
                      :steps [{:type :given :pattern :no-ready-beads-to-format}
                              {:type :when :pattern :format-ready-output}
                              {:type :then :pattern :assert-output-equals :expected "No ready beads."}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-no-ready-beads-to-format)" output)))

    (it "generates executable code for project-config-status-and-max-workers"
      (let [scenario {:scenario "Config with status"
                      :steps [{:type :given :pattern :project-config-status-and-max-workers :slug "proj" :status "paused" :max-workers 1}
                              {:type :and :pattern :no-active-workers}
                              {:type :when :pattern :compute-ready-beads}
                              {:type :then :pattern :assert-result-empty}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-project-config \"proj\" {:status \"paused\" :max-workers 1})" output)))

    (it "generates executable code for bead-no-status"
      (let [scenario {:scenario "No bead status"
                      :steps [{:type :given :pattern :session :session-id "s5" :label "project:proj:proj-mno"}
                              {:type :and :pattern :session-status :session-id "s5" :status "running" :age-seconds 100}
                              {:type :and :pattern :bead-no-status :bead-id "proj-mno"}
                              {:type :when :pattern :check-zombies}
                              {:type :then :pattern :assert-no-zombies}]}
            background {:steps [{:type :given :pattern :project-config :slug "proj" :worker-timeout 3600}]}
            output (gen/generate-scenario scenario background)]
        ;; bead-no-status means no setup needed — skip that step
        (should-not-contain "bead-status" output)
        (should-not-contain "pending" output)))

    ;; --- Iteration management scenario generation ---

    (it "generates executable code for parse iteration EDN scenario"
      (let [scenario {:scenario "Parse iteration EDN with defaults"
                      :steps [{:type :given :pattern :iteration-edn :number "003" :status "active" :story-count 1}
                              {:type :and :pattern :edn-no-guardrails-or-notes}
                              {:type :when :pattern :parse-iteration-edn}
                              {:type :then :pattern :assert-iteration-number :expected "003"}
                              {:type :and :pattern :assert-iteration-status :expected "active"}
                              {:type :and :pattern :assert-iteration-guardrails-empty}
                              {:type :and :pattern :assert-iteration-notes-empty}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-iteration-edn \"003\" \"active\" 1)" output)
        (should-contain "(h/parse-iteration-edn!)" output)
        (should-contain "(should= \"003\" (h/iteration-number))" output)
        (should-contain "(should= \"active\" (h/iteration-status))" output)
        (should-contain "(should (empty? (h/iteration-guardrails)))" output)
        (should-contain "(should (empty? (h/iteration-notes)))" output)))

    (it "generates executable code for annotate stories scenario"
      (let [scenario {:scenario "Annotate stories with bead data"
                      :steps [{:type :given :pattern :iteration-with-stories :story-ids ["proj-abc" "proj-def"]}
                              {:type :and :pattern :iter-bead-status :bead-id "proj-abc" :status "open" :priority 1}
                              {:type :and :pattern :iter-bead-status :bead-id "proj-def" :status "closed" :priority 2}
                              {:type :when :pattern :annotate-stories}
                              {:type :then :pattern :assert-story-status :story-id "proj-abc" :expected "open"}
                              {:type :and :pattern :assert-story-status :story-id "proj-def" :expected "closed"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-iteration-stories [\"proj-abc\" \"proj-def\"])" output)
        (should-contain "(h/add-iter-bead \"proj-abc\" \"open\" 1)" output)
        (should-contain "(h/add-iter-bead \"proj-def\" \"closed\" 2)" output)
        (should-contain "(h/annotate-stories!)" output)
        (should-contain "(should= \"open\" (h/story-status \"proj-abc\"))" output)
        (should-contain "(should= \"closed\" (h/story-status \"proj-def\"))" output)))

    (it "generates executable code for completion stats scenario"
      (let [scenario {:scenario "Completion stats calculation"
                      :steps [{:type :given :pattern :annotated-stories :closed 2 :open 2 :total 4}
                              {:type :when :pattern :calculate-completion-stats}
                              {:type :then :pattern :assert-total :expected 4}
                              {:type :and :pattern :assert-closed-count :expected 2}
                              {:type :and :pattern :assert-completion-percent :expected 50}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-annotated-stories 2 2 4)" output)
        (should-contain "(h/calculate-completion-stats!)" output)
        (should-contain "(should= 4 (h/stats-total))" output)
        (should-contain "(should= 2 (h/stats-closed))" output)
        (should-contain "(should= 50 (h/stats-percent))" output)))

    (it "generates executable code for format iteration scenario"
      (let [scenario {:scenario "Format iteration with status icons"
                      :steps [{:type :given :pattern :iteration-number-status :number "009" :status "active"}
                              {:type :and :pattern :story-with-status :story-id "proj-abc" :status "open"}
                              {:type :and :pattern :story-with-status :story-id "proj-def" :status "closed"}
                              {:type :and :pattern :completion-stats :closed 1 :total 2}
                              {:type :when :pattern :format-iteration}
                              {:type :then :pattern :assert-output-contains :expected "Iteration 009"}
                              {:type :and :pattern :assert-output-contains :expected "active"}
                              {:type :and :pattern :assert-output-contains :expected "50%"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-iteration-number-status \"009\" \"active\")" output)
        (should-contain "(h/add-story-with-status \"proj-abc\" \"open\")" output)
        (should-contain "(h/set-completion-stats 1 2)" output)
        (should-contain "(h/format-iteration!)" output)
        (should-contain "(should (clojure.string/includes? (h/output) \"Iteration 009\"))" output)))

    (it "generates executable code for format iteration JSON scenario"
      (let [scenario {:scenario "Format iteration as JSON"
                      :steps [{:type :given :pattern :iteration-number-status :number "001" :status "active"}
                              {:type :and :pattern :story-with-status :story-id "a" :status "open"}
                              {:type :and :pattern :completion-stats :closed 0 :total 1}
                              {:type :when :pattern :format-iteration-json}
                              {:type :then :pattern :assert-json-contains :expected "number"}
                              {:type :and :pattern :assert-json-contains :expected "stories"}
                              {:type :and :pattern :assert-json-contains :expected "percent"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/format-iteration-json!)" output)
        (should-contain "(should (clojure.string/includes? (h/iter-json-output) \"number\"))" output)))

    (it "generates executable code for validate iteration scenario"
      (let [scenario {:scenario "Validate rejects invalid status"
                      :steps [{:type :given :pattern :iteration-with-status :number "001" :status "bogus"}
                              {:type :when :pattern :validate-iteration}
                              {:type :then :pattern :assert-validation-fail :expected "Invalid status"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-iteration-with-status \"001\" \"bogus\")" output)
        (should-contain "(h/validate-iteration!)" output)))

    ;; --- Configuration generate-scenario ---

    (it "generates executable code for config get scenario"
      (let [scenario {:scenario "Config get returns value"
                      :steps [{:type :given :pattern :config-with-values
                               :table {:headers ["key" "value"]
                                       :rows [["braids-home" "/custom/path"]]}}
                              {:type :when :pattern :get-config-key :key "braids-home"}
                              {:type :then :pattern :assert-result-ok-with-value :expected "/custom/path"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-config-from-table" output)
        (should-contain "(h/get-config-key!" output)
        (should-contain "(should= \"/custom/path\" (:ok (h/config-result)))" output)))

    (it "generates executable code for config set scenario"
      (let [scenario {:scenario "Config set updates value"
                      :steps [{:type :given :pattern :config-with-values
                               :table {:headers ["key" "value"]
                                       :rows [["braids-home" "~/Projects"]]}}
                              {:type :when :pattern :set-config-key :key "braids-home" :value "/new/path"}
                              {:type :then :pattern :assert-config-has-value :key "braids-home" :expected "/new/path"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-config-key!" output)
        (should-contain "(h/current-config)" output)))

    ;; --- Project status generate-scenario ---

    (it "generates executable code for dashboard build scenario"
      (let [scenario {:scenario "Dashboard includes all projects"
                      :steps [{:type :given :pattern :registry-with-projects-table
                               :table {:headers ["slug" "status" "priority" "path"]
                                       :rows [["alpha" "active" "high" "~/Projects/alpha"]]}}
                              {:type :and :pattern :project-configs-table
                               :table {:headers ["slug" "max-workers"]
                                       :rows [["alpha" "2"]]}}
                              {:type :and :pattern :active-iterations-table
                               :table {:headers ["slug" "number" "total" "closed" "percent"]
                                       :rows [["alpha" "009" "3" "1" "33"]]}}
                              {:type :and :pattern :active-workers-table
                               :table {:headers ["slug" "count"]
                                       :rows [["alpha" "1"]]}}
                              {:type :when :pattern :build-dashboard}
                              {:type :then :pattern :assert-dashboard-project-count :count 1}
                              {:type :and :pattern :assert-project-status :slug "alpha" :expected "active"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-registry-from-table" output)
        (should-contain "(h/set-project-configs-from-table" output)
        (should-contain "(h/set-active-iterations-from-table" output)
        (should-contain "(h/set-active-workers-from-table" output)
        (should-contain "(h/build-dashboard!)" output)
        (should-contain "(should= 1 (count (:projects (h/dashboard))))" output)
        (should-contain "(should= \"active\" (:status (h/dashboard-project \"alpha\")))" output)))

    (it "generates executable code for project detail scenario"
      (let [scenario {:scenario "Project detail"
                      :steps [{:type :given :pattern :dashboard-project :slug "alpha"
                               :table {:headers ["status" "active"] :rows []}}
                              {:type :and :pattern :project-has-iteration :slug "alpha"
                               :table {:headers ["number" "009"] :rows []}}
                              {:type :and :pattern :project-has-stories :slug "alpha"
                               :table {:headers ["id" "title" "status"]
                                       :rows [["a-001" "Do thing" "closed"]]}}
                              {:type :when :pattern :format-project-detail :slug "alpha"}
                              {:type :then :pattern :assert-output-contains :expected "alpha"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-dashboard-project \"alpha\"" output)
        (should-contain "(h/set-project-iteration \"alpha\"" output)
        (should-contain "(h/set-project-stories \"alpha\"" output)
        (should-contain "(h/format-project-detail! \"alpha\")" output)))

    (it "generates executable code for no-iteration project detail"
      (let [scenario {:scenario "No iteration fallback"
                      :steps [{:type :given :pattern :dashboard-project :slug "beta"
                               :table {:headers ["status" "paused"] :rows []}}
                              {:type :and :pattern :project-has-no-iteration :slug "beta"}
                              {:type :when :pattern :format-project-detail :slug "beta"}
                              {:type :then :pattern :assert-output-contains :expected "no active iteration"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/clear-project-iteration \"beta\")" output)
        (should-contain "(h/format-project-detail! \"beta\")" output)))

    (it "generates executable code for dashboard JSON scenario"
      (let [scenario {:scenario "Dashboard JSON output"
                      :steps [{:type :when :pattern :format-dashboard-json}
                              {:type :then :pattern :assert-json-project-count :count 1}
                              {:type :and :pattern :assert-json-project-iteration-percent :slug "alpha" :percent 33}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/format-dashboard-json!)" output)
        (should-contain "(should= 1 (count (:projects (h/dashboard-json))))" output)
        (should-contain "(should= 33 (get-in (h/dashboard-json-project \"alpha\") [\"iteration\" \"stats\" \"percent\"]))" output)))

    (it "generates executable code for empty registry scenario"
      (let [scenario {:scenario "Empty registry"
                      :steps [{:type :given :pattern :empty-registry}
                              {:type :when :pattern :build-dashboard}
                              {:type :and :pattern :format-dashboard}
                              {:type :then :pattern :assert-output-equals :expected "No projects registered."}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-empty-registry)" output)
        (should-contain "(h/build-dashboard!)" output)
        (should-contain "(h/format-dashboard!)" output)
        (should-contain "(should= \"No projects registered.\" (h/output))" output)))

    ;; --- Orch runner generate-scenario ---

    (it "generates executable code for build worker task scenario"
      (let [scenario {:scenario "Build worker task message from template"
                      :steps [{:type :given :pattern :spawn-entry-path-bead :path "~/Projects/test" :bead "test-abc"}
                              {:type :and :pattern :spawn-iteration-channel :iteration "001" :channel "12345"}
                              {:type :when :pattern :build-worker-task}
                              {:type :then :pattern :assert-task-contains :expected "~/Projects/test"}
                              {:type :and :pattern :assert-task-contains :expected "test-abc"}
                              {:type :and :pattern :assert-task-contains :expected "001"}
                              {:type :and :pattern :assert-task-contains :expected "worker.md"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-spawn-entry {:path \"~/Projects/test\" :bead \"test-abc\"})" output)
        (should-contain "(h/update-spawn-entry {:iteration \"001\" :channel \"12345\"})" output)
        (should-contain "(h/build-worker-task!)" output)
        (should-contain "(should (clojure.string/includes? (h/worker-task) \"~/Projects/test\"))" output)))

    (it "generates executable code for build worker args scenario"
      (let [scenario {:scenario "Build worker CLI args with session ID"
                      :steps [{:type :given :pattern :spawn-entry-bead :bead "proj-abc"}
                              {:type :and :pattern :no-worker-agent}
                              {:type :when :pattern :build-worker-args}
                              {:type :then :pattern :assert-args-include :expected "--message"}
                              {:type :and :pattern :assert-args-include :expected "--session-id"}
                              {:type :and :pattern :assert-args-not-include :expected "--agent"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-spawn-entry {:bead \"proj-abc\"})" output)
        (should-contain "(h/build-worker-args!)" output)
        (should-contain "(should (some #(= \"--message\" %) (h/worker-args)))" output)
        (should-contain "(should-not (some #(= \"--agent\" %) (h/worker-args)))" output)))

    (it "generates executable code for custom agent scenario"
      (let [scenario {:scenario "Build args with custom agent"
                      :steps [{:type :given :pattern :spawn-entry-bead :bead "proj-abc"}
                              {:type :and :pattern :worker-agent :agent "scrapper"}
                              {:type :when :pattern :build-worker-args}
                              {:type :then :pattern :assert-args-include :expected "--agent"}
                              {:type :and :pattern :assert-agent-value :expected "scrapper"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-worker-agent \"scrapper\")" output)
        (should-contain "(h/build-worker-args!)" output)))

    (it "generates executable code for parse CLI args defaults scenario"
      (let [scenario {:scenario "Parse CLI args defaults to dry-run"
                      :steps [{:type :given :pattern :no-cli-args}
                              {:type :when :pattern :parse-cli-args}
                              {:type :then :pattern :assert-dry-run :expected true}
                              {:type :and :pattern :assert-verbose :expected false}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-cli-args [])" output)
        (should-contain "(h/parse-cli-args!)" output)
        (should-contain "(should= true (:dry-run (h/parsed-cli-args)))" output)
        (should-contain "(should= false (:verbose (h/parsed-cli-args)))" output)))

    (it "generates executable code for parse --confirmed scenario"
      (let [scenario {:scenario "Parse --confirmed enables run"
                      :steps [{:type :given :pattern :cli-args :args "--confirmed"}
                              {:type :when :pattern :parse-cli-args}
                              {:type :then :pattern :assert-dry-run :expected false}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-cli-args [\"--confirmed\"])" output)
        (should-contain "(h/parse-cli-args!)" output)
        (should-contain "(should= false (:dry-run (h/parsed-cli-args)))" output)))

    (it "generates executable code for parse unknown arg scenario"
      (let [scenario {:scenario "Parse unknown arg returns error"
                      :steps [{:type :given :pattern :cli-args :args "--bogus"}
                              {:type :when :pattern :parse-cli-args}
                              {:type :then :pattern :assert-parse-error}
                              {:type :and :pattern :assert-error-contains :expected "--bogus"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-cli-args [\"--bogus\"])" output)
        (should-contain "(should (:error (h/parsed-cli-args)))" output)
        (should-contain "(should (clojure.string/includes? (:error (h/parsed-cli-args)) \"--bogus\"))" output)))

    (it "generates executable code for format spawn log scenario"
      (let [scenario {:scenario "Format spawn log"
                      :steps [{:type :given :pattern :spawn-tick-result :count 2}
                              {:type :and :pattern :spawn-beads :beads ["b1" "b2"]}
                              {:type :when :pattern :format-spawn-log}
                              {:type :then :pattern :assert-log-contains :expected "2 worker"}
                              {:type :and :pattern :assert-log-contains :expected "b1"}
                              {:type :and :pattern :assert-log-contains :expected "b2"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-spawn-tick-result 2 [])" output)
        (should-contain "(h/add-spawn-beads [\"b1\" \"b2\"])" output)
        (should-contain "(h/format-spawn-log!)" output)
        (should-contain "(should (some #(clojure.string/includes? % \"2 worker\") (h/runner-log)))" output)))

    (it "generates executable code for format idle log scenario"
      (let [scenario {:scenario "Format idle log"
                      :steps [{:type :given :pattern :idle-tick-result :reason "all-at-capacity"}
                              {:type :when :pattern :format-idle-log}
                              {:type :then :pattern :assert-log-contains :expected "Idle"}
                              {:type :and :pattern :assert-log-contains :expected "all-at-capacity"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-idle-tick-result \"all-at-capacity\")" output)
        (should-contain "(h/format-idle-log!)" output)
        (should-contain "(should (some #(clojure.string/includes? % \"Idle\") (h/runner-log)))" output)))

    (it "generates executable code for format zombie log scenario"
      (let [scenario {:scenario "Format zombie log"
                      :steps [{:type :given :pattern :zombie-sessions :count 2 :reasons ["bead-closed" "timeout"]}
                              {:type :when :pattern :format-zombie-log}
                              {:type :then :pattern :assert-log-contains :expected "2 zombie"}
                              {:type :and :pattern :assert-log-contains :expected "bead-closed"}
                              {:type :and :pattern :assert-log-contains :expected "timeout"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-zombie-sessions 2 [\"bead-closed\" \"timeout\"])" output)
        (should-contain "(h/format-zombie-log!)" output)
        (should-contain "(should (some #(clojure.string/includes? % \"2 zombie\") (h/runner-log)))" output))))

  (context "generate-spec"

    (it "generates a complete spec file with pending for unrecognized steps"
      (let [ir {:source "test_feature.feature"
                :feature "Test feature"
                :scenarios [{:scenario "First test"
                             :steps [{:type :given :pattern :unrecognized :text "a given"}
                                     {:type :when :pattern :unrecognized :text "an action"}
                                     {:type :then :pattern :unrecognized :text "a result"}]}]}
            output (gen/generate-spec ir)]
        (should-contain "(ns braids.features.test-feature-spec" output)
        (should-contain "(describe \"Test feature\"" output)
        (should-contain "(context \"First test\"" output)
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "skips @wip scenarios"
      (let [ir {:source "wip_test.feature"
                :feature "WIP test"
                :scenarios [{:scenario "Normal"
                             :steps [{:type :given :pattern :unrecognized :text "a"}
                                     {:type :when :pattern :unrecognized :text "b"}
                                     {:type :then :pattern :unrecognized :text "c"}]}
                            {:scenario "WIP one"
                             :steps [{:type :given :pattern :unrecognized :text "a"}
                                     {:type :when :pattern :unrecognized :text "b"}
                                     {:type :then :pattern :unrecognized :text "c"}]
                             :wip true}]}
            output (gen/generate-spec ir)]
        (should-contain "(context \"Normal\"" output)
        (should-not-contain "WIP one" output)))

    (it "generates ns with harness require when scenarios have executable code"
      (let [ir {:source "zombie.feature"
                :feature "Zombie"
                :scenarios [{:scenario "Test"
                             :steps [{:type :given :pattern :session :session-id "s1" :label "project:proj:proj-abc"}
                                     {:type :when :pattern :check-zombies}
                                     {:type :then :pattern :assert-no-zombies}]}]}
            output (gen/generate-spec ir)]
        (should-contain "[braids.features.harness :as h]" output)))

    (it "does not include harness require when all scenarios are pending"
      (let [ir {:source "test.feature"
                :feature "Test"
                :scenarios [{:scenario "S1"
                             :steps [{:type :given :pattern :unrecognized :text "a"}
                                     {:type :when :pattern :unrecognized :text "b"}
                                     {:type :then :pattern :unrecognized :text "c"}]}]}
            output (gen/generate-spec ir)]
        (should-not-contain "harness" output)))

    (it "generates valid Clojure that can be read"
      (let [ir {:source "readable.feature"
                :feature "Readable spec"
                :scenarios [{:scenario "Test one"
                             :steps [{:type :given :pattern :unrecognized :text "step"}
                                     {:type :when :pattern :unrecognized :text "action"}
                                     {:type :then :pattern :unrecognized :text "result"}]}]}
            output (gen/generate-spec ir)]
        ;; Should be parseable as Clojure forms
        (should-not-throw
          (read-string (str "[" output "]"))))))

  (context "generate-spec with real IR"

    ;; Note: These tests require running bb features:parse first to generate the .edn IR files
    ;; with the new typed IR format.

    (it "generates spec from orch_spawning IR with all executable scenarios"
      (let [ir (read-string (slurp "features/edn/orch_spawning.edn"))
            output (gen/generate-spec ir)]
        (should-contain "(ns braids.features.orch-spawning-spec" output)
        (should-contain "(describe \"Orchestrator spawning behavior\"" output)
        ;; All 7 scenarios should be present
        (should= 7 (count (re-seq #"\(context " output)))
        ;; All orch_spawning steps are now recognized — no pending
        (should-not-contain "pending" output)
        ;; Should have harness calls
        (should-contain "(h/reset!)" output)
        (should-contain "(h/orch-tick!)" output)
        (should-contain "[braids.features.harness :as h]" output)))

    (it "generates spec from worker_session_tracking IR with all executable scenarios"
      (let [ir (read-string (slurp "features/edn/worker_session_tracking.edn"))
            output (gen/generate-spec ir)]
        ;; 6 total scenarios, 2 are @wip, so 4 should be generated
        (should= 4 (count (re-seq #"\(context " output)))
        (should-not-contain "Prevent duplicate spawning" output)
        (should-not-contain "Session with missing bead data" output)
        ;; All 4 session tracking steps are now recognized — no pending
        (should-not-contain "pending" output)
        ;; Should have harness calls
        (should-contain "(h/reset!)" output)
        (should-contain "(h/generate-session-id!)" output)
        (should-contain "[braids.features.harness :as h]" output)))

    (it "generates spec from zombie_detection IR with executable code"
      (let [ir (read-string (slurp "features/edn/zombie_detection.edn"))
            output (gen/generate-spec ir)]
        ;; 7 total scenarios, 2 are @wip, so 5 should be generated
        (should= 5 (count (re-seq #"\(context " output)))
        (should-not-contain "Zombie cleanup kills" output)
        (should-not-contain "Zombie detection across multiple" output)
        ;; All 5 zombie scenarios have fully recognized steps — no pending
        (should-not-contain "pending" output)
        ;; Should have harness calls
        (should-contain "(h/reset!)" output)
        (should-contain "(h/check-zombies!)" output))))

  (context "generate-features!"

    (it "writes generated spec files to output directory"
      (let [tmp-dir (str "/tmp/gen-test-" (System/currentTimeMillis))
            edn-dir "features/edn"]
        (with-out-str (gen/generate-features! edn-dir tmp-dir))
        (let [files (->> (io/file tmp-dir) .listFiles (map #(.getName %)) sort vec)]
          (should= ["configuration_spec.clj"
                    "iteration_management_spec.clj"
                    "orch_runner_spec.clj"
                    "orch_spawning_spec.clj"
                    "project_lifecycle_spec.clj"
                    "project_listing_spec.clj"
                    "project_status_spec.clj"
                    "ready_beads_spec.clj"
                    "worker_session_tracking_spec.clj"
                    "zombie_detection_spec.clj"]
                   files))
        ;; Verify content of one file
        (let [content (slurp (str tmp-dir "/orch_spawning_spec.clj"))]
          (should-contain "(ns braids.features.orch-spawning-spec" content))
        ;; Cleanup
        (doseq [f (.listFiles (io/file tmp-dir))]
          (.delete f))
        (.delete (io/file tmp-dir))))))
