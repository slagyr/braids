(ns braids.features.steps.orch-spawning
  (:require [clojure.string :as str]))

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
              {:pattern :assert-idle-reason :expected expected})]

           [#"^the spawns should include$"
            (fn [_] {:pattern :spawns-should-include})]

           [#"^the spawns should not include bead \"([^\"]+)\"$"
            (fn [[_ bead-id]]
              {:pattern :spawns-not-include-bead :bead-id bead-id})]

           [#"^the spawn for \"([^\"]+)\" should not have key \"([^\"]+)\"$"
            (fn [[_ bead-id key]]
              {:pattern :spawn-missing-key :bead-id bead-id :key key})]

           [#"^spawn (\d+) should be for project \"([^\"]+)\"$"
            (fn [[_ index project]]
              {:pattern :spawn-at-project :index (parse-long index) :project project})]]})

(defn- table-row->assertion-code
  "Generate assertion code for a single row of the spawns-should-include table."
  [headers row]
  (let [m (zipmap headers row)
        entries (str/join ", " (map (fn [[k v]] (str "\"" k "\" \"" v "\"")) m))]
    (str "(should (h/spawn-includes? {" entries "}))")))

(def step-registry
  {:active-workers         {:text (fn [{:keys [slug count]}]            (str "project \"" slug "\" has " count " active workers"))
                            :code (fn [{:keys [slug count]}]            (str "(h/set-active-workers \"" slug "\" " count ")"))}
   :orch-tick              {:text (constantly "the orchestrator ticks")
                            :code (constantly "(h/orch-tick!)")}
   :assert-action          {:text (fn [{:keys [expected]}]              (str "the action should be \"" expected "\""))
                            :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/tick-action))"))}
   :assert-idle-reason     {:text (fn [{:keys [expected]}]              (str "the idle reason should be \"" expected "\""))
                            :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/idle-reason))"))}
   :spawns-should-include  {:text (constantly "the spawns should include")
                            :code (fn [{:keys [table]}]
                                    (let [{:keys [headers rows]} table]
                                      (str/join "\n" (map #(table-row->assertion-code headers %) rows))))}
   :spawns-not-include-bead {:text (fn [{:keys [bead-id]}]             (str "the spawns should not include bead \"" bead-id "\""))
                             :code (fn [{:keys [bead-id]}]             (str "(should (h/spawn-excludes-bead? \"" bead-id "\"))"))}
   :spawn-missing-key      {:text (fn [{:keys [bead-id key]}]         (str "the spawn for \"" bead-id "\" should not have key \"" key "\""))
                             :code (fn [{:keys [bead-id key]}]         (str "(should (h/spawn-missing-key? \"" bead-id "\" \"" key "\"))"))}
   :spawn-at-project       {:text (fn [{:keys [index project]}]       (str "spawn " index " should be for project \"" project "\""))
                             :code (fn [{:keys [index project]}]       (str "(should= \"" project "\" (:project (h/spawn-at " index ")))"))}})

