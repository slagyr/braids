(ns braids.features.steps.orch-output)

(def step-patterns
  {:given [[#"^configured projects:$"
            (fn [_] {:pattern :configured-projects-table})]

           [#"^project \"([^\"]+)\" has beads:$"
            (fn [[_ slug]]
              {:pattern :project-has-beads-table :slug slug})]]

   :when  []

   :then  [[#"^the output contains lines matching$"
            (fn [_] {:pattern :output-contains-lines-matching})]

           [#"^the output does not contain$"
            (fn [_] {:pattern :output-does-not-contain})]

           [#"^the output has \"([^\"]+)\" before \"([^\"]+)\"$"
            (fn [[_ first-text second-text]]
              {:pattern :output-has-before :first first-text :second second-text})]]})

(def step-registry
  {})