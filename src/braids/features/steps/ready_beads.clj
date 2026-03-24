(ns braids.features.steps.ready-beads
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defgiven registry-with-projects-table "a registry with projects:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-registry-from-table headers rows)))

(defgiven project-config-status-and-max-workers "project \"{slug}\" has config with status \"{status}\" and max-workers {max-workers:int}"
  [slug status max-workers]
  (h/set-project-config slug {:status status :max-workers max-workers}))

(defgiven project-config-max-workers "project \"{slug}\" has config with max-workers {max-workers:int}"
  [slug max-workers]
  (h/set-project-config slug {:max-workers max-workers}))

(defgiven project-ready-beads-table "project \"{slug}\" has ready beads:"
  [slug table]
  (let [{:keys [headers rows]} table]
    (h/set-project-ready-beads slug headers rows)))

(defgiven no-active-workers "no active workers"
  []
  nil)

(defgiven ready-beads-to-format "ready beads to format:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-ready-beads-to-format headers rows)))

(defgiven no-ready-beads-to-format "no ready beads to format"
  []
  (h/set-no-ready-beads-to-format))

(defwhen compute-ready-beads "computing ready beads"
  []
  (h/compute-ready-beads!))

(defwhen format-ready-output "formatting ready output"
  []
  (h/format-ready-output!))

(defthen assert-result-contains-bead "the result should contain bead \"{bead-id}\""
  [bead-id]
  (should (h/result-contains-bead? bead-id)))

(defthen assert-result-not-contains-bead "the result should not contain bead \"{bead-id}\""
  [bead-id]
  (should-not (h/result-contains-bead? bead-id)))

(defthen assert-result-empty "the result should be empty"
  []
  (should (empty? (h/ready-result))))

(defthen assert-nth-result-project #"^the (first|second|third) result should be from project \"([^\"]+)\"$"
  [ordinal slug]
  (let [position (case ordinal "first" 1 "second" 2 "third" 3)]
    (should= slug (:project (nth (h/ready-result) (dec position))))))

(defthen assert-output-contains "the output should contain \"{expected}\""
  [expected]
  (should (str/includes? (h/output) expected)))
