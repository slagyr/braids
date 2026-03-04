(ns braids.orch-runner-test
  (:require [clojure.test :refer :all]
            [braids.orch-runner :as orch]))

(deftest build-worker-args-uses-config-thinking
  (let [config {:worker-thinking "high"}
        spawn {:bead "braids-123" :path "/tmp" :iteration "001" :channel "123"}
        args (orch/build-worker-args config spawn)]
    (is (some #{"--thinking"} args))
    (is (some #{"high"} args))))

(deftest build-worker-args-default-thinking
  (let [config {}
        spawn {:bead "braids-123" :path "/tmp" :iteration "001" :channel "123"}
        args (orch/build-worker-args config spawn)]
    (is (some #{"--thinking"} args))
    (is (some #{"high"} args))))