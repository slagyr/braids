(ns braids.features.steps.project-status
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [speclj.core :refer :all]))

(defgiven project-configs-table "project configs:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-project-configs-from-table headers rows)))

(defgiven active-iterations-table "active iterations:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-active-iterations-from-table headers rows)))

(defgiven active-workers-table "active workers:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-active-workers-from-table headers rows)))

(defgiven no-active-iterations "no active iterations"
  []
  nil)

(defgiven dashboard-project "a dashboard project \"{slug}\" with:"
  [slug table]
  (let [{:keys [headers rows]} table]
    (h/set-dashboard-project slug headers rows)))

(defgiven project-has-iteration "project \"{slug}\" has iteration:"
  [slug table]
  (let [{:keys [headers rows]} table]
    (h/set-project-iteration slug headers rows)))

(defgiven project-has-stories "project \"{slug}\" has stories:"
  [slug table]
  (let [{:keys [headers rows]} table]
    (h/set-project-stories slug headers rows)))

(defgiven project-has-no-iteration "project \"{slug}\" has no iteration"
  [slug]
  (h/clear-project-iteration slug))

(defgiven empty-registry "an empty registry"
  []
  (h/set-empty-registry))

(defwhen build-dashboard "building the dashboard"
  []
  (h/build-dashboard!))

(defwhen format-project-detail "formatting project detail for \"{slug}\""
  [slug]
  (h/format-project-detail! slug))

(defwhen format-dashboard-json "formatting the dashboard as JSON"
  []
  (h/format-dashboard-json!))

(defwhen format-dashboard "formatting the dashboard"
  []
  (h/format-dashboard!))

(defthen assert-dashboard-project-count #"^the dashboard should have (\d+) projects?$"
  [cnt]
  (should= (parse-long cnt) (clojure.core/count (:projects (h/dashboard)))))

(defthen assert-project-status "project \"{slug}\" should have status \"{expected}\""
  [slug expected]
  (should= expected (:status (h/dashboard-project slug))))

(defthen assert-project-iteration-number "project \"{slug}\" should have iteration number \"{expected}\""
  [slug expected]
  (should= expected (get-in (h/dashboard-project slug) [:iteration :number])))

(defthen assert-project-workers "project \"{slug}\" should have workers {workers:int} of {max-workers:int}"
  [slug workers max-workers]
  (should= workers (:workers (h/dashboard-project slug)))
  (should= max-workers (:max-workers (h/dashboard-project slug))))

(defthen assert-project-no-iteration "project \"{slug}\" should have no iteration"
  [slug]
  (should-be-nil (:iteration (h/dashboard-project slug))))

(defthen assert-json-project-count #"^the JSON should contain (\d+) projects?$"
  [cnt]
  (should= (parse-long cnt) (clojure.core/count (:projects (h/dashboard-json)))))

(defthen assert-json-project-iteration-percent "the JSON project \"{slug}\" should have iteration percent {percent:int}"
  [slug percent]
  (should= percent (get-in (h/dashboard-json-project slug) ["iteration" "stats" "percent"])))
