(ns braids.features.steps.iteration
  (:require [clojure.string :as str]))

(def step-patterns
  {:given [[#"^iteration EDN with number \"([^\"]+)\" and status \"([^\"]+)\" and (\d+) stor(?:y|ies)$"
            (fn [[_ number status count]]
              {:pattern :iteration-edn :number number :status status :story-count (parse-long count)})]

           [#"^the EDN has no guardrails or notes$"
            (fn [_] {:pattern :edn-no-guardrails-or-notes})]

           [#"^an iteration with number \"([^\"]+)\" and status \"([^\"]+)\" and stories$"
            (fn [[_ number status]]
              {:pattern :iteration-with-status :number number :status status})]

           [#"^an iteration with no number$"
            (fn [_] {:pattern :iteration-no-number})]

           [#"^an iteration with stories \"([^\"]+)\" and \"([^\"]+)\"$"
            (fn [[_ id1 id2]]
              {:pattern :iteration-with-stories :story-ids [id1 id2]})]

           [#"^an iteration with story \"([^\"]+)\"$"
            (fn [[_ story-id]]
              {:pattern :iteration-with-story :story-id story-id})]

           [#"^bead \"([^\"]+)\" has status \"([^\"]+)\" and priority (\d+)$"
            (fn [[_ bead-id status priority]]
              {:pattern :iter-bead-status :bead-id bead-id :status status :priority (parse-long priority)})]

           [#"^no bead data exists$"
            (fn [_] {:pattern :no-bead-data})]

           [#"^annotated stories with (\d+) closed and (\d+) open out of (\d+) total$"
            (fn [[_ closed open total]]
              {:pattern :annotated-stories :closed (parse-long closed) :open (parse-long open) :total (parse-long total)})]

           [#"^an iteration with no stories$"
            (fn [_] {:pattern :iteration-no-stories})]

           [#"^an iteration \"([^\"]+)\" with status \"([^\"]+)\"$"
            (fn [[_ number status]]
              {:pattern :iteration-number-status :number number :status status})]

           [#"^a story \"([^\"]+)\" with status \"([^\"]+)\"$"
            (fn [[_ story-id status]]
              {:pattern :story-with-status :story-id story-id :status status})]

           [#"^completion stats of (\d+) closed out of (\d+)$"
            (fn [[_ closed total]]
              {:pattern :completion-stats :closed (parse-long closed) :total (parse-long total)})]]

   :when  [[#"^parsing the iteration EDN$"
            (fn [_] {:pattern :parse-iteration-edn})]

           [#"^validating the iteration$"
            (fn [_] {:pattern :validate-iteration})]

           [#"^annotating stories with bead data$"
            (fn [_] {:pattern :annotate-stories})]

           [#"^calculating completion stats$"
            (fn [_] {:pattern :calculate-completion-stats})]

           [#"^formatting the iteration$"
            (fn [_] {:pattern :format-iteration})]

           [#"^formatting the iteration as JSON$"
            (fn [_] {:pattern :format-iteration-json})]]

   :then  [[#"^the iteration number should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-iteration-number :expected expected})]

           [#"^the iteration status should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-iteration-status :expected expected})]

           [#"^the iteration guardrails should be empty$"
            (fn [_] {:pattern :assert-iteration-guardrails-empty})]

           [#"^the iteration notes should be empty$"
            (fn [_] {:pattern :assert-iteration-notes-empty})]

           [#"^story \"([^\"]+)\" should have status \"([^\"]+)\"$"
            (fn [[_ story-id expected]]
              {:pattern :assert-story-status :story-id story-id :expected expected})]

           [#"^the total should be (\d+)$"
            (fn [[_ expected]]
              {:pattern :assert-total :expected (parse-long expected)})]

           [#"^the closed count should be (\d+)$"
            (fn [[_ expected]]
              {:pattern :assert-closed-count :expected (parse-long expected)})]

           [#"^the completion percent should be (\d+)$"
            (fn [[_ expected]]
              {:pattern :assert-completion-percent :expected (parse-long expected)})]

           [#"^the JSON should contain \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-json-contains :expected expected})]]})

(def step-registry
  {:iteration-edn         {:text (fn [{:keys [number status story-count]}]
                                   (str "iteration EDN with number \"" number "\" and status \"" status "\" and " story-count " story"))
                            :code (fn [{:keys [number status story-count]}]
                                   (str "(h/set-iteration-edn \"" number "\" \"" status "\" " story-count ")"))}
   :edn-no-guardrails-or-notes
                           {:text (constantly "the EDN has no guardrails or notes")
                            :code (constantly nil)}
   :iteration-with-status {:text (fn [{:keys [number status]}]
                                   (str "an iteration with number \"" number "\" and status \"" status "\" and stories"))
                            :code (fn [{:keys [number status]}]
                                   (str "(h/set-iteration-with-status \"" number "\" \"" status "\")"))}
   :iteration-no-number   {:text (constantly "an iteration with no number")
                            :code (constantly "(h/set-iteration-no-number)")}
   :iteration-with-stories {:text (fn [{:keys [story-ids]}]
                                    (str "an iteration with stories \"" (first story-ids) "\" and \"" (second story-ids) "\""))
                             :code (fn [{:keys [story-ids]}]
                                    (str "(h/set-iteration-stories " (pr-str story-ids) ")"))}
   :iteration-with-story  {:text (fn [{:keys [story-id]}]
                                   (str "an iteration with story \"" story-id "\""))
                            :code (fn [{:keys [story-id]}]
                                   (str "(h/set-iteration-stories [\"" story-id "\"])"))}
   :iter-bead-status      {:text (fn [{:keys [bead-id status priority]}]
                                   (str "bead \"" bead-id "\" has status \"" status "\" and priority " priority))
                            :code (fn [{:keys [bead-id status priority]}]
                                   (str "(h/add-iter-bead \"" bead-id "\" \"" status "\" " priority ")"))}
   :no-bead-data          {:text (constantly "no bead data exists")
                            :code (constantly nil)}
   :annotated-stories     {:text (fn [{:keys [closed open total]}]
                                   (str "annotated stories with " closed " closed and " open " open out of " total " total"))
                            :code (fn [{:keys [closed open total]}]
                                   (str "(h/set-annotated-stories " closed " " open " " total ")"))}
   :iteration-no-stories  {:text (constantly "an iteration with no stories")
                            :code (constantly "(h/set-iteration-stories [])")}
   :iteration-number-status {:text (fn [{:keys [number status]}]
                                     (str "an iteration \"" number "\" with status \"" status "\""))
                              :code (fn [{:keys [number status]}]
                                     (str "(h/set-iteration-number-status \"" number "\" \"" status "\")"))}
   :story-with-status     {:text (fn [{:keys [story-id status]}]
                                   (str "a story \"" story-id "\" with status \"" status "\""))
                            :code (fn [{:keys [story-id status]}]
                                   (str "(h/add-story-with-status \"" story-id "\" \"" status "\")"))}
   :completion-stats      {:text (fn [{:keys [closed total]}]
                                   (str "completion stats of " closed " closed out of " total))
                            :code (fn [{:keys [closed total]}]
                                   (str "(h/set-completion-stats " closed " " total ")"))}
   :parse-iteration-edn   {:text (constantly "parsing the iteration EDN")
                            :code (constantly "(h/parse-iteration-edn!)")}
   :validate-iteration    {:text (constantly "validating the iteration")
                            :code (constantly "(h/validate-iteration!)")}
   :annotate-stories      {:text (constantly "annotating stories with bead data")
                            :code (constantly "(h/annotate-stories!)")}
   :calculate-completion-stats {:text (constantly "calculating completion stats")
                                 :code (constantly "(h/calculate-completion-stats!)")}
   :format-iteration      {:text (constantly "formatting the iteration")
                            :code (constantly "(h/format-iteration!)")}
   :format-iteration-json {:text (constantly "formatting the iteration as JSON")
                            :code (constantly "(h/format-iteration-json!)")}
   :assert-iteration-number {:text (fn [{:keys [expected]}]
                                     (str "the iteration number should be \"" expected "\""))
                              :code (fn [{:keys [expected]}]
                                     (str "(should= \"" expected "\" (h/iteration-number))"))}
   :assert-iteration-status {:text (fn [{:keys [expected]}]
                                     (str "the iteration status should be \"" expected "\""))
                              :code (fn [{:keys [expected]}]
                                     (str "(should= \"" expected "\" (h/iteration-status))"))}
   :assert-iteration-guardrails-empty {:text (constantly "the iteration guardrails should be empty")
                                        :code (constantly "(should (empty? (h/iteration-guardrails)))")}
   :assert-iteration-notes-empty {:text (constantly "the iteration notes should be empty")
                                   :code (constantly "(should (empty? (h/iteration-notes)))")}
   :assert-story-status   {:text (fn [{:keys [story-id expected]}]
                                   (str "story \"" story-id "\" should have status \"" expected "\""))
                            :code (fn [{:keys [story-id expected]}]
                                   (str "(should= \"" expected "\" (h/story-status \"" story-id "\"))"))}
   :assert-total          {:text (fn [{:keys [expected]}]
                                   (str "the total should be " expected))
                            :code (fn [{:keys [expected]}]
                                   (str "(should= " expected " (h/stats-total))"))}
   :assert-closed-count   {:text (fn [{:keys [expected]}]
                                   (str "the closed count should be " expected))
                            :code (fn [{:keys [expected]}]
                                   (str "(should= " expected " (h/stats-closed))"))}
   :assert-completion-percent {:text (fn [{:keys [expected]}]
                                       (str "the completion percent should be " expected))
                                :code (fn [{:keys [expected]}]
                                       (str "(should= " expected " (h/stats-percent))"))}
   :assert-json-contains  {:text (fn [{:keys [expected]}]
                                    (str "the JSON should contain \"" expected "\""))
                             :code (fn [{:keys [expected]}]
                                    (str "(should (clojure.string/includes? (h/iter-json-output) \"" expected "\"))"))}})
