(ns braids.gherkin-generator-spec
  (:require [speclj.core :refer :all]
            [braids.gherkin-generator :as gen]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(describe "Gherkin Generator"

  (describe "source->ns-name"

    (it "converts feature filename to spec namespace"
      (should= "braids.features.orch-spawning-spec"
               (gen/source->ns-name "orch_spawning.feature")))

    (it "handles hyphens in filenames"
      (should= "braids.features.worker-session-tracking-spec"
               (gen/source->ns-name "worker_session_tracking.feature")))

    (it "handles .edn extension too"
      (should= "braids.features.zombie-detection-spec"
               (gen/source->ns-name "zombie_detection.edn"))))

  (describe "generate-ns-form"

    (it "generates a valid ns declaration"
      (let [ns-form (gen/generate-ns-form "orch_spawning.feature")]
        (should-contain "(ns braids.features.orch-spawning-spec" ns-form)
        (should-contain "[speclj.core :refer :all]" ns-form))))

  (describe "step-text"

    (it "returns :text for unrecognized steps"
      (should= "a step" (gen/step-text {:type :unrecognized :text "a step"})))

    (it "formats project-config step"
      (should= "a project \"proj\" with worker-timeout 3600"
               (gen/step-text {:type :project-config :slug "proj" :worker-timeout 3600})))

    (it "formats session step"
      (should= "a session \"s1\" with label \"project:proj:proj-abc\""
               (gen/step-text {:type :session :session-id "s1" :label "project:proj:proj-abc"})))

    (it "formats session-status step"
      (should= "session \"s1\" has status \"running\" and age 100 seconds"
               (gen/step-text {:type :session-status :session-id "s1" :status "running" :age-seconds 100})))

    (it "formats check-zombies step"
      (should= "checking for zombies"
               (gen/step-text {:type :check-zombies})))

    (it "formats assert-zombie step"
      (should= "session \"s1\" should be a zombie with reason \"bead-closed\""
               (gen/step-text {:type :assert-zombie :session-id "s1" :reason "bead-closed"})))

    (it "formats assert-no-zombies step"
      (should= "no zombies should be detected"
               (gen/step-text {:type :assert-no-zombies})))

    ;; --- Orch spawning step-text ---

    (it "formats project-config with max-workers step"
      (should= "a project \"alpha\" with max-workers 2"
               (gen/step-text {:type :project-config :slug "alpha" :max-workers 2})))

    (it "formats active-iteration step"
      (should= "project \"alpha\" has an active iteration \"003\""
               (gen/step-text {:type :active-iteration :slug "alpha" :iteration "003"})))

    (it "formats no-active-iteration step"
      (should= "project \"beta\" has no active iteration"
               (gen/step-text {:type :no-active-iteration :slug "beta"})))

    (it "formats ready-beads step"
      (should= "project \"alpha\" has 3 ready beads"
               (gen/step-text {:type :ready-beads :slug "alpha" :count 3})))

    (it "formats ready-bead-with-id step"
      (should= "project \"alpha\" has 1 ready bead with id \"alpha-abc\""
               (gen/step-text {:type :ready-bead-with-id :slug "alpha" :bead-id "alpha-abc"})))

    (it "formats active-workers step"
      (should= "project \"alpha\" has 0 active workers"
               (gen/step-text {:type :active-workers :slug "alpha" :count 0})))

    (it "formats orch-tick step"
      (should= "the orchestrator ticks"
               (gen/step-text {:type :orch-tick})))

    (it "formats orch-tick-project step"
      (should= "the orchestrator ticks for project \"beta\" only"
               (gen/step-text {:type :orch-tick-project :slug "beta"})))

    (it "formats assert-action step"
      (should= "the action should be \"spawn\""
               (gen/step-text {:type :assert-action :expected "spawn"})))

    (it "formats assert-spawn-count step"
      (should= "2 workers should be spawned"
               (gen/step-text {:type :assert-spawn-count :count 2})))

    (it "formats assert-idle-reason step"
      (should= "the idle reason should be \"no-ready-beads\""
               (gen/step-text {:type :assert-idle-reason :expected "no-ready-beads"})))

    (it "formats assert-spawn-label step"
      (should= "the spawn label should be \"project:alpha:alpha-abc\""
               (gen/step-text {:type :assert-spawn-label :expected "project:alpha:alpha-abc"})))

    ;; --- Worker session tracking step-text ---

    (it "formats bead step"
      (should= "a bead with id \"proj-abc\""
               (gen/step-text {:type :bead :bead-id "proj-abc"})))

    (it "formats session-id-literal step"
      (should= "a session ID \"braids-proj-abc-worker\""
               (gen/step-text {:type :session-id-literal :session-id "braids-proj-abc-worker"})))

    (it "formats generate-session-id step"
      (should= "generating the session ID"
               (gen/step-text {:type :generate-session-id})))

    (it "formats generate-session-id-twice step"
      (should= "generating the session ID twice"
               (gen/step-text {:type :generate-session-id-twice})))

    (it "formats generate-session-ids-both step"
      (should= "generating session IDs for both"
               (gen/step-text {:type :generate-session-ids-both})))

    (it "formats parse-session-id step"
      (should= "parsing the session ID"
               (gen/step-text {:type :parse-session-id})))

    (it "formats assert-session-id step"
      (should= "the session ID should be \"braids-proj-abc-worker\""
               (gen/step-text {:type :assert-session-id :expected "braids-proj-abc-worker"})))

    (it "formats assert-ids-identical step"
      (should= "both session IDs should be identical"
               (gen/step-text {:type :assert-ids-identical})))

    (it "formats assert-ids-different step"
      (should= "the session IDs should be different"
               (gen/step-text {:type :assert-ids-different})))

    (it "formats assert-bead-id step"
      (should= "the extracted bead ID should be \"proj-abc\""
               (gen/step-text {:type :assert-bead-id :expected "proj-abc"}))))

  (describe "generate-step-comments"

    (it "generates Given comments from IR nodes"
      (let [comments (gen/generate-step-comments
                       {:givens [{:type :unrecognized :text "a step"}
                                 {:type :unrecognized :text "another step"}]
                        :whens [] :thens []}
                       nil)]
        (should-contain ";; Given a step" comments)
        (should-contain ";; And another step" comments)))

    (it "generates When comments from IR nodes"
      (let [comments (gen/generate-step-comments
                       {:givens [] :whens [{:type :unrecognized :text "something happens"}] :thens []}
                       nil)]
        (should-contain ";; When something happens" comments)))

    (it "generates Then comments from IR nodes"
      (let [comments (gen/generate-step-comments
                       {:givens [] :whens [] :thens [{:type :unrecognized :text "a result"}
                                                     {:type :unrecognized :text "another result"}]}
                       nil)]
        (should-contain ";; Then a result" comments)
        (should-contain ";; And another result" comments)))

    (it "includes background steps as comments before scenario steps"
      (let [background {:givens [{:type :unrecognized :text "common setup"}
                                 {:type :unrecognized :text "other setup"}]}
            comments (gen/generate-step-comments
                       {:givens [{:type :unrecognized :text "scenario step"}]
                        :whens [{:type :unrecognized :text "action"}]
                        :thens [{:type :unrecognized :text "result"}]}
                       background)]
        (should-contain ";; Background:" comments)
        (should-contain ";; Given common setup" comments)
        (should-contain ";; And other setup" comments)
        ;; Background should come before scenario steps
        (should (< (str/index-of comments "Background:")
                   (str/index-of comments "scenario step"))))))

  (describe "generate-scenario"

    (it "generates a context with pending it block for unrecognized steps"
      (let [scenario {:scenario "Simple test"
                      :givens [{:type :unrecognized :text "a step"}]
                      :whens [{:type :unrecognized :text "action"}]
                      :thens [{:type :unrecognized :text "result"}]}
            output (gen/generate-scenario scenario nil)]
        (should-contain "(context \"Simple test\"" output)
        (should-contain "(it \"Simple test\"" output)
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "includes step comments in the pending it block"
      (let [scenario {:scenario "Test"
                      :givens [{:type :unrecognized :text "first given"}]
                      :whens [{:type :unrecognized :text "the action"}]
                      :thens [{:type :unrecognized :text "expected result"}]}
            output (gen/generate-scenario scenario nil)]
        (should-contain ";; Given first given" output)
        (should-contain ";; When the action" output)
        (should-contain ";; Then expected result" output)))

    (it "includes background comments when present"
      (let [scenario {:scenario "With bg"
                      :givens [{:type :unrecognized :text "scenario step"}]
                      :whens [{:type :unrecognized :text "action"}]
                      :thens [{:type :unrecognized :text "result"}]}
            background {:givens [{:type :unrecognized :text "bg step"}]}
            output (gen/generate-scenario scenario background)]
        (should-contain ";; Background:" output)
        (should-contain ";; Given bg step" output)))

    (it "generates executable code for fully recognized scenarios"
      (let [scenario {:scenario "Zombie detected"
                      :givens [{:type :session :session-id "s1" :label "project:proj:proj-abc"}
                               {:type :session-status :session-id "s1" :status "running" :age-seconds 100}
                               {:type :bead-status :bead-id "proj-abc" :status "closed"}]
                      :whens [{:type :check-zombies}]
                      :thens [{:type :assert-zombie :session-id "s1" :reason "bead-closed"}]}
            background {:givens [{:type :project-config :slug "proj" :worker-timeout 3600}]}
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
                      :givens [{:type :session :session-id "s3" :label "project:proj:proj-ghi"}
                               {:type :session-status :session-id "s3" :status "running" :age-seconds 100}
                               {:type :bead-status :bead-id "proj-ghi" :status "open"}]
                      :whens [{:type :check-zombies}]
                      :thens [{:type :assert-no-zombies}]}
            background {:givens [{:type :project-config :slug "proj" :worker-timeout 3600}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(should= [] (h/zombies))" output)))

    (it "generates pending for mixed recognized/unrecognized steps"
      (let [scenario {:scenario "Mixed"
                      :givens [{:type :session :session-id "s1" :label "project:proj:proj-abc"}
                               {:type :unrecognized :text "some unknown step"}]
                      :whens [{:type :check-zombies}]
                      :thens [{:type :assert-no-zombies}]}
            output (gen/generate-scenario scenario nil)]
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "generates executable code for orch_spawning scenario"
      (let [scenario {:scenario "Spawn workers"
                      :givens [{:type :ready-beads :slug "alpha" :count 3}
                               {:type :active-workers :slug "alpha" :count 0}]
                      :whens [{:type :orch-tick}]
                      :thens [{:type :assert-action :expected "spawn"}
                              {:type :assert-spawn-count :count 2}]}
            background {:givens [{:type :project-config :slug "alpha" :max-workers 2}
                                 {:type :active-iteration :slug "alpha" :iteration "003"}]}
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
                      :givens [{:type :ready-beads :slug "alpha" :count 0}
                               {:type :active-workers :slug "alpha" :count 0}]
                      :whens [{:type :orch-tick}]
                      :thens [{:type :assert-action :expected "idle"}
                              {:type :assert-idle-reason :expected "no-ready-beads"}]}
            background {:givens [{:type :project-config :slug "alpha" :max-workers 2}
                                 {:type :active-iteration :slug "alpha" :iteration "003"}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(should= \"idle\" (h/tick-action))" output)
        (should-contain "(should= \"no-ready-beads\" (h/idle-reason))" output)))

    (it "generates executable code for orch-tick-project"
      (let [scenario {:scenario "Idle for beta only"
                      :givens [{:type :no-active-iteration :slug "beta"}]
                      :whens [{:type :orch-tick-project :slug "beta"}]
                      :thens [{:type :assert-action :expected "idle"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/remove-iteration \"beta\")" output)
        (should-contain "(h/orch-tick-project! \"beta\")" output)))

    (it "generates executable code for spawn label assertion"
      (let [scenario {:scenario "Label check"
                      :givens [{:type :ready-bead-with-id :slug "alpha" :bead-id "alpha-abc"}
                               {:type :active-workers :slug "alpha" :count 0}]
                      :whens [{:type :orch-tick}]
                      :thens [{:type :assert-action :expected "spawn"}
                              {:type :assert-spawn-label :expected "project:alpha:alpha-abc"}]}
            background {:givens [{:type :project-config :slug "alpha" :max-workers 2}
                                 {:type :active-iteration :slug "alpha" :iteration "003"}]}
            output (gen/generate-scenario scenario background)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-ready-bead-with-id \"alpha\" \"alpha-abc\")" output)
        (should-contain "(should= \"project:alpha:alpha-abc\" (h/spawn-label))" output)))

    (it "generates executable code for session ID generation scenario"
      (let [scenario {:scenario "Generate deterministic session ID"
                      :givens [{:type :bead :bead-id "proj-abc"}]
                      :whens [{:type :generate-session-id}]
                      :thens [{:type :assert-session-id :expected "braids-proj-abc-worker"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/reset!)" output)
        (should-contain "(h/set-bead-id \"proj-abc\")" output)
        (should-contain "(h/generate-session-id!)" output)
        (should-contain "(should= \"braids-proj-abc-worker\" (h/session-id-result))" output)))

    (it "generates executable code for session ID identical scenario"
      (let [scenario {:scenario "Same bead same ID"
                      :givens [{:type :bead :bead-id "proj-xyz"}]
                      :whens [{:type :generate-session-id-twice}]
                      :thens [{:type :assert-ids-identical}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-bead-id \"proj-xyz\")" output)
        (should-contain "(h/generate-session-id-twice!)" output)
        (should-contain "(should (h/session-ids-identical?))" output)))

    (it "generates executable code for different session IDs scenario"
      (let [scenario {:scenario "Different beads different IDs"
                      :givens [{:type :bead :bead-id "proj-aaa"}
                               {:type :bead :bead-id "proj-bbb"}]
                      :whens [{:type :generate-session-ids-both}]
                      :thens [{:type :assert-ids-different}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-bead-id \"proj-aaa\")" output)
        (should-contain "(h/set-bead-id \"proj-bbb\")" output)
        (should-contain "(h/generate-session-ids-both!)" output)
        (should-contain "(should (h/session-ids-different?))" output)))

    (it "generates executable code for parse session ID scenario"
      (let [scenario {:scenario "Parse session ID"
                      :givens [{:type :session-id-literal :session-id "braids-proj-abc-worker"}]
                      :whens [{:type :parse-session-id}]
                      :thens [{:type :assert-bead-id :expected "proj-abc"}]}
            output (gen/generate-scenario scenario nil)]
        (should-not-contain "pending" output)
        (should-contain "(h/set-session-id-literal \"braids-proj-abc-worker\")" output)
        (should-contain "(h/parse-session-id!)" output)
        (should-contain "(should= \"proj-abc\" (h/parsed-bead-id))" output)))

    (it "generates executable code for bead-no-status"
      (let [scenario {:scenario "No bead status"
                      :givens [{:type :session :session-id "s5" :label "project:proj:proj-mno"}
                               {:type :session-status :session-id "s5" :status "running" :age-seconds 100}
                               {:type :bead-no-status :bead-id "proj-mno"}]
                      :whens [{:type :check-zombies}]
                      :thens [{:type :assert-no-zombies}]}
            background {:givens [{:type :project-config :slug "proj" :worker-timeout 3600}]}
            output (gen/generate-scenario scenario background)]
        ;; bead-no-status means no setup needed — skip that step
        (should-not-contain "bead-status" output)
        (should-not-contain "pending" output))))

  (describe "generate-spec"

    (it "generates a complete spec file with pending for unrecognized steps"
      (let [ir {:source "test_feature.feature"
                :feature "Test feature"
                :scenarios [{:scenario "First test"
                             :givens [{:type :unrecognized :text "a given"}]
                             :whens [{:type :unrecognized :text "an action"}]
                             :thens [{:type :unrecognized :text "a result"}]}]}
            output (gen/generate-spec ir)]
        (should-contain "(ns braids.features.test-feature-spec" output)
        (should-contain "(describe \"Test feature\"" output)
        (should-contain "(context \"First test\"" output)
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "skips @wip scenarios"
      (let [ir {:source "wip_test.feature"
                :feature "WIP test"
                :scenarios [{:scenario "Normal"
                             :givens [{:type :unrecognized :text "a"}]
                             :whens [{:type :unrecognized :text "b"}]
                             :thens [{:type :unrecognized :text "c"}]}
                            {:scenario "WIP one"
                             :givens [{:type :unrecognized :text "a"}]
                             :whens [{:type :unrecognized :text "b"}]
                             :thens [{:type :unrecognized :text "c"}]
                             :wip true}]}
            output (gen/generate-spec ir)]
        (should-contain "(context \"Normal\"" output)
        (should-not-contain "WIP one" output)))

    (it "generates ns with harness require when scenarios have executable code"
      (let [ir {:source "zombie.feature"
                :feature "Zombie"
                :scenarios [{:scenario "Test"
                             :givens [{:type :session :session-id "s1" :label "project:proj:proj-abc"}]
                             :whens [{:type :check-zombies}]
                             :thens [{:type :assert-no-zombies}]}]}
            output (gen/generate-spec ir)]
        (should-contain "[braids.features.harness :as h]" output)))

    (it "does not include harness require when all scenarios are pending"
      (let [ir {:source "test.feature"
                :feature "Test"
                :scenarios [{:scenario "S1"
                             :givens [{:type :unrecognized :text "a"}]
                             :whens [{:type :unrecognized :text "b"}]
                             :thens [{:type :unrecognized :text "c"}]}]}
            output (gen/generate-spec ir)]
        (should-not-contain "harness" output)))

    (it "generates valid Clojure that can be read"
      (let [ir {:source "readable.feature"
                :feature "Readable spec"
                :scenarios [{:scenario "Test one"
                             :givens [{:type :unrecognized :text "step"}]
                             :whens [{:type :unrecognized :text "action"}]
                             :thens [{:type :unrecognized :text "result"}]}]}
            output (gen/generate-spec ir)]
        ;; Should be parseable as Clojure forms
        (should-not-throw
          (read-string (str "[" output "]"))))))

  (describe "generate-spec with real IR"

    ;; Note: These tests require running bb parse:features first to generate the .edn IR files
    ;; with the new typed IR format.

    (it "generates spec from orch_spawning IR with all executable scenarios"
      (let [ir (read-string (slurp "spec/features/edn/orch_spawning.edn"))
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
      (let [ir (read-string (slurp "spec/features/edn/worker_session_tracking.edn"))
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
      (let [ir (read-string (slurp "spec/features/edn/zombie_detection.edn"))
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

  (describe "generate-features!"

    (it "writes generated spec files to output directory"
      (let [tmp-dir (str "/tmp/gen-test-" (System/currentTimeMillis))
            edn-dir "spec/features/edn"]
        (gen/generate-features! edn-dir tmp-dir)
        (let [files (->> (io/file tmp-dir) .listFiles (map #(.getName %)) sort vec)]
          (should= ["orch_spawning_spec.clj"
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
