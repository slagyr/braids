(ns braids.features.steps.configuration
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.config :as config]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defn- list-config* []
  (let [cfg (g/get :config-map)
        output (config/config-list cfg)]
    (g/assoc! :output output)))

(defn- get-config-key* [key-str]
  (let [cfg (g/get :config-map)
        result (config/config-get cfg key-str)]
    (g/assoc! :config-result result)))

(defn- set-config-key* [key-str value-str]
  (let [cfg (g/get :config-map)
        updated (config/config-set cfg key-str value-str)]
    (g/assoc! :config-map updated)))

(defn- parse-config* []
  (let [edn-str (g/get :config-edn-str)
        result (config/parse-config edn-str)]
    (g/assoc! :config-map result)))

(defn- request-config-help* []
  (let [output (config/config-help)]
    (g/assoc! :output output)))

(defgiven config-with-values "a config with values:"
  [table]
  (let [{:keys [headers rows]} table
        m (reduce (fn [acc row]
                    (let [kv (zipmap headers row)]
                      (assoc acc (keyword (get kv "key")) (get kv "value"))))
                  {}
                  rows)]
    (g/assoc! :config-map m)))

(defgiven empty-config-string "an empty config string"
  []
  (g/assoc! :config-edn-str "{}"))

(defwhen list-config "listing the config"
  []
  (list-config*))

(defwhen get-config-key "getting config key {key:string}"
  [key]
  (get-config-key* key))

(defwhen set-config-key "setting config key {key:string} to {value:string}"
  [key value]
  (set-config-key* key value))

(defwhen parse-config "parsing the config"
  []
  (parse-config*))

(defwhen request-config-help "requesting config help"
  []
  (request-config-help*))

(defthen assert-result-ok-with-value "the result should be ok with value {expected:string}"
  [expected]
  (should= expected (:ok (g/get :config-result))))

(defthen assert-result-error "the result should be an error"
  []
  (should (:error (g/get :config-result))))

(defthen assert-error-message-contains "the error message should contain {expected:string}"
  [expected]
  (should (str/includes? (:error (g/get :config-result)) expected)))

(defthen assert-config-has-value "the config should have {key:string} set to {expected:string}"
  [key expected]
  (should= expected (str (get (g/get :config-map) (keyword key)))))

(defthen assert-appears-before "{first-item:string} should appear before {second-item:string} in the output"
  [first-item second-item]
  (should (< (str/index-of (g/get :output) first-item)
             (str/index-of (g/get :output) second-item))))
