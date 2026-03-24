(ns braids.features.steps.configuration
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defgiven config-with-values "a config with values:"
  [table]
  (let [{:keys [headers rows]} table]
    (h/set-config-from-table headers rows)))

(defgiven empty-config-string "an empty config string"
  []
  (h/set-empty-config))

(defwhen list-config "listing the config"
  []
  (h/list-config!))

(defwhen get-config-key "getting config key \"{key}\""
  [key]
  (h/get-config-key! key))

(defwhen set-config-key "setting config key \"{key}\" to \"{value}\""
  [key value]
  (h/set-config-key! key value))

(defwhen parse-config "parsing the config"
  []
  (h/parse-config!))

(defwhen request-config-help "requesting config help"
  []
  (h/request-config-help!))

(defthen assert-result-ok-with-value "the result should be ok with value \"{expected}\""
  [expected]
  (should= expected (:ok (h/config-result))))

(defthen assert-result-error "the result should be an error"
  []
  (should (:error (h/config-result))))

(defthen assert-error-message-contains "the error message should contain \"{expected}\""
  [expected]
  (should (str/includes? (:error (h/config-result)) expected)))

(defthen assert-config-has-value "the config should have \"{key}\" set to \"{expected}\""
  [key expected]
  (should= expected (str (get (h/current-config) (keyword key)))))

(defthen assert-appears-before "\"{first-item}\" should appear before \"{second-item}\" in the output"
  [first-item second-item]
  (should (< (str/index-of (h/output) first-item)
             (str/index-of (h/output) second-item))))
