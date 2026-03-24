(ns braids.features.steps.project-listing
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defgiven project-list-with-table "a project list with the following projects:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-project-list-from-table headers rows)))

(defgiven empty-project-list "an empty project list"
  []
  (h/set-empty-project-list))

(defwhen format-list "formatting the project list"
  []
  (h/format-list!))

(defwhen format-list-json "formatting the project list as JSON"
  []
  (h/format-list-json!))

(defthen assert-column-headers #"^the output should contain column headers (.+)$"
  [headers-str]
  (let [headers (mapv second (re-seq #"\"([^\"]+)\"" headers-str))]
    (doseq [header headers]
      (should (str/includes? (h/list-output) header)))))

(defthen assert-output-contains-slug "the output should contain slug \"{slug}\""
  [slug]
  (should (str/includes? (h/list-output) slug)))

(defthen assert-output-contains-iteration "the output should contain iteration \"{iteration}\""
  [iteration]
  (should (str/includes? (h/list-output) iteration)))

(defthen assert-output-contains-progress "the output should contain progress \"{progress}\""
  [progress]
  (should (str/includes? (h/list-output) progress)))

(defthen assert-output-contains-workers "the output should contain workers \"{workers}\""
  [workers]
  (should (str/includes? (h/list-output) workers)))

(defthen assert-dash-placeholder "the line for \"{slug}\" should contain a dash for {field}"
  [slug field]
  (should (h/line-contains-dash? slug)))

(defthen assert-output-equals "the output should be \"{expected}\""
  [expected]
  (should= expected (h/output)))

(defthen assert-status-color "\"{status}\" status should be colorized {color}"
  [status color]
  (should (h/colorized? (h/list-output) status color)))

(defthen assert-priority-color "\"{priority}\" priority should be colorized {color}"
  [priority color]
  (should (h/colorized? (h/list-output) priority color)))

(defthen assert-progress-color "{percent:int} percent progress should be colorized {color}"
  [percent color]
  (should (h/colorized? (h/list-output) (str percent "%") color)))

(defthen assert-json-project-exists "the JSON output should contain a project with slug \"{slug}\""
  [slug]
  (should (h/json-project slug)))

(defthen assert-json-project-string "the JSON project \"{slug}\" should have {key} \"{expected}\""
  [slug key expected]
  (should= expected (get (h/json-project slug) key)))

(defthen assert-json-project-number "the JSON project \"{slug}\" should have {key} {expected:int}"
  [slug key expected]
  (should= expected (get (h/json-project slug) key)))

(defthen assert-json-iteration-number "the JSON project \"{slug}\" should have iteration number \"{number}\""
  [slug number]
  (should= number (get-in (h/json-project slug) ["iteration" "number"])))
