(ns braids.init-spec
  (:require [speclj.core :refer :all]
            [braids.init :as init]))

(describe "braids.init"

  (describe "check-prerequisites"
    (it "returns no errors when all prerequisites are met"
      (let [result (init/check-prerequisites {:braids-dir-exists? false
                                               :registry-exists? false
                                               :bd-available? true})]
        (should= [] result)))

    (it "returns error when bd is not available"
      (let [result (init/check-prerequisites {:braids-dir-exists? false
                                               :registry-exists? false
                                               :bd-available? false})]
        (should= ["bd (beads) is not installed. Install it from https://github.com/nickthecook/bd"] result)))

    (it "returns error when already initialized (registry exists)"
      (let [result (init/check-prerequisites {:braids-dir-exists? true
                                               :registry-exists? true
                                               :bd-available? true})]
        (should= ["braids is already initialized (registry.edn exists). Use --force to reinitialize."] result)))

    (it "allows reinit when force flag is set"
      (let [result (init/check-prerequisites {:braids-dir-exists? true
                                               :registry-exists? true
                                               :bd-available? true
                                               :force? true})]
        (should= [] result))))

  (describe "plan-init"
    (it "plans all actions for fresh install"
      (let [plan (init/plan-init {:braids-dir "/home/user/.openclaw/braids"
                                   :braids-home "/home/user/Projects"
                                   :registry-path "/home/user/.openclaw/braids/registry.edn"
                                   :braids-dir-exists? false
                                   :braids-home-exists? false})]
        (should= [:create-braids-dir :create-braids-home :create-registry]
                 (map :action plan))))

    (it "skips existing directories"
      (let [plan (init/plan-init {:braids-dir "/home/user/.openclaw/braids"
                                   :braids-home "/home/user/Projects"
                                   :registry-path "/home/user/.openclaw/braids/registry.edn"
                                   :braids-dir-exists? true
                                   :braids-home-exists? true})]
        (should= [:create-registry]
                 (map :action plan))))

    (it "includes all necessary paths in plan"
      (let [plan (init/plan-init {:braids-dir "/tmp/braids"
                                   :braids-home "/tmp/projects"
                                   :registry-path "/tmp/braids/registry.edn"
                                   :braids-dir-exists? false
                                   :braids-home-exists? false})
            actions (into {} (map (juxt :action identity) plan))]
        (should= "/tmp/braids" (:path (:create-braids-dir actions)))
        (should= "/tmp/projects" (:path (:create-braids-home actions)))
        (should= "/tmp/braids/registry.edn" (:path (:create-registry actions))))))

  (describe "format-result"
    (it "formats success message"
      (let [msg (init/format-result {:success? true
                                      :braids-dir "/home/user/.openclaw/braids"
                                      :braids-home "/home/user/Projects"
                                      :registry-path "/home/user/.openclaw/braids/registry.edn"
                                      :actions-taken [:create-braids-dir :create-braids-home :create-registry]})]
        (should-contain "✓ braids initialized" msg)
        (should-contain "/home/user/.openclaw/braids" msg)
        (should-contain "/home/user/Projects" msg)))

    (it "formats error message"
      (let [msg (init/format-result {:success? false
                                      :errors ["bd not found"]})]
        (should-contain "Error" msg)
        (should-contain "bd not found" msg)))))
