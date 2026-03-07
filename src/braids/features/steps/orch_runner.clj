(ns braids.features.steps.orch-runner
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^a spawn entry with path \"([^\"]+)\" and bead \"([^\"]+)\"$"
            (fn [[_ path bead]]
              {:pattern :spawn-entry-path-bead :path path :bead bead})]

           [#"^iteration \"([^\"]+)\" and channel \"([^\"]+)\"$"
            (fn [[_ iteration channel]]
              {:pattern :spawn-iteration-channel :iteration iteration :channel channel})]

           [#"^a spawn entry with bead \"([^\"]+)\"$"
            (fn [[_ bead]]
              {:pattern :spawn-entry-bead :bead bead})]

           [#"^no custom worker agent$"
            (fn [_] {:pattern :no-worker-agent})]

           [#"^worker agent \"([^\"]+)\"$"
            (fn [[_ agent]]
              {:pattern :worker-agent :agent agent})]

           [#"^no CLI arguments$"
            (fn [_] {:pattern :no-cli-args})]

           [#"^CLI arguments \"([^\"]+)\"$"
            (fn [[_ args]]
              {:pattern :cli-args :args args})]

           [#"^a spawn tick result with (\d+) workers?$"
            (fn [[_ count]]
              {:pattern :spawn-tick-result :count (parse-long count)})]

           [#"^beads \"([^\"]+)\" and \"([^\"]+)\"$"
            (fn [[_ b1 b2]]
              {:pattern :spawn-beads :beads [b1 b2]})]

           [#"^an idle tick result with reason \"([^\"]+)\"$"
            (fn [[_ reason]]
              {:pattern :idle-tick-result :reason reason})]

           [#"^(\d+) zombie sessions with reasons \"([^\"]+)\" and \"([^\"]+)\"$"
            (fn [[_ count r1 r2]]
              {:pattern :zombie-sessions :count (parse-long count) :reasons [r1 r2]})]]

   :when  [[#"^building the worker task$"
            (fn [_] {:pattern :build-worker-task})]

           [#"^building the worker args$"
            (fn [_] {:pattern :build-worker-args})]

           [#"^parsing CLI args$"
            (fn [_] {:pattern :parse-cli-args})]

           [#"^formatting the spawn log$"
            (fn [_] {:pattern :format-spawn-log})]

           [#"^formatting the idle log$"
            (fn [_] {:pattern :format-idle-log})]

           [#"^formatting the zombie log$"
            (fn [_] {:pattern :format-zombie-log})]]

   :then  [[#"^the task should contain \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-task-contains :expected expected})]

           [#"^the args should include \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-args-include :expected expected})]

           [#"^the args should not include \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-args-not-include :expected expected})]

           [#"^the agent value should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-agent-value :expected expected})]

           [#"^dry-run should be (true|false)$"
            (fn [[_ val]]
              {:pattern :assert-dry-run :expected (= val "true")})]

           [#"^verbose should be (true|false)$"
            (fn [[_ val]]
              {:pattern :assert-verbose :expected (= val "true")})]

           [#"^parsing should return an error$"
            (fn [_] {:pattern :assert-parse-error})]

           [#"^the error should contain \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-error-contains :expected expected})]

           [#"^the log should contain \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-log-contains :expected expected})]]})

(def step-registry
  {:spawn-entry-path-bead {:text (fn [{:keys [path bead]}]
                                   (str "a spawn entry with path \"" path "\" and bead \"" bead "\""))
                           :code (fn [{:keys [path bead]}]
                                   (str "(h/set-spawn-entry {:path \"" path "\" :bead \"" bead "\"})"))}
   :spawn-iteration-channel {:text (fn [{:keys [iteration channel]}]
                                     (str "iteration \"" iteration "\" and channel \"" channel "\""))
                             :code (fn [{:keys [iteration channel]}]
                                     (str "(h/update-spawn-entry {:iteration \"" iteration "\" :channel \"" channel "\"})"))}
   :spawn-entry-bead     {:text (fn [{:keys [bead]}]
                                  (str "a spawn entry with bead \"" bead "\""))
                          :code (fn [{:keys [bead]}]
                                  (str "(h/set-spawn-entry {:bead \"" bead "\"})"))}
   :no-worker-agent      {:text (constantly "no custom worker agent")
                          :code (constantly nil)}
   :worker-agent         {:text (fn [{:keys [agent]}]
                                  (str "worker agent \"" agent "\""))
                          :code (fn [{:keys [agent]}]
                                  (str "(h/set-worker-agent \"" agent "\")"))}
   :no-cli-args          {:text (constantly "no CLI arguments")
                          :code (constantly "(h/set-cli-args [])")}
   :cli-args             {:text (fn [{:keys [args]}]
                                  (str "CLI arguments \"" args "\""))
                          :code (fn [{:keys [args]}]
                                  (str "(h/set-cli-args [\"" args "\"])"))}
   :spawn-tick-result    {:text (fn [{:keys [count]}]
                                  (str "a spawn tick result with " count " workers"))
                          :code (fn [{:keys [count]}]
                                  (str "(h/set-spawn-tick-result " count " [])"))}
   :spawn-beads          {:text (fn [{:keys [beads]}]
                                  (let [[b1 b2] beads]
                                    (str "beads \"" b1 "\" and \"" b2 "\"")))
                          :code (fn [{:keys [beads]}]
                                  (str "(h/add-spawn-beads " (pr-str beads) ")"))}
   :idle-tick-result     {:text (fn [{:keys [reason]}]
                                  (str "an idle tick result with reason \"" reason "\""))
                          :code (fn [{:keys [reason]}]
                                  (str "(h/set-idle-tick-result \"" reason "\")"))}
   :zombie-sessions      {:text (fn [{:keys [count reasons]}]
                                  (let [[r1 r2] reasons]
                                    (str count " zombie sessions with reasons \"" r1 "\" and \"" r2 "\"")))
                          :code (fn [{:keys [count reasons]}]
                                  (str "(h/set-zombie-sessions " count " " (pr-str reasons) ")"))}
   :build-worker-task    {:text (constantly "building the worker task")
                          :code (constantly "(h/build-worker-task!)")}
   :build-worker-args    {:text (constantly "building the worker args")
                          :code (constantly "(h/build-worker-args!)")}
   :parse-cli-args       {:text (constantly "parsing CLI args")
                          :code (constantly "(h/parse-cli-args!)")}
   :format-spawn-log     {:text (constantly "formatting the spawn log")
                          :code (constantly "(h/format-spawn-log!)")}
   :format-idle-log      {:text (constantly "formatting the idle log")
                          :code (constantly "(h/format-idle-log!)")}
   :format-zombie-log    {:text (constantly "formatting the zombie log")
                          :code (constantly "(h/format-zombie-log!)")}
   :assert-task-contains {:text (fn [{:keys [expected]}]
                                  (str "the task should contain \"" expected "\""))
                          :code (fn [{:keys [expected]}]
                                  (str "(should (clojure.string/includes? (h/worker-task) \"" expected "\"))"))}
   :assert-args-include  {:text (fn [{:keys [expected]}]
                                  (str "the args should include \"" expected "\""))
                          :code (fn [{:keys [expected]}]
                                  (str "(should (some #(= \"" expected "\" %) (h/worker-args)))"))}
   :assert-args-not-include {:text (fn [{:keys [expected]}]
                                     (str "the args should not include \"" expected "\""))
                             :code (fn [{:keys [expected]}]
                                     (str "(should-not (some #(= \"" expected "\" %) (h/worker-args)))"))}
   :assert-agent-value   {:text (fn [{:keys [expected]}]
                                  (str "the agent value should be \"" expected "\""))
                          :code (fn [{:keys [expected]}]
                                  (str "(let [args (h/worker-args)\n"
                                       "      idx (.indexOf args \"--agent\")]\n"
                                       "  (should (>= idx 0))\n"
                                       "  (should= \"" expected "\" (nth args (inc idx))))"))}
   :assert-dry-run       {:text (fn [{:keys [expected]}]
                                  (str "dry-run should be " expected))
                          :code (fn [{:keys [expected]}]
                                  (str "(should= " expected " (:dry-run (h/parsed-cli-args)))"))}
   :assert-verbose       {:text (fn [{:keys [expected]}]
                                  (str "verbose should be " expected))
                          :code (fn [{:keys [expected]}]
                                  (str "(should= " expected " (:verbose (h/parsed-cli-args)))"))}
   :assert-parse-error   {:text (constantly "parsing should return an error")
                          :code (constantly "(should (:error (h/parsed-cli-args)))")}
   :assert-error-contains {:text (fn [{:keys [expected]}]
                                   (str "the error should contain \"" expected "\""))
                           :code (fn [{:keys [expected]}]
                                   (str "(should (clojure.string/includes? (:error (h/parsed-cli-args)) \"" expected "\"))"))}
   :assert-log-contains  {:text (fn [{:keys [expected]}]
                                  (str "the log should contain \"" expected "\""))
                          :code (fn [{:keys [expected]}]
                                  (str "(should (some #(clojure.string/includes? % \"" expected "\") (h/runner-log)))"))}})
