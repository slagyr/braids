(ns braids.features.project-lifecycle-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Project lifecycle"

  (context "Init checks bd availability"
    (it "Init checks bd availability"
      (h/reset!)
      (h/set-bd-not-available)
      (h/set-no-registry)
      (h/check-prerequisites!)
      (should-not (empty? (h/prereq-errors)))
      (should (some #(clojure.string/includes? % "bd (beads) is not installed") (h/prereq-errors)))))

  (context "Init fails when registry exists without force"
    (it "Init fails when registry exists without force"
      (h/reset!)
      (h/set-bd-available)
      (h/set-registry-exists)
      (h/set-force-not-set)
      (h/check-prerequisites!)
      (should-not (empty? (h/prereq-errors)))
      (should (some #(clojure.string/includes? % "braids is already initialized") (h/prereq-errors)))))

  (context "Init allows reinit with force flag"
    (it "Init allows reinit with force flag"
      (h/reset!)
      (h/set-bd-available)
      (h/set-registry-exists)
      (h/set-force-set)
      (h/check-prerequisites!)
      (should (empty? (h/prereq-errors)))))

  (context "Init plans directory creation for fresh install"
    (it "Init plans directory creation for fresh install"
      (h/reset!)
      (h/set-braids-dir-not-exists)
      (h/set-braids-home-not-exists)
      (h/plan-init!)
      (should (some #{"create-braids-dir"} (h/plan-actions)))
      (should (some #{"create-braids-home"} (h/plan-actions)))
      (should (some #{"create-registry"} (h/plan-actions)))
      (should (some #{"save-config"} (h/plan-actions)))))

  (context "Init skips existing directories in plan"
    (it "Init skips existing directories in plan"
      (h/reset!)
      (h/set-braids-dir-exists)
      (h/set-braids-home-exists)
      (h/plan-init!)
      (should-not (some #{"create-braids-dir"} (h/plan-actions)))
      (should-not (some #{"create-braids-home"} (h/plan-actions)))
      (should (some #{"create-registry"} (h/plan-actions)))
      (should (some #{"save-config"} (h/plan-actions)))))

  (context "New project validates slug format"
    (it "New project validates slug format"
      (h/reset!)
      (h/set-new-project-slug "Bad Slug")
      (h/set-new-project-name "My Project")
      (h/set-new-project-goal "Build something")
      (h/validate-new-project!)
      (should-not (empty? (h/validation-errors)))
      (should (some #(clojure.string/includes? % "Invalid slug") (h/validation-errors)))))

  (context "New project rejects duplicate slug"
    (it "New project rejects duplicate slug"
      (h/reset!)
      (h/set-registry-with-project "my-project")
      (h/set-new-registry-entry "my-project")
      (h/add-to-registry!)
      (should (clojure.string/includes? (or (h/add-registry-error) "") "already exists"))))

  (context "New project builds config with defaults"
    (it "New project builds config with defaults"
      (h/reset!)
      (h/set-new-project-name "My Project")
      (h/build-project-config!)
      (should= :active (:status (h/project-config)))
      (should= :normal (:priority (h/project-config)))
      (should= :full (:autonomy (h/project-config)))
      (should= 1 (:max-workers (h/project-config)))
      (should= 1800 (:worker-timeout (h/project-config))))))
