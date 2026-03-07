(ns braids.features.steps.ready-beads
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^a registry with projects:$"
            (fn [_] {:pattern :registry-with-projects-table})]

           [#"^project \"([^\"]+)\" has config with status \"([^\"]+)\" and max-workers (\d+)$"
            (fn [[_ slug status max-w]]
              {:pattern :project-config-status-and-max-workers :slug slug :status status :max-workers (parse-long max-w)})]

           [#"^project \"([^\"]+)\" has config with max-workers (\d+)$"
            (fn [[_ slug max-w]]
              {:pattern :project-config-max-workers :slug slug :max-workers (parse-long max-w)})]

           [#"^project \"([^\"]+)\" has ready beads:$"
            (fn [[_ slug]]
              {:pattern :project-ready-beads-table :slug slug})]

           [#"^no active workers$"
            (fn [_] {:pattern :no-active-workers})]

           [#"^ready beads to format:$"
            (fn [_] {:pattern :ready-beads-to-format})]

           [#"^no ready beads to format$"
            (fn [_] {:pattern :no-ready-beads-to-format})]]

   :when  [[#"^computing ready beads$"
            (fn [_] {:pattern :compute-ready-beads})]

           [#"^formatting ready output$"
            (fn [_] {:pattern :format-ready-output})]]

   :then  [[#"^the result should contain bead \"([^\"]+)\"$"
            (fn [[_ bead-id]]
              {:pattern :assert-result-contains-bead :bead-id bead-id})]

           [#"^the result should not contain bead \"([^\"]+)\"$"
            (fn [[_ bead-id]]
              {:pattern :assert-result-not-contains-bead :bead-id bead-id})]

           [#"^the result should be empty$"
            (fn [_] {:pattern :assert-result-empty})]

           [#"^the (first|second|third) result should be from project \"([^\"]+)\"$"
            (fn [[_ ordinal slug]]
              {:pattern :assert-nth-result-project
               :position (case ordinal "first" 1 "second" 2 "third" 3)
               :slug slug})]

           [#"^the output should contain \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-output-contains :expected expected})]]})

(def step-registry
  {:registry-with-projects-table
                       {:text (constantly "a registry with projects:")
                        :code (fn [{:keys [table]}]
                                (when table
                                  (let [{:keys [headers rows]} table]
                                    (str "(h/set-registry-from-table\n"
                                         "  " (pr-str headers) "\n"
                                         "  " (pr-str rows) ")"))))}
   :project-config-max-workers
                       {:text (fn [{:keys [slug max-workers]}]
                                (str "project \"" slug "\" has config with max-workers " max-workers))
                        :code (fn [{:keys [slug max-workers]}]
                                (str "(h/set-project-config \"" slug "\" {:max-workers " max-workers "})"))}
   :project-config-status-and-max-workers
                       {:text (fn [{:keys [slug status max-workers]}]
                                (str "project \"" slug "\" has config with status \"" status "\" and max-workers " max-workers))
                        :code (fn [{:keys [slug status max-workers]}]
                                (str "(h/set-project-config \"" slug "\" {:status \"" status "\" :max-workers " max-workers "})"))}
   :project-ready-beads-table
                       {:text (fn [{:keys [slug]}]
                                (str "project \"" slug "\" has ready beads:"))
                        :code (fn [{:keys [slug table]}]
                                (when table
                                  (let [{:keys [headers rows]} table]
                                    (str "(h/set-project-ready-beads \"" slug "\"\n"
                                         "  " (pr-str headers) "\n"
                                         "  " (pr-str rows) ")"))))}
   :no-active-workers {:text (constantly "no active workers")
                       :code (constantly nil)}
   :compute-ready-beads {:text (constantly "computing ready beads")
                         :code (constantly "(h/compute-ready-beads!)")}
   :assert-result-contains-bead
                       {:text (fn [{:keys [bead-id]}]
                                (str "the result should contain bead \"" bead-id "\""))
                        :code (fn [{:keys [bead-id]}]
                                (str "(should (h/result-contains-bead? \"" bead-id "\"))"))}
   :assert-result-not-contains-bead
                       {:text (fn [{:keys [bead-id]}]
                                (str "the result should not contain bead \"" bead-id "\""))
                        :code (fn [{:keys [bead-id]}]
                                (str "(should-not (h/result-contains-bead? \"" bead-id "\"))"))}
   :assert-result-empty {:text (constantly "the result should be empty")
                         :code (constantly "(should (empty? (h/ready-result)))")}
   :assert-nth-result-project
                       {:text (fn [{:keys [position slug]}]
                                (let [ordinal (case position 1 "first" 2 "second" 3 "third")]
                                  (str "the " ordinal " result should be from project \"" slug "\"")))
                        :code (fn [{:keys [position slug]}]
                                (str "(should= \"" slug "\" (:project (nth (h/ready-result) " (dec position) ")))"))}
   :ready-beads-to-format
                       {:text (constantly "ready beads to format:")
                        :code (fn [{:keys [table]}]
                                (when table
                                  (let [{:keys [headers rows]} table]
                                    (str "(h/set-ready-beads-to-format\n"
                                         "  " (pr-str headers) "\n"
                                         "  " (pr-str rows) ")"))))}
   :no-ready-beads-to-format
                       {:text (constantly "no ready beads to format")
                        :code (constantly "(h/set-no-ready-beads-to-format)")}
   :format-ready-output {:text (constantly "formatting ready output")
                         :code (constantly "(h/format-ready-output!)")}
   :assert-output-contains
                       {:text (fn [{:keys [expected]}]
                                (str "the output should contain \"" expected "\""))
                        :code (fn [{:keys [expected]}]
                                (str "(should (clojure.string/includes? (h/output) \"" expected "\"))"))}})
