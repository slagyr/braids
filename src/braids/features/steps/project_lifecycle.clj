(ns braids.features.steps.project-lifecycle
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.init :as init]
            [braids.new :as new-proj]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defn- check-prerequisites* []
  (let [opts (g/get :prereq-opts)
        result (init/check-prerequisites opts)]
    (g/assoc! :prereq-result result)))

(defn- plan-init* []
  (let [opts (merge {:registry-path "/tmp/braids/registry.edn"
                     :config-path "/tmp/braids/config.edn"}
                    (g/get :plan-opts))
        result (init/plan-init opts)]
    (g/assoc! :plan-result result)))

(defn- validate-new-project* []
  (let [params (g/get :new-project-params)
        result (new-proj/validate-new-params params)]
    (g/assoc! :validation-result result)))

(defn- add-to-registry* []
  (let [registry (g/get :registry)
        entry (g/get :new-entry)]
    (try
      (let [result (new-proj/add-to-registry registry entry)]
        (g/assoc! :add-registry-result {:ok result}))
      (catch Exception e
        (g/assoc! :add-registry-result {:error (.getMessage e)})))))

(defn- build-project-config* []
  (let [params (g/get :new-project-params)
        result (new-proj/build-project-config params)]
    (g/assoc! :project-config-result result)))

(defgiven bd-not-available "bd is not available"
  []
  (g/assoc-in! [:prereq-opts :bd-available?] false))

(defgiven bd-available "bd is available"
  []
  (g/assoc-in! [:prereq-opts :bd-available?] true))

(defgiven no-registry "no registry exists"
  []
  (g/assoc-in! [:prereq-opts :registry-exists?] false))

(defgiven registry-exists "a registry already exists"
  []
  (g/assoc-in! [:prereq-opts :registry-exists?] true))

(defgiven force-not-set "force is not set"
  []
  (g/assoc-in! [:prereq-opts :force?] false))

(defgiven force-set "force is set"
  []
  (g/assoc-in! [:prereq-opts :force?] true))

(defgiven braids-dir-not-exists "braids dir does not exist"
  []
  (g/update! :plan-opts merge {:braids-dir "/tmp/braids"
                                :braids-dir-exists? false}))

(defgiven braids-dir-exists "braids dir already exists"
  []
  (g/update! :plan-opts merge {:braids-dir "/tmp/braids"
                                :braids-dir-exists? true}))

(defgiven braids-home-not-exists "braids home does not exist"
  []
  (g/update! :plan-opts merge {:braids-home "/tmp/projects"
                                :braids-home-exists? false}))

(defgiven braids-home-exists "braids home already exists"
  []
  (g/update! :plan-opts merge {:braids-home "/tmp/projects"
                                :braids-home-exists? true}))

(defgiven new-project-slug "a new project with slug {slug:string}"
  [slug]
  (g/assoc-in! [:new-project-params :slug] slug))

(defgiven new-project-name "a new project with name {name:string}"
  [name]
  (g/assoc-in! [:new-project-params :name] name))

(defgiven set-name "name {name:string}"
  [name]
  (g/assoc-in! [:new-project-params :name] name))

(defgiven set-goal "goal {goal:string}"
  [goal]
  (g/assoc-in! [:new-project-params :goal] goal))

(defgiven registry-with-project "a registry with project {slug:string}"
  [slug]
  (g/assoc! :registry {:projects [{:slug slug :status :active :path (str "/projects/" slug)}]}))

(defgiven new-registry-entry "a new registry entry with slug {slug:string}"
  [slug]
  (g/assoc! :new-entry {:slug slug :status :active :path (str "/projects/" slug)}))

(defwhen check-prerequisites "checking prerequisites"
  []
  (check-prerequisites*))

(defwhen plan-init "planning init"
  []
  (plan-init*))

(defwhen validate-new-project "validating new project params"
  []
  (validate-new-project*))

(defwhen add-to-registry "adding the entry to the registry"
  []
  (add-to-registry*))

(defwhen build-project-config "building the project config"
  []
  (build-project-config*))

(defthen assert-prereq-fail "prerequisites should fail with {expected:string}"
  [expected]
  (should-not (empty? (g/get :prereq-result)))
  (should (some #(str/includes? % expected) (g/get :prereq-result))))

(defthen assert-prereq-pass "prerequisites should pass"
  []
  (should (empty? (g/get :prereq-result))))

(defthen assert-plan-include "the plan should include {action:string}"
  [action]
  (should (some #{action} (mapv (comp name :action) (g/get :plan-result)))))

(defthen assert-plan-not-include "the plan should not include {action:string}"
  [action]
  (should-not (some #{action} (mapv (comp name :action) (g/get :plan-result)))))

(defthen assert-validation-fail "validation should fail with {expected:string}"
  [expected]
  (should-not (empty? (g/get :validation-result)))
  (should (some #(str/includes? % expected) (g/get :validation-result))))

(defthen assert-should-fail "it should fail with {expected:string}"
  [expected]
  (should (str/includes? (or (g/get-in [:add-registry-result :error]) "") expected)))

(defthen assert-config-value #"^the config (\S+) should be \"([^\"]+)\"$"
  [key expected]
  (let [expected-kw (if (re-matches #"^:.*" expected) (keyword (subs expected 1)) (keyword expected))]
    (should= expected-kw ((keyword key) (g/get :project-config-result)))))

(defthen assert-config-number #"^the config (\S+) should be (\d+)$"
  [key expected]
  (should= (parse-long expected) ((keyword key) (g/get :project-config-result))))
