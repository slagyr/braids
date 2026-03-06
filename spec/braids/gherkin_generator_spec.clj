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

  (describe "generate-step-comments"

    (it "generates Given comments"
      (let [comments (gen/generate-step-comments
                       {:givens ["a step" "another step"]
                        :whens [] :thens []}
                       nil)]
        (should-contain ";; Given a step" comments)
        (should-contain ";; And another step" comments)))

    (it "generates When comments"
      (let [comments (gen/generate-step-comments
                       {:givens [] :whens ["something happens"] :thens []}
                       nil)]
        (should-contain ";; When something happens" comments)))

    (it "generates Then comments"
      (let [comments (gen/generate-step-comments
                       {:givens [] :whens [] :thens ["a result" "another result"]}
                       nil)]
        (should-contain ";; Then a result" comments)
        (should-contain ";; And another result" comments)))

    (it "includes background steps as comments before scenario steps"
      (let [background {:givens ["common setup" "other setup"]}
            comments (gen/generate-step-comments
                       {:givens ["scenario step"] :whens ["action"] :thens ["result"]}
                       background)]
        (should-contain ";; Background:" comments)
        (should-contain ";; Given common setup" comments)
        (should-contain ";; And other setup" comments)
        ;; Background should come before scenario steps
        (should (< (str/index-of comments "Background:")
                   (str/index-of comments "scenario step"))))))

  (describe "generate-scenario"

    (it "generates a context with pending it block"
      (let [scenario {:scenario "Simple test"
                      :givens ["a step"]
                      :whens ["action"]
                      :thens ["result"]}
            output (gen/generate-scenario scenario nil)]
        (should-contain "(context \"Simple test\"" output)
        (should-contain "(it \"Simple test\"" output)
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "includes step comments in the it block"
      (let [scenario {:scenario "Test"
                      :givens ["first given"]
                      :whens ["the action"]
                      :thens ["expected result"]}
            output (gen/generate-scenario scenario nil)]
        (should-contain ";; Given first given" output)
        (should-contain ";; When the action" output)
        (should-contain ";; Then expected result" output)))

    (it "includes background comments when present"
      (let [scenario {:scenario "With bg"
                      :givens ["scenario step"]
                      :whens ["action"]
                      :thens ["result"]}
            background {:givens ["bg step"]}
            output (gen/generate-scenario scenario background)]
        (should-contain ";; Background:" output)
        (should-contain ";; Given bg step" output))))

  (describe "generate-spec"

    (it "generates a complete spec file from IR"
      (let [ir {:source "test_feature.feature"
                :feature "Test feature"
                :scenarios [{:scenario "First test"
                             :givens ["a given"]
                             :whens ["an action"]
                             :thens ["a result"]}]}
            output (gen/generate-spec ir)]
        (should-contain "(ns braids.features.test-feature-spec" output)
        (should-contain "(describe \"Test feature\"" output)
        (should-contain "(context \"First test\"" output)
        (should-contain "(pending \"not yet implemented\")" output)))

    (it "skips @wip scenarios"
      (let [ir {:source "wip_test.feature"
                :feature "WIP test"
                :scenarios [{:scenario "Normal" :givens ["a"] :whens ["b"] :thens ["c"]}
                            {:scenario "WIP one" :givens ["a"] :whens ["b"] :thens ["c"] :wip true}]}
            output (gen/generate-spec ir)]
        (should-contain "(context \"Normal\"" output)
        (should-not-contain "WIP one" output)))

    (it "includes background in every scenario"
      (let [ir {:source "bg.feature"
                :feature "Background test"
                :background {:givens ["common step"]}
                :scenarios [{:scenario "S1" :givens ["s1 step"] :whens ["a"] :thens ["b"]}
                            {:scenario "S2" :givens ["s2 step"] :whens ["c"] :thens ["d"]}]}
            output (gen/generate-spec ir)]
        ;; Background should appear in both scenarios
        (should= 2 (count (re-seq #"Background:" output)))))

    (it "generates valid Clojure that can be read"
      (let [ir {:source "readable.feature"
                :feature "Readable spec"
                :scenarios [{:scenario "Test one"
                             :givens ["step"]
                             :whens ["action"]
                             :thens ["result"]}]}
            output (gen/generate-spec ir)]
        ;; Should be parseable as Clojure forms
        (should-not-throw
          (read-string (str "[" output "]"))))))

  (describe "generate-spec with real IR"

    (it "generates spec from orch_spawning IR"
      (let [ir (read-string (slurp "spec/features/edn/orch_spawning.edn"))
            output (gen/generate-spec ir)]
        (should-contain "(ns braids.features.orch-spawning-spec" output)
        (should-contain "(describe \"Orchestrator spawning behavior\"" output)
        ;; All 7 scenarios should be present
        (should= 7 (count (re-seq #"\(context " output)))))

    (it "generates spec from worker_session_tracking IR skipping wip"
      (let [ir (read-string (slurp "spec/features/edn/worker_session_tracking.edn"))
            output (gen/generate-spec ir)]
        ;; 6 total scenarios, 2 are @wip, so 4 should be generated
        (should= 4 (count (re-seq #"\(context " output)))
        (should-not-contain "Prevent duplicate spawning" output)
        (should-not-contain "Session with missing bead data" output)))

    (it "generates spec from zombie_detection IR skipping wip"
      (let [ir (read-string (slurp "spec/features/edn/zombie_detection.edn"))
            output (gen/generate-spec ir)]
        ;; 7 total scenarios, 2 are @wip, so 5 should be generated
        (should= 5 (count (re-seq #"\(context " output)))
        (should-not-contain "Zombie cleanup kills" output)
        (should-not-contain "Zombie detection across multiple" output))))

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
