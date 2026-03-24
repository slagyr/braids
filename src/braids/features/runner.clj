(ns braids.features.runner
  (:require [gherclj.pipeline :as pipeline]
            [braids.features.steps.configuration]
            [braids.features.steps.iteration]
            [braids.features.steps.orch-output]
            [braids.features.steps.orch-runner]
            [braids.features.steps.orch-spawning]
            [braids.features.steps.project-lifecycle]
            [braids.features.steps.project-listing]
            [braids.features.steps.project-status]
            [braids.features.steps.ready-beads]
            [braids.features.steps.worker-session]
            [braids.features.steps.zombie-detection]))

(def config
  {:features-dir    "features"
   :edn-dir         "features/edn"
   :output-dir      "features/generated"
   :step-namespaces ['braids.features.steps.configuration
                     'braids.features.steps.iteration
                     'braids.features.steps.orch-output
                     'braids.features.steps.orch-runner
                     'braids.features.steps.orch-spawning
                     'braids.features.steps.project-lifecycle
                     'braids.features.steps.project-listing
                     'braids.features.steps.project-status
                     'braids.features.steps.ready-beads
                     'braids.features.steps.worker-session
                     'braids.features.steps.zombie-detection]
   :harness-ns      'braids.features.harness
   :test-framework  :speclj})

(defn -main [& _args]
  (pipeline/run! config))
