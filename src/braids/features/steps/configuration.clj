(ns braids.features.steps.configuration
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^a config with values:$"
            (fn [_] {:pattern :config-with-values})]

           [#"^an empty config string$"
            (fn [_] {:pattern :empty-config-string})]]

   :when  [[#"^listing the config$"
            (fn [_] {:pattern :list-config})]

           [#"^getting config key \"([^\"]+)\"$"
            (fn [[_ key]]
              {:pattern :get-config-key :key key})]

           [#"^setting config key \"([^\"]+)\" to \"([^\"]+)\"$"
            (fn [[_ key value]]
              {:pattern :set-config-key :key key :value value})]

           [#"^parsing the config$"
            (fn [_] {:pattern :parse-config})]

           [#"^requesting config help$"
            (fn [_] {:pattern :request-config-help})]]

   :then  [[#"^the result should be ok with value \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-result-ok-with-value :expected expected})]

           [#"^the result should be an error$"
            (fn [_] {:pattern :assert-result-error})]

           [#"^the error message should contain \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-error-message-contains :expected expected})]

           [#"^the config should have \"([^\"]+)\" set to \"([^\"]+)\"$"
            (fn [[_ key expected]]
              {:pattern :assert-config-has-value :key key :expected expected})]

           [#"^\"([^\"]+)\" should appear before \"([^\"]+)\" in the output$"
            (fn [[_ first-item second-item]]
              {:pattern :assert-appears-before :first first-item :second second-item})]]})

(def step-registry
  {:config-with-values    {:text (constantly "a config with values:")
                            :code (fn [{:keys [table]}]
                                    (when table
                                      (let [{:keys [headers rows]} table]
                                        (str "(h/set-config-from-table\n"
                                             "  " (pr-str headers) "\n"
                                             "  " (pr-str rows) ")"))))}
   :list-config           {:text (constantly "listing the config")
                            :code (constantly "(h/list-config!)")}
   :get-config-key        {:text (fn [{:keys [key]}]
                                    (str "getting config key \"" key "\""))
                            :code (fn [{:keys [key]}]
                                    (str "(h/get-config-key! \"" key "\")"))}
   :set-config-key        {:text (fn [{:keys [key value]}]
                                    (str "setting config key \"" key "\" to \"" value "\""))
                            :code (fn [{:keys [key value]}]
                                    (str "(h/set-config-key! \"" key "\" \"" value "\")"))}
   :empty-config-string   {:text (constantly "an empty config string")
                            :code (constantly "(h/set-empty-config)")}
   :parse-config          {:text (constantly "parsing the config")
                            :code (constantly "(h/parse-config!)")}
   :request-config-help   {:text (constantly "requesting config help")
                            :code (constantly "(h/request-config-help!)")}
   :assert-result-ok-with-value
                           {:text (fn [{:keys [expected]}]
                                    (str "the result should be ok with value \"" expected "\""))
                            :code (fn [{:keys [expected]}]
                                    (str "(should= \"" expected "\" (:ok (h/config-result)))"))}
   :assert-result-error   {:text (constantly "the result should be an error")
                            :code (constantly "(should (:error (h/config-result)))")}
   :assert-error-message-contains
                           {:text (fn [{:keys [expected]}]
                                    (str "the error message should contain \"" expected "\""))
                            :code (fn [{:keys [expected]}]
                                    (str "(should (clojure.string/includes? (:error (h/config-result)) \"" expected "\"))"))}
   :assert-config-has-value
                           {:text (fn [{:keys [key expected]}]
                                    (str "the config should have \"" key "\" set to \"" expected "\""))
                            :code (fn [{:keys [key expected]}]
                                    (str "(should= \"" expected "\" (str (get (h/current-config) (keyword \"" key "\"))))"))}
   :assert-appears-before {:text (fn [{:keys [first second]}]
                                    (str "\"" first "\" should appear before \"" second "\" in the output"))
                            :code (fn [{:keys [first second]}]
                                    (str "(should (< (clojure.string/index-of (h/output) \"" first "\")\n"
                                         "           (clojure.string/index-of (h/output) \"" second "\")))"))}})
