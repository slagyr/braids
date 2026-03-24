(ns braids.features.steps.orch-output
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defgiven configured-projects-table "configured projects:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/configure-projects-from-table headers rows)))

(defgiven project-has-beads-table "project \"{slug}\" has beads:"
  [slug table]
  (let [{:keys [headers rows]} table]
    (h/set-project-beads slug headers rows)))

(defthen output-contains-lines-matching "the output contains lines matching"
  [table]
  (let [{:keys [rows]} table]
    (doseq [row rows]
      (should (h/output-contains-line? (first row))))))

(defthen output-contains-a-line-matching "the output contains a line matching"
  [table]
  (let [{:keys [rows]} table]
    (doseq [row rows]
      (should (h/output-contains-line-matching? (first row))))))

(defthen output-does-not-contain "the output does not contain"
  [table]
  (let [{:keys [rows]} table]
    (doseq [row rows]
      (should-not (h/output-contains? (first row))))))

(defthen output-has-before "the output has \"{first-text}\" before \"{second-text}\""
  [first-text second-text]
  (should (h/output-has-before? first-text second-text)))
