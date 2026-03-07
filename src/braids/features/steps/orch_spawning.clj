(ns braids.features.steps.orch-spawning)

(def step-patterns
  {:given [[#"^project \"([^\"]+)\" has (\d+) active workers?$"
            (fn [[_ slug count]]
              {:pattern :active-workers :slug slug :count (parse-long count)})]]

   :when  [[#"^the orchestrator ticks$"
            (fn [_] {:pattern :orch-tick})]]

   :then  [[#"^the action should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-action :expected expected})]

           [#"^the idle reason should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-idle-reason :expected expected})]]})

(def step-registry
  {:active-workers     {:text (fn [{:keys [slug count]}]            (str "project \"" slug "\" has " count " active workers"))
                        :code (fn [{:keys [slug count]}]            (str "(h/set-active-workers \"" slug "\" " count ")"))}
   :orch-tick          {:text (constantly "the orchestrator ticks")
                        :code (constantly "(h/orch-tick!)")}
   :assert-action      {:text (fn [{:keys [expected]}]              (str "the action should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/tick-action))"))}
   :assert-idle-reason {:text (fn [{:keys [expected]}]              (str "the idle reason should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/idle-reason))"))}})
