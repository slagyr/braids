(ns braids.features.steps.project-lifecycle
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(defgiven bd-not-available "bd is not available"
  []
  (h/set-bd-not-available))

(defgiven bd-available "bd is available"
  []
  (h/set-bd-available))

(defgiven no-registry "no registry exists"
  []
  (h/set-no-registry))

(defgiven registry-exists "a registry already exists"
  []
  (h/set-registry-exists))

(defgiven force-not-set "force is not set"
  []
  (h/set-force-not-set))

(defgiven force-set "force is set"
  []
  (h/set-force-set))

(defgiven braids-dir-not-exists "braids dir does not exist"
  []
  (h/set-braids-dir-not-exists))

(defgiven braids-dir-exists "braids dir already exists"
  []
  (h/set-braids-dir-exists))

(defgiven braids-home-not-exists "braids home does not exist"
  []
  (h/set-braids-home-not-exists))

(defgiven braids-home-exists "braids home already exists"
  []
  (h/set-braids-home-exists))

(defgiven new-project-slug "a new project with slug \"{slug}\""
  [slug]
  (h/set-new-project-slug slug))

(defgiven new-project-name "a new project with name \"{name}\""
  [name]
  (h/set-new-project-name name))

(defgiven set-name "name \"{name}\""
  [name]
  (h/set-new-project-name name))

(defgiven set-goal "goal \"{goal}\""
  [goal]
  (h/set-new-project-goal goal))

(defgiven registry-with-project "a registry with project \"{slug}\""
  [slug]
  (h/set-registry-with-project slug))

(defgiven new-registry-entry "a new registry entry with slug \"{slug}\""
  [slug]
  (h/set-new-registry-entry slug))

(defwhen check-prerequisites "checking prerequisites"
  []
  (h/check-prerequisites!))

(defwhen plan-init "planning init"
  []
  (h/plan-init!))

(defwhen validate-new-project "validating new project params"
  []
  (h/validate-new-project!))

(defwhen add-to-registry "adding the entry to the registry"
  []
  (h/add-to-registry!))

(defwhen build-project-config "building the project config"
  []
  (h/build-project-config!))

(defthen assert-prereq-fail "prerequisites should fail with \"{expected}\""
  [expected]
  (should-not (empty? (h/prereq-errors)))
  (should (some #(str/includes? % expected) (h/prereq-errors))))

(defthen assert-prereq-pass "prerequisites should pass"
  []
  (should (empty? (h/prereq-errors))))

(defthen assert-plan-include "the plan should include \"{action}\""
  [action]
  (should (some #{action} (h/plan-actions))))

(defthen assert-plan-not-include "the plan should not include \"{action}\""
  [action]
  (should-not (some #{action} (h/plan-actions))))

(defthen assert-validation-fail "validation should fail with \"{expected}\""
  [expected]
  (should-not (empty? (h/validation-errors)))
  (should (some #(str/includes? % expected) (h/validation-errors))))

(defthen assert-should-fail "it should fail with \"{expected}\""
  [expected]
  (should (str/includes? (or (h/add-registry-error) "") expected)))

(defthen assert-config-value "the config {key} should be \"{expected}\""
  [key expected]
  (let [expected-kw (if (re-matches #"^:.*" expected) (keyword (subs expected 1)) (keyword expected))]
    (should= expected-kw ((keyword key) (h/project-config)))))

(defthen assert-config-number "the config {key} should be {expected:int}"
  [key expected]
  (should= expected ((keyword key) (h/project-config))))
