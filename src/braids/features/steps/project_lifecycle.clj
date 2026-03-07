(ns braids.features.steps.project-lifecycle
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^bd is not available$"
            (fn [_] {:pattern :bd-not-available})]

           [#"^bd is available$"
            (fn [_] {:pattern :bd-available})]

           [#"^no registry exists$"
            (fn [_] {:pattern :no-registry})]

           [#"^a registry already exists$"
            (fn [_] {:pattern :registry-exists})]

           [#"^force is not set$"
            (fn [_] {:pattern :force-not-set})]

           [#"^force is set$"
            (fn [_] {:pattern :force-set})]

           [#"^braids dir does not exist$"
            (fn [_] {:pattern :braids-dir-not-exists})]

           [#"^braids dir already exists$"
            (fn [_] {:pattern :braids-dir-exists})]

           [#"^braids home does not exist$"
            (fn [_] {:pattern :braids-home-not-exists})]

           [#"^braids home already exists$"
            (fn [_] {:pattern :braids-home-exists})]

           [#"^a new project with slug \"([^\"]+)\"$"
            (fn [[_ slug]]
              {:pattern :new-project-slug :slug slug})]

           [#"^a new project with name \"([^\"]+)\"$"
            (fn [[_ name]]
              {:pattern :new-project-name :name name})]

           [#"^name \"([^\"]+)\"$"
            (fn [[_ name]]
              {:pattern :set-name :name name})]

           [#"^goal \"([^\"]+)\"$"
            (fn [[_ goal]]
              {:pattern :set-goal :goal goal})]

           [#"^a registry with project \"([^\"]+)\"$"
            (fn [[_ slug]]
              {:pattern :registry-with-project :slug slug})]

           [#"^a new registry entry with slug \"([^\"]+)\"$"
            (fn [[_ slug]]
              {:pattern :new-registry-entry :slug slug})]]

   :when  [[#"^checking prerequisites$"
            (fn [_] {:pattern :check-prerequisites})]

           [#"^planning init$"
            (fn [_] {:pattern :plan-init})]

           [#"^validating new project params$"
            (fn [_] {:pattern :validate-new-project})]

           [#"^adding the entry to the registry$"
            (fn [_] {:pattern :add-to-registry})]

           [#"^building the project config$"
            (fn [_] {:pattern :build-project-config})]]

   :then  [[#"^prerequisites should fail with \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-prereq-fail :expected expected})]

           [#"^prerequisites should pass$"
            (fn [_] {:pattern :assert-prereq-pass})]

           [#"^the plan should include \"([^\"]+)\"$"
            (fn [[_ action]]
              {:pattern :assert-plan-include :action action})]

           [#"^the plan should not include \"([^\"]+)\"$"
            (fn [[_ action]]
              {:pattern :assert-plan-not-include :action action})]

           [#"^validation should fail with \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-validation-fail :expected expected})]

           [#"^it should fail with \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-should-fail :expected expected})]

           [#"^the config (\S+) should be \"([^\"]+)\"$"
            (fn [[_ key expected]]
              {:pattern :assert-config-value :key key :expected expected})]

           [#"^the config (\S+) should be (\d+)$"
            (fn [[_ key expected]]
              {:pattern :assert-config-number :key key :expected (parse-long expected)})]]})

(def step-registry
  {:bd-not-available      {:text (constantly "bd is not available")
                           :code (constantly "(h/set-bd-not-available)")}
   :bd-available          {:text (constantly "bd is available")
                           :code (constantly "(h/set-bd-available)")}
   :no-registry           {:text (constantly "no registry exists")
                           :code (constantly "(h/set-no-registry)")}
   :registry-exists       {:text (constantly "a registry already exists")
                           :code (constantly "(h/set-registry-exists)")}
   :force-not-set         {:text (constantly "force is not set")
                           :code (constantly "(h/set-force-not-set)")}
   :force-set             {:text (constantly "force is set")
                           :code (constantly "(h/set-force-set)")}
   :braids-dir-not-exists {:text (constantly "braids dir does not exist")
                           :code (constantly "(h/set-braids-dir-not-exists)")}
   :braids-dir-exists     {:text (constantly "braids dir already exists")
                           :code (constantly "(h/set-braids-dir-exists)")}
   :braids-home-not-exists {:text (constantly "braids home does not exist")
                            :code (constantly "(h/set-braids-home-not-exists)")}
   :braids-home-exists    {:text (constantly "braids home already exists")
                           :code (constantly "(h/set-braids-home-exists)")}
   :check-prerequisites   {:text (constantly "checking prerequisites")
                           :code (constantly "(h/check-prerequisites!)")}
   :plan-init             {:text (constantly "planning init")
                           :code (constantly "(h/plan-init!)")}
   :assert-prereq-fail    {:text (fn [{:keys [expected]}]              (str "prerequisites should fail with \"" expected "\""))
                           :code (fn [{:keys [expected]}]              (str "(should-not (empty? (h/prereq-errors)))\n(should (some #(clojure.string/includes? % \"" expected "\") (h/prereq-errors)))"))}
   :assert-prereq-pass    {:text (constantly "prerequisites should pass")
                           :code (constantly "(should (empty? (h/prereq-errors)))")}
   :assert-plan-include   {:text (fn [{:keys [action]}]               (str "the plan should include \"" action "\""))
                           :code (fn [{:keys [action]}]               (str "(should (some #{\"" action "\"} (h/plan-actions)))"))}
   :assert-plan-not-include {:text (fn [{:keys [action]}]             (str "the plan should not include \"" action "\""))
                             :code (fn [{:keys [action]}]             (str "(should-not (some #{\"" action "\"} (h/plan-actions)))"))}
   :new-project-slug      {:text (fn [{:keys [slug]}]                (str "a new project with slug \"" slug "\""))
                           :code (fn [{:keys [slug]}]                (str "(h/set-new-project-slug \"" slug "\")"))}
   :new-project-name      {:text (fn [{:keys [name]}]                (str "a new project with name \"" name "\""))
                           :code (fn [{:keys [name]}]                (str "(h/set-new-project-name \"" name "\")"))}
   :set-name              {:text (fn [{:keys [name]}]                (str "name \"" name "\""))
                           :code (fn [{:keys [name]}]                (str "(h/set-new-project-name \"" name "\")"))}
   :set-goal              {:text (fn [{:keys [goal]}]                (str "goal \"" goal "\""))
                           :code (fn [{:keys [goal]}]                (str "(h/set-new-project-goal \"" goal "\")"))}
   :registry-with-project {:text (fn [{:keys [slug]}]                (str "a registry with project \"" slug "\""))
                           :code (fn [{:keys [slug]}]                (str "(h/set-registry-with-project \"" slug "\")"))}
   :new-registry-entry    {:text (fn [{:keys [slug]}]                (str "a new registry entry with slug \"" slug "\""))
                           :code (fn [{:keys [slug]}]                (str "(h/set-new-registry-entry \"" slug "\")"))}
   :validate-new-project  {:text (constantly "validating new project params")
                           :code (constantly "(h/validate-new-project!)")}
   :add-to-registry       {:text (constantly "adding the entry to the registry")
                           :code (constantly "(h/add-to-registry!)")}
   :build-project-config  {:text (constantly "building the project config")
                           :code (constantly "(h/build-project-config!)")}
   :assert-validation-fail {:text (fn [{:keys [expected]}]            (str "validation should fail with \"" expected "\""))
                            :code (fn [{:keys [expected]}]            (str "(should-not (empty? (h/validation-errors)))\n(should (some #(clojure.string/includes? % \"" expected "\") (h/validation-errors)))"))}
   :assert-should-fail    {:text (fn [{:keys [expected]}]             (str "it should fail with \"" expected "\""))
                           :code (fn [{:keys [expected]}]             (str "(should (clojure.string/includes? (or (h/add-registry-error) \"\") \"" expected "\"))"))}
   :assert-config-value   {:text (fn [{:keys [key expected]}]         (str "the config " key " should be \"" expected "\""))
                           :code (fn [{:keys [key expected]}]         (str "(should= " (if (re-matches #"^:.*" expected) expected (str ":" expected)) " (:" key " (h/project-config)))"))}
   :assert-config-number  {:text (fn [{:keys [key expected]}]         (str "the config " key " should be " expected))
                           :code (fn [{:keys [key expected]}]         (str "(should= " expected " (:" key " (h/project-config)))"))}})
