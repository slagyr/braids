(ns braids.features.steps.worker-session)

(def step-patterns
  {:given [[#"^a bead with id \"([^\"]+)\"$"
            (fn [[_ bead-id]]
              {:pattern :bead :bead-id bead-id})]

           [#"^another bead with id \"([^\"]+)\"$"
            (fn [[_ bead-id]]
              {:pattern :bead :bead-id bead-id})]

           [#"^a session ID \"([^\"]+)\"$"
            (fn [[_ session-id]]
              {:pattern :session-id-literal :session-id session-id})]]

   :when  [[#"^generating the session ID twice$"
            (fn [_] {:pattern :generate-session-id-twice})]

           [#"^generating the session ID$"
            (fn [_] {:pattern :generate-session-id})]

           [#"^generating session IDs for both$"
            (fn [_] {:pattern :generate-session-ids-both})]

           [#"^parsing the session ID$"
            (fn [_] {:pattern :parse-session-id})]]

   :then  [[#"^the session ID should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-session-id :expected expected})]

           [#"^both session IDs should be identical$"
            (fn [_] {:pattern :assert-ids-identical})]

           [#"^the session IDs should be different$"
            (fn [_] {:pattern :assert-ids-different})]

           [#"^the extracted bead ID should be \"([^\"]+)\"$"
            (fn [[_ expected]]
              {:pattern :assert-bead-id :expected expected})]]})

(def step-registry
  {:bead               {:text (fn [{:keys [bead-id]}]               (str "a bead with id \"" bead-id "\""))
                        :code (fn [{:keys [bead-id]}]               (str "(h/set-bead-id \"" bead-id "\")"))}
   :session-id-literal {:text (fn [{:keys [session-id]}]            (str "a session ID \"" session-id "\""))
                        :code (fn [{:keys [session-id]}]            (str "(h/set-session-id-literal \"" session-id "\")"))}
   :generate-session-id       {:text (constantly "generating the session ID")
                                :code (constantly "(h/generate-session-id!)")}
   :generate-session-id-twice {:text (constantly "generating the session ID twice")
                                :code (constantly "(h/generate-session-id-twice!)")}
   :generate-session-ids-both {:text (constantly "generating session IDs for both")
                                :code (constantly "(h/generate-session-ids-both!)")}
   :parse-session-id          {:text (constantly "parsing the session ID")
                                :code (constantly "(h/parse-session-id!)")}
   :assert-session-id  {:text (fn [{:keys [expected]}]              (str "the session ID should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/session-id-result))"))}
   :assert-ids-identical {:text (constantly "both session IDs should be identical")
                           :code (constantly "(should (h/session-ids-identical?))")}
   :assert-ids-different {:text (constantly "the session IDs should be different")
                           :code (constantly "(should (h/session-ids-different?))")}
   :assert-bead-id     {:text (fn [{:keys [expected]}]              (str "the extracted bead ID should be \"" expected "\""))
                        :code (fn [{:keys [expected]}]              (str "(should= \"" expected "\" (h/parsed-bead-id))"))}})

