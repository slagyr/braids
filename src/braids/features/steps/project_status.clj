(ns braids.features.steps.project-status
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^project configs:$"
            (fn [_] {:pattern :project-configs-table})]

           [#"^active iterations:$"
            (fn [_] {:pattern :active-iterations-table})]

           [#"^active workers:$"
            (fn [_] {:pattern :active-workers-table})]

           [#"^no active iterations$"
            (fn [_] {:pattern :no-active-iterations})]

           [#"^a dashboard project \"([^\"]+)\" with:$"
            (fn [[_ slug]]
              {:pattern :dashboard-project :slug slug})]

           [#"^project \"([^\"]+)\" has iteration:$"
            (fn [[_ slug]]
              {:pattern :project-has-iteration :slug slug})]

           [#"^project \"([^\"]+)\" has stories:$"
            (fn [[_ slug]]
              {:pattern :project-has-stories :slug slug})]

           [#"^project \"([^\"]+)\" has no iteration$"
            (fn [[_ slug]]
              {:pattern :project-has-no-iteration :slug slug})]

           [#"^an empty registry$"
            (fn [_] {:pattern :empty-registry})]]

   :when  [[#"^building the dashboard$"
            (fn [_] {:pattern :build-dashboard})]

           [#"^formatting project detail for \"([^\"]+)\"$"
            (fn [[_ slug]]
              {:pattern :format-project-detail :slug slug})]

           [#"^formatting the dashboard as JSON$"
            (fn [_] {:pattern :format-dashboard-json})]

           [#"^formatting the dashboard$"
            (fn [_] {:pattern :format-dashboard})]]

   :then  [[#"^the dashboard should have (\d+) projects?$"
            (fn [[_ count]]
              {:pattern :assert-dashboard-project-count :count (parse-long count)})]

           [#"^project \"([^\"]+)\" should have status \"([^\"]+)\"$"
            (fn [[_ slug expected]]
              {:pattern :assert-project-status :slug slug :expected expected})]

           [#"^project \"([^\"]+)\" should have iteration number \"([^\"]+)\"$"
            (fn [[_ slug expected]]
              {:pattern :assert-project-iteration-number :slug slug :expected expected})]

           [#"^project \"([^\"]+)\" should have workers (\d+) of (\d+)$"
            (fn [[_ slug workers max-workers]]
              {:pattern :assert-project-workers :slug slug :workers (parse-long workers) :max-workers (parse-long max-workers)})]

           [#"^project \"([^\"]+)\" should have no iteration$"
            (fn [[_ slug]]
              {:pattern :assert-project-no-iteration :slug slug})]

           [#"^the JSON should contain (\d+) projects?$"
            (fn [[_ count]]
              {:pattern :assert-json-project-count :count (parse-long count)})]

           [#"^the JSON project \"([^\"]+)\" should have iteration percent (\d+)$"
            (fn [[_ slug percent]]
              {:pattern :assert-json-project-iteration-percent :slug slug :percent (parse-long percent)})]]})

