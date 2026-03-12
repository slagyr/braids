(ns braids.features.steps.orch-output
  (:require [clojure.string :as str]))

(defn- table-row->configure-project-code
  "Generate harness call for a single row of the configured-projects table."
  [headers row]
  (let [m (zipmap headers row)
        hs (pr-str (vec headers))
        rs (pr-str (vec row))]
    nil)) ;; unused — we generate all rows at once

(def step-patterns
  {:given [[#"^configured projects:$"
            (fn [_] {:pattern :configured-projects-table})]

           [#"^project \"([^\"]+)\" has beads:$"
            (fn [[_ slug]]
              {:pattern :project-has-beads-table :slug slug})]]

   :when  []

   :then  [[#"^the output contains lines matching$"
            (fn [_] {:pattern :output-contains-lines-matching})]

           [#"^the output contains a line matching$"
            (fn [_] {:pattern :output-contains-a-line-matching})]

           [#"^the output does not contain$"
            (fn [_] {:pattern :output-does-not-contain})]

           [#"^the output has \"([^\"]+)\" before \"([^\"]+)\"$"
            (fn [[_ first-text second-text]]
              {:pattern :output-has-before :first first-text :second second-text})]]})

(def step-registry
  {:configured-projects-table
   {:text (constantly "configured projects:")
    :code (fn [{:keys [table]}]
            (let [{:keys [headers rows]} table
                  hs (pr-str (vec headers))
                  rs (str/join " " (map #(pr-str (vec %)) rows))]
              (str "(h/configure-projects-from-table\n"
                   "  " hs "\n"
                   "  [" rs "])")))}

   :project-has-beads-table
   {:text (fn [{:keys [slug]}] (str "project \"" slug "\" has beads:"))
    :code (fn [{:keys [slug table]}]
            (let [{:keys [headers rows]} table
                  hs (pr-str (vec headers))
                  rs (str/join " " (map #(pr-str (vec %)) rows))]
              (str "(h/set-project-beads \"" slug "\"\n"
                   "  " hs "\n"
                   "  [" rs "])")))}

   :output-contains-lines-matching
   {:text (constantly "the output contains lines matching")
    :code (fn [{:keys [table]}]
            (let [{:keys [rows]} table]
              (str/join "\n"
                        (map (fn [row]
                               (let [text (first row)]
                                 (str "(should (h/output-contains-line? \"" text "\"))")))
                             rows))))}

   :output-contains-a-line-matching
   {:text (constantly "the output contains a line matching")
    :code (fn [{:keys [doc-string]}]
            (when doc-string
              (str "(should (h/output-contains-line-matching? \"" (str/replace doc-string "\"" "\\\"") "\"))")))}

   :output-does-not-contain
   {:text (constantly "the output does not contain")
    :code (fn [{:keys [table]}]
            (let [{:keys [rows]} table]
              (str/join "\n"
                        (map (fn [row]
                               (let [text (first row)]
                                 (str "(should-not (h/output-contains? \"" text "\"))")))
                             rows))))}

   :output-has-before
   {:text (fn [{:keys [first second]}] (str "the output has \"" first "\" before \"" second "\""))
    :code (fn [{:keys [first second]}] (str "(should (h/output-has-before? \"" first "\" \"" second "\"))"))}})
