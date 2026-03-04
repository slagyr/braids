(ns braids.orch-runner-test
  (:require [speclj.core :refer :all]
            [braids.orch :as orch]))

(describe "orch/build-worker-spawn"
  (it "includes agent-id, model, and thinking in spawn data"
    (let [cfg {:worker-agent "scrapper"
               :worker-model "grok"
               :worker-thinking :high}
          spawn (orch/build-worker-spawn cfg {:path "/tmp/test" :bead "test-bead" :iteration "001" :channel "123"})]
      (should= "scrapper" (:agent-id spawn))
      (should= "grok" (:model spawn))
      (should= :high (:thinking spawn))))

  (it "defaults thinking to :high when not specified"
    (let [cfg {:worker-agent "scrapper"}
          spawn (orch/build-worker-spawn cfg {:path "/tmp/test" :bead "test-bead" :iteration "001" :channel "123"})]
      (should= :high (:thinking spawn))))

  (it "handles nil model and thinking"
    (let [cfg {:worker-agent "scrapper"}
          spawn (orch/build-worker-spawn cfg {:path "/tmp/test" :bead "test-bead" :iteration "001" :channel "123"})]
      (should= nil (:model spawn))
      (should= :high (:thinking spawn)))))