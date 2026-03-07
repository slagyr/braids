(ns braids.features.steps.project-listing
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^a project list with the following projects:$"
            (fn [_] {:pattern :project-list-with-table})]

           [#"^an empty project list$"
            (fn [_] {:pattern :empty-project-list})]]

   :when  [[#"^formatting the project list as JSON$"
            (fn [_] {:pattern :format-list-json})]

           [#"^formatting the project list$"
            (fn [_] {:pattern :format-list})]]

   :then  [[#"^the output should contain column headers (.+)$"
            (fn [[_ headers-str]]
              {:pattern :assert-column-headers :headers (re-seq #"\"([^\"]+)\"" headers-str)})]

           [#"^the output should contain slug \"([^\"]+)\"$"
            (fn [[_ slug]]
              {:pattern :assert-output-contains-slug :slug slug})]

           [#"^the output should contain iteration \"([^\"]+)\"$"
            (fn [[_ iteration]]
              {:pattern :assert-output-contains-iteration :iteration iteration})]

           [#"^the output should contain progress \"([^\"]+)\"$"
            (fn [[_ progress]]
              {:pattern :assert-output-contains-progress :progress progress})]

           [#"^the output should contain workers \"([^\"]+)\"$"
            (fn [[_ workers]]
              {:pattern :assert-output-contains-workers :workers workers})]

           [#"^the line for \"([^\"]+)\" should contain a dash for (\S+)$"
            (fn [[_ slug field]]
              {:pattern :assert-dash-placeholder :slug slug :field field})]

           [#"^the output should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-output-equals :expected expected})]

           [#"^\"([^\"]+)\" status should be colorized (\w+)$"
            (fn [[_ status color]]
              {:pattern :assert-status-color :status status :color color})]

           [#"^\"([^\"]+)\" priority should be colorized (\w+)$"
            (fn [[_ priority color]]
              {:pattern :assert-priority-color :priority priority :color color})]

           [#"^(\d+) percent progress should be colorized (\w+)$"
            (fn [[_ percent color]]
              {:pattern :assert-progress-color :percent (parse-long percent) :color color})]

           [#"^the JSON output should contain a project with slug \"([^\"]+)\"$"
            (fn [[_ slug]]
              {:pattern :assert-json-project-exists :slug slug})]

           [#"^the JSON project \"([^\"]+)\" should have (\S+) \"([^\"]+)\"$"
            (fn [[_ slug key expected]]
              {:pattern :assert-json-project-string :slug slug :key key :expected expected})]

           [#"^the JSON project \"([^\"]+)\" should have (\S+) (\d+)$"
            (fn [[_ slug key expected]]
              {:pattern :assert-json-project-number :slug slug :key key :expected (parse-long expected)})]

           [#"^the JSON project \"([^\"]+)\" should have iteration number \"([^\"]+)\"$"
            (fn [[_ slug number]]
              {:pattern :assert-json-iteration-number :slug slug :number number})]]})

(def step-registry
  {:project-list-with-table {:text (constantly "a project list with the following projects:")
                             :code (fn [{:keys [table]}]
                                     (when table
                                       (let [{:keys [headers rows]} table]
                                         (str "(h/set-project-list-from-table\n"
                                              "  " (pr-str headers) "\n"
                                              "  " (pr-str rows) ")"))))}
   :empty-project-list    {:text (constantly "an empty project list")
                           :code (constantly "(h/set-empty-project-list)")}
   :format-list           {:text (constantly "formatting the project list")
                           :code (constantly "(h/format-list!)")}
   :format-list-json      {:text (constantly "formatting the project list as JSON")
                           :code (constantly "(h/format-list-json!)")}
   :assert-column-headers {:text (fn [{:keys [headers]}]              (str "the output should contain column headers"))
                           :code (fn [{:keys [headers]}]
                                   (let [header-strs (mapv second headers)]
                                     (str/join "\n" (map #(str "(should (clojure.string/includes? (h/list-output) \"" % "\"))") header-strs))))}
   :assert-output-contains-slug {:text (fn [{:keys [slug]}]           (str "the output should contain slug \"" slug "\""))
                                 :code (fn [{:keys [slug]}]           (str "(should (clojure.string/includes? (h/list-output) \"" slug "\"))"))}
   :assert-output-contains-iteration {:text (fn [{:keys [iteration]}] (str "the output should contain iteration \"" iteration "\""))
                                      :code (fn [{:keys [iteration]}] (str "(should (clojure.string/includes? (h/list-output) \"" iteration "\"))"))}
   :assert-output-contains-progress {:text (fn [{:keys [progress]}]   (str "the output should contain progress \"" progress "\""))
                                     :code (fn [{:keys [progress]}]   (str "(should (clojure.string/includes? (h/list-output) \"" progress "\"))"))}
   :assert-output-contains-workers {:text (fn [{:keys [workers]}]     (str "the output should contain workers \"" workers "\""))
                                    :code (fn [{:keys [workers]}]     (str "(should (clojure.string/includes? (h/list-output) \"" workers "\"))"))}
   :assert-dash-placeholder {:text (fn [{:keys [slug field]}]         (str "the line for \"" slug "\" should contain a dash for " field))
                             :code (fn [{:keys [slug]}]               (str "(should (h/line-contains-dash? \"" slug "\"))"))}
   :assert-output-equals  {:text (fn [{:keys [expected]}]             (str "the output should be \"" expected "\""))
                            :code (fn [{:keys [expected]}]             (str "(should= \"" expected "\" (h/output))"))}
   :assert-status-color   {:text (fn [{:keys [status color]}]        (str "\"" status "\" status should be colorized " color))
                           :code (fn [{:keys [status color]}]        (str "(should (h/colorized? (h/list-output) \"" status "\" \"" color "\"))"))}
   :assert-priority-color {:text (fn [{:keys [priority color]}]      (str "\"" priority "\" priority should be colorized " color))
                           :code (fn [{:keys [priority color]}]      (str "(should (h/colorized? (h/list-output) \"" priority "\" \"" color "\"))"))}
   :assert-progress-color {:text (fn [{:keys [percent color]}]       (str percent " percent progress should be colorized " color))
                           :code (fn [{:keys [percent color]}]       (str "(should (h/colorized? (h/list-output) \"" percent "%\" \"" color "\"))"))}
   :assert-json-project-exists {:text (fn [{:keys [slug]}]           (str "the JSON output should contain a project with slug \"" slug "\""))
                                :code (fn [{:keys [slug]}]           (str "(should (h/json-project \"" slug "\"))"))}
   :assert-json-project-string {:text (fn [{:keys [slug key expected]}] (str "the JSON project \"" slug "\" should have " key " \"" expected "\""))
                                :code (fn [{:keys [slug key expected]}] (str "(should= \"" expected "\" (get (h/json-project \"" slug "\") \"" key "\"))"))}
   :assert-json-project-number {:text (fn [{:keys [slug key expected]}] (str "the JSON project \"" slug "\" should have " key " " expected))
                                :code (fn [{:keys [slug key expected]}] (str "(should= " expected " (get (h/json-project \"" slug "\") \"" key "\"))"))}
   :assert-json-iteration-number {:text (fn [{:keys [slug number]}]  (str "the JSON project \"" slug "\" should have iteration number \"" number "\""))
                                   :code (fn [{:keys [slug number]}]  (str "(should= \"" number "\" (get-in (h/json-project \"" slug "\") [\"iteration\" \"number\"]))"))}})
