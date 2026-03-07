(ns braids.features.steps.orch-spawning)

(defn- project-config-text [{:keys [slug worker-timeout max-workers]}]
  (cond
    worker-timeout (str "a project \"" slug "\" with worker-timeout " worker-timeout)
    max-workers    (str "a project \"" slug "\" with max-workers " max-workers)
    :else          (str "a project \"" slug "\"")))

(defn- project-config-code [{:keys [slug worker-timeout max-workers]}]
  (cond
    worker-timeout (str "(h/add-project-config \"" slug "\" {:worker-timeout " worker-timeout "})")
    max-workers    (str "(h/add-project \"" slug "\" {:max-workers " max-workers "})")
    :else          nil))

(def step-patterns
  {:given [[#"^a project \"([^\"]+)\" with max-workers (\d+)$"
            (fn [[_ slug max-w]]
              {:pattern :project-config :slug slug :max-workers (parse-long max-w)})]

           [#"^project \"([^\"]+)\" has an active iteration \"([^\"]+)\"$"
            (fn [[_ slug iteration]]
              {:pattern :active-iteration :slug slug :iteration iteration})]

           [#"^project \"([^\"]+)\" has no active iteration$"
            (fn [[_ slug]]
              {:pattern :no-active-iteration :slug slug})]

           [#"^project \"([^\"]+)\" has (\d+) ready beads? with id \"([^\"]+)\"$"
            (fn [[_ slug _count bead-id]]
              {:pattern :ready-bead-with-id :slug slug :bead-id bead-id})]

           [#"^project \"([^\"]+)\" has (\d+) ready beads?$"
            (fn [[_ slug count]]
              {:pattern :ready-beads :slug slug :count (parse-long count)})]

           [#"^project \"([^\"]+)\" has (\d+) active workers?$"
            (fn [[_ slug count]]
              {:pattern :active-workers :slug slug :count (parse-long count)})]]

   :when  [[#"^the orchestrator ticks$"
            (fn [_] {:pattern :orch-tick})]

           [#"^the orchestrator ticks for project \"([^\"]+)\" only$"
            (fn [[_ slug]]
              {:pattern :orch-tick-project :slug slug})]]

   :then  [[#"^the action should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-action :expected expected})]

           [#"^(\d+) workers? should be spawned$"
            (fn [[_ count]]
              {:pattern :assert-spawn-count :count (parse-long count)})]

           [#"^the idle reason should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-idle-reason :expected expected})]

           [#"^the spawn label should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-spawn-label :expected expected})]]})

(def step-registry
  {:project-config     {:text project-config-text
                        :code project-config-code}
   :active-iteration   {:text (fn [{:keys [slug iteration]}]        (str "project \"" slug "\" has an active iteration \"" iteration "\""))
                        :code (fn [{:keys [slug iteration]}]        (str "(h/set-active-iteration \"" slug "\" \"" iteration "\")"))}
   :no-active-iteration {:text (fn [{:keys [slug]}]                 (str "project \"" slug "\" has no active iteration"))
                          :code (fn [{:keys [slug]}]                 (str "(h/remove-iteration \"" slug "\")"))}
   :ready-beads        {:text (fn [{:keys [slug count]}]            (str "project \"" slug "\" has " count " ready beads"))
                        :code (fn [{:keys [slug count]}]            (str "(h/set-ready-beads \"" slug "\" " count ")"))}
   :ready-bead-with-id {:text (fn [{:keys [slug bead-id]}]          (str "project \"" slug "\" has 1 ready bead with id \"" bead-id "\""))
                        :code (fn [{:keys [slug bead-id]}]          (str "(h/set-ready-bead-with-id \"" slug "\" \"" bead-id "\")"))}
   :active-workers     {:text (fn [{:keys [slug count]}]            (str "project \"" slug "\" has " count " active workers"))
                        :code (fn [{:keys [slug count]}]            (str "(h/set-active-workers \"" slug "\" " count ")"))}
   :orch-tick          {:text (constantly "the orchestrator ticks")
                        :code (constantly "(h/orch-tick!)")}
   :orch-tick-project  {:text (fn [{:keys [slug]}]                  (str "the orchestrator ticks for project \"" slug "\" only"))
                        :code (fn [{:keys [slug]}]                  (str "(h/orch-tick-project! \"" slug "\")"))}
   :assert-action      {:text (fn [{:keys [expected]}]              (str "the action should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/tick-action))"))}
   :assert-spawn-count {:text (fn [{:keys [count]}]                 (str count " workers should be spawned"))
                        :code (fn [{:keys [count]}]                 (str "(should= " count " (h/spawn-count))"))}
   :assert-idle-reason {:text (fn [{:keys [expected]}]              (str "the idle reason should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/idle-reason))"))}
   :assert-spawn-label {:text (fn [{:keys [expected]}]              (str "the spawn label should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/spawn-label))"))}})
