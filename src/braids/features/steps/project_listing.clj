(ns braids.features.steps.project-listing
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.list :as list]
            [cheshire.core :as json]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defn- table-row->project
  [headers row]
  (let [m (zipmap headers row)
        slug (get m "slug")
        status (get m "status")
        priority (get m "priority")
        iteration-num (get m "iteration")
        closed (get m "closed")
        total (get m "total")
        percent (get m "percent")
        workers-str (get m "workers")
        max-workers-str (get m "max-workers")
        path (get m "path")]
    (cond->
      {:slug slug
       :status (when (seq status) (keyword status))
       :priority (when (seq priority) (keyword priority))
       :path path
       :workers (when (seq workers-str) (parse-long workers-str))
       :max-workers (when (seq max-workers-str) (parse-long max-workers-str))}
      (seq iteration-num)
      (assoc :iteration
             (cond-> {:number iteration-num}
               (seq closed)
               (assoc :stats {:closed (parse-long closed)
                              :total (parse-long total)
                              :percent (parse-long percent)}))))))

(defn- list-output []
  (or (g/get :list-output) (g/get :list-json-output)))

(defn- line-contains-dash? [slug]
  (when-let [output (g/get :list-output)]
    (some (fn [line]
            (and (str/includes? line slug)
                 (str/includes? line "\u2014")))
          (str/split-lines output))))

(defn- colorized? [output text color]
  (let [color-code (case color
                     "red" "\033[31m"
                     "green" "\033[32m"
                     "yellow" "\033[33m"
                     nil)]
    (and color-code
         (str/includes? output (str color-code))
         (str/includes? output text))))

(defn- json-project [slug]
  (when-let [output (g/get :list-json-output)]
    (let [parsed (json/parse-string output)
          projects (if (map? parsed) (get parsed "projects") parsed)]
      (first (filter #(= slug (get % "slug")) projects)))))

(defgiven project-list-with-table "a project list with the following projects:"
  [table]
  (let [{:keys [headers rows]} table
        projects (mapv #(table-row->project headers %) rows)]
    (g/assoc! :list-projects projects)))

(defgiven empty-project-list "an empty project list"
  []
  (g/assoc! :list-projects []))

(defwhen format-list "formatting the project list"
  []
  (let [output (list/format-list {:projects (g/get :list-projects)})]
    (g/assoc! :list-output output :output output)))

(defwhen format-list-json "formatting the project list as JSON"
  []
  (let [output (list/format-list-json {:projects (g/get :list-projects)})]
    (g/assoc! :list-json-output output :output output)))

(defthen assert-column-headers #"^the output should contain column headers (.+)$"
  [headers-str]
  (let [headers (mapv second (re-seq #"\"([^\"]+)\"" headers-str))]
    (doseq [header headers]
      (should (str/includes? (list-output) header)))))

(defthen assert-output-contains-slug #"^the output should contain slug \"([^\"]+)\"$"
  [slug]
  (should (str/includes? (list-output) slug)))

(defthen assert-output-contains-iteration #"^the output should contain iteration \"([^\"]+)\"$"
  [iteration]
  (should (str/includes? (list-output) iteration)))

(defthen assert-output-contains-progress #"^the output should contain progress \"([^\"]+)\"$"
  [progress]
  (should (str/includes? (list-output) progress)))

(defthen assert-output-contains-workers #"^the output should contain workers \"([^\"]+)\"$"
  [workers]
  (should (str/includes? (list-output) workers)))

(defthen assert-dash-placeholder #"^the line for \"([^\"]+)\" should contain a dash for (\S+)$"
  [slug _field]
  (should (line-contains-dash? slug)))

(defthen assert-output-equals #"^the output should be \"([^\"]+)\"$"
  [expected]
  (should= expected (g/get :output)))

(defthen assert-status-color #"^\"([^\"]+)\" status should be colorized (\w+)$"
  [status color]
  (should (colorized? (list-output) status color)))

(defthen assert-priority-color #"^\"([^\"]+)\" priority should be colorized (\w+)$"
  [priority color]
  (should (colorized? (list-output) priority color)))

(defthen assert-progress-color #"^(\d+) percent progress should be colorized (\w+)$"
  [percent color]
  (should (colorized? (list-output) (str percent "%") color)))

(defthen assert-json-project-exists #"^the JSON output should contain a project with slug \"([^\"]+)\"$"
  [slug]
  (should (json-project slug)))

(defthen assert-json-project-string #"^the JSON project \"([^\"]+)\" should have (\S+) \"([^\"]+)\"$"
  [slug key expected]
  (should= expected (get (json-project slug) key)))

(defthen assert-json-project-number #"^the JSON project \"([^\"]+)\" should have (\S+) (\d+)$"
  [slug key expected]
  (should= (parse-long expected) (get (json-project slug) key)))

(defthen assert-json-iteration-number #"^the JSON project \"([^\"]+)\" should have iteration number \"([^\"]+)\"$"
  [slug number]
  (should= number (get-in (json-project slug) ["iteration" "number"])))