(def step-registry
  {:project-configs-table {:text (constantly "project configs:")
                           :code (fn [{:keys [table]}]
                                   (when table
                                     (let [{:keys [headers rows]} table]
                                       (str "(h/set-project-configs-from-table\n"
                                            "  " (pr-str headers) "\n"
                                            "  " (pr-str rows) ")"))))}
   :active-iterations-table {:text (constantly "active iterations:")
                             :code (fn [{:keys [table]}]
                                     (when table
                                       (let [{:keys [headers rows]} table]
                                         (str "(h/set-active-iterations-from-table\n"
                                              "  " (pr-str headers) "\n"
                                              "  " (pr-str rows) ")"))))}
   :active-workers-table {:text (constantly "active workers:")
                          :code (fn [{:keys [table]}]
                                  (when table
                                    (let [{:keys [headers rows]} table]
                                      (str "(h/set-active-workers-from-table\n"
                                           "  " (pr-str headers) "\n"
                                           "  " (pr-str rows) ")"))))}
   :no-active-iterations {:text (constantly "no active iterations")
                          :code (constantly nil)}
   :build-dashboard      {:text (constantly "building the dashboard")
                          :code (constantly "(h/build-dashboard!)")}
   :assert-dashboard-project-count
                         {:text (fn [{:keys [count]}]
                                  (str "the dashboard should have " count " projects"))
                          :code (fn [{:keys [count]}]
                                  (str "(should= " count " (count (:projects (h/dashboard))))"))}
   :assert-project-status {:text (fn [{:keys [slug expected]}]
                                   (str "project \"" slug "\" should have status \"" expected "\""))
                           :code (fn [{:keys [slug expected]}]
                                   (str "(should= \"" expected "\" (:status (h/dashboard-project \"" slug "\")))"))}
   :assert-project-iteration-number
                         {:text (fn [{:keys [slug expected]}]
                                  (str "project \"" slug "\" should have iteration number \"" expected "\""))
                          :code (fn [{:keys [slug expected]}]
                                  (str "(should= \"" expected "\" (get-in (h/dashboard-project \"" slug "\") [:iteration :number]))"))}
   :assert-project-workers {:text (fn [{:keys [slug workers max-workers]}]
                                    (str "project \"" slug "\" should have workers " workers " of " max-workers))
                            :code (fn [{:keys [slug workers max-workers]}]
                                    (str "(should= " workers " (:workers (h/dashboard-project \"" slug "\")))\n"
                                         "(should= " max-workers " (:max-workers (h/dashboard-project \"" slug "\")))"))}
   :assert-project-no-iteration
                         {:text (fn [{:keys [slug]}]
                                  (str "project \"" slug "\" should have no iteration"))
                          :code (fn [{:keys [slug]}]
                                  (str "(should-be-nil (:iteration (h/dashboard-project \"" slug "\")))"))}
   :dashboard-project    {:text (fn [{:keys [slug]}]
                                  (str "a dashboard project \"" slug "\" with:"))
                          :code (fn [{:keys [slug table]}]
                                  (when table
                                    (let [{:keys [headers rows]} table]
                                      (str "(h/set-dashboard-project \"" slug "\"\n"
                                           "  " (pr-str headers) "\n"
                                           "  " (pr-str rows) ")"))))}
   :project-has-iteration {:text (fn [{:keys [slug]}]
                                   (str "project \"" slug "\" has iteration:"))
                           :code (fn [{:keys [slug table]}]
                                   (when table
                                     (let [{:keys [headers rows]} table]
                                       (str "(h/set-project-iteration \"" slug "\"\n"
                                            "  " (pr-str headers) "\n"
                                            "  " (pr-str rows) ")"))))}
   :project-has-stories  {:text (fn [{:keys [slug]}]
                                  (str "project \"" slug "\" has stories:"))
                          :code (fn [{:keys [slug table]}]
                                  (when table
                                    (let [{:keys [headers rows]} table]
                                      (str "(h/set-project-stories \"" slug "\"\n"
                                           "  " (pr-str headers) "\n"
                                           "  " (pr-str rows) ")"))))}
   :project-has-no-iteration {:text (fn [{:keys [slug]}]
                                      (str "project \"" slug "\" has no iteration"))
                              :code (fn [{:keys [slug]}]
                                      (str "(h/clear-project-iteration \"" slug "\")"))}
   :format-project-detail {:text (fn [{:keys [slug]}]
                                   (str "formatting project detail for \"" slug "\""))
                           :code (fn [{:keys [slug]}]
                                   (str "(h/format-project-detail! \"" slug "\")"))}
   :format-dashboard-json {:text (constantly "formatting the dashboard as JSON")
                           :code (constantly "(h/format-dashboard-json!)")}
   :format-dashboard     {:text (constantly "formatting the dashboard")
                          :code (constantly "(h/format-dashboard!)")}
   :assert-json-project-count
                         {:text (fn [{:keys [count]}]
                                  (str "the JSON should contain " count " project"))
                          :code (fn [{:keys [count]}]
                                  (str "(should= " count " (count (:projects (h/dashboard-json))))"))}
   :assert-json-project-iteration-percent
                         {:text (fn [{:keys [slug percent]}]
                                  (str "the JSON project \"" slug "\" should have iteration percent " percent))
                          :code (fn [{:keys [slug percent]}]
                                  (str "(should= " percent " (get-in (h/dashboard-json-project \"" slug "\") [\"iteration\" \"stats\" \"percent\"]))"))}
   :empty-registry       {:text (constantly "an empty registry")
                          :code (constantly "(h/set-empty-registry)")}})
