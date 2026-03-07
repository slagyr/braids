(ns braids.features.steps.zombie-detection)

(def step-patterns
  {:given [[#"^a project \"([^\"]+)\" with worker-timeout (\d+)$"
            (fn [[_ slug timeout]]
              {:pattern :project-config :slug slug :worker-timeout (parse-long timeout)})]

           [#"^a session \"([^\"]+)\" with label \"([^\"]+)\"$"
            (fn [[_ session-id label]]
              {:pattern :session :session-id session-id :label label})]

           [#"^session \"([^\"]+)\" has status \"([^\"]+)\" and age (\d+) seconds$"
            (fn [[_ session-id status age]]
              {:pattern :session-status :session-id session-id :status status :age-seconds (parse-long age)})]

           [#"^bead \"([^\"]+)\" has status \"([^\"]+)\"$"
            (fn [[_ bead-id status]]
              {:pattern :bead-status :bead-id bead-id :status status})]

           [#"^bead \"([^\"]+)\" has no recorded status$"
            (fn [[_ bead-id]]
              {:pattern :bead-no-status :bead-id bead-id})]]

   :when  [[#"^checking for zombies$"
             (fn [_] {:pattern :check-zombies})]]

   :then  [[#"^session \"([^\"]+)\" should be a zombie with reason \"([^\"]+)\"$"
             (fn [[_ session-id reason]]
               {:pattern :assert-zombie :session-id session-id :reason reason})]

            [#"^no zombies should be detected$"
             (fn [_] {:pattern :assert-no-zombies})]]})

(def step-registry
  {:session            {:text (fn [{:keys [session-id label]}]       (str "a session \"" session-id "\" with label \"" label "\""))
                        :code (fn [{:keys [session-id label]}]       (str "(h/add-session \"" session-id "\" {:label \"" label "\"})"))}
   :session-status     {:text (fn [{:keys [session-id status age-seconds]}] (str "session \"" session-id "\" has status \"" status "\" and age " age-seconds " seconds"))
                        :code (fn [{:keys [session-id status age-seconds]}] (str "(h/set-session-status \"" session-id "\" \"" status "\" " age-seconds ")"))}
   :bead-status        {:text (fn [{:keys [bead-id status]}]         (str "bead \"" bead-id "\" has status \"" status "\""))
                        :code (fn [{:keys [bead-id status]}]         (str "(h/set-bead-status \"" bead-id "\" \"" status "\")"))}
   :bead-no-status     {:text (fn [{:keys [bead-id]}]               (str "bead \"" bead-id "\" has no recorded status"))
                        :code (constantly nil)}
   :check-zombies      {:text (constantly "checking for zombies")
                        :code (constantly "(h/check-zombies!)")}
   :assert-zombie      {:text (fn [{:keys [session-id reason]}]     (str "session \"" session-id "\" should be a zombie with reason \"" reason "\""))
                        :code (fn [{:keys [session-id reason]}]     (str "(should (h/zombie? \"" session-id "\"))\n(should= \"" reason "\" (h/zombie-reason \"" session-id "\"))"))}
   :assert-no-zombies  {:text (constantly "no zombies should be detected")
                        :code (constantly "(should= [] (h/zombies))")}})
