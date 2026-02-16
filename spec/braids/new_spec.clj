(ns braids.new-spec
  (:require [speclj.core :refer :all]
            [braids.new :as new]))

(describe "braids.new"

  (describe "validate-slug"

    (it "accepts valid slugs"
      (should= [] (new/validate-slug "my-project"))
      (should= [] (new/validate-slug "foo"))
      (should= [] (new/validate-slug "a1b2")))

    (it "rejects empty slug"
      (should-not= [] (new/validate-slug nil))
      (should-not= [] (new/validate-slug "")))

    (it "rejects slugs with uppercase"
      (should-not= [] (new/validate-slug "MyProject")))

    (it "rejects slugs with spaces"
      (should-not= [] (new/validate-slug "my project")))

    (it "rejects slugs starting with hyphen"
      (should-not= [] (new/validate-slug "-foo")))

    (it "rejects slugs ending with hyphen"
      (should-not= [] (new/validate-slug "foo-"))))

  (describe "validate-new-params"

    (it "validates complete params"
      (should= [] (new/validate-new-params
                    {:slug "my-project" :name "My Project" :goal "Build something"})))

    (it "requires slug"
      (should-not= [] (new/validate-new-params
                        {:name "My Project" :goal "Build something"})))

    (it "requires name"
      (should-not= [] (new/validate-new-params
                        {:slug "my-project" :goal "Build something"})))

    (it "requires goal (stored in AGENTS.md, not config.edn)"
      (should-not= [] (new/validate-new-params
                        {:slug "my-project" :name "My Project"})))

    (it "validates slug format"
      (should-not= [] (new/validate-new-params
                        {:slug "Bad Slug" :name "My Project" :goal "Build something"}))))

  (describe "build-project-config"

    (it "builds config with defaults"
      (let [config (new/build-project-config
                     {:slug "my-project" :name "My Project"})]
        (should= "My Project" (:name config))
        (should= :active (:status config))
        (should= :normal (:priority config))
        (should= :full (:autonomy config))
        (should= :on-demand (:checkin config))
        (should= 1 (:max-workers config))
        (should= 3600 (:worker-timeout config))
        (should= nil (:channel config))
        (should-not-contain :goal config)
        (should-not-contain :guardrails config)
        (should (every? true? (vals (:notifications config))))))

    (it "allows overriding defaults"
      (let [config (new/build-project-config
                     {:slug "my-project" :name "My Project"
                      :priority :high :autonomy :ask-first :channel "12345"
                      :max-workers 3})]
        (should= :high (:priority config))
        (should= :ask-first (:autonomy config))
        (should= "12345" (:channel config))
        (should= 3 (:max-workers config))))

    (it "does not include goal or guardrails in config (those go in AGENTS.md)"
      (let [config (new/build-project-config
                     {:slug "x" :name "X"})]
        (should-not-contain :goal config)
        (should-not-contain :guardrails config))))

  (describe "build-registry-entry"

    (it "builds a registry entry from params"
      (let [entry (new/build-registry-entry {:slug "my-project" :priority :high} "/home/user/Projects/my-project")]
        (should= "my-project" (:slug entry))
        (should= :active (:status entry))
        (should= :high (:priority entry))
        (should= "/home/user/Projects/my-project" (:path entry))))

    (it "defaults priority to normal"
      (let [entry (new/build-registry-entry {:slug "foo"} "/path")]
        (should= :normal (:priority entry)))))

  (describe "build-iteration-content"

    (it "generates iteration 001 EDN"
      (let [iter (new/build-iteration-content)]
        (should= 1 (:number iter))
        (should= :planning (:status iter))
        (should= [] (:stories iter)))))

  (describe "build-agents-md"

    (it "generates AGENTS.md content"
      (let [content (new/build-agents-md)]
        (should-contain "braids" content)
        (should-contain "config.edn" content)
        (should-not-contain "PROJECT.md" content))))

  (describe "add-to-registry"

    (it "adds a project to an existing registry"
      (let [registry {:projects [{:slug "existing" :status :active :priority :normal :path "/p/existing"}]}
            entry {:slug "new-proj" :status :active :priority :high :path "/p/new-proj"}
            result (new/add-to-registry registry entry)]
        (should= 2 (count (:projects result)))
        (should= "new-proj" (:slug (second (:projects result))))))

    (it "rejects duplicate slug"
      (let [registry {:projects [{:slug "foo" :status :active :priority :normal :path "/p/foo"}]}
            entry {:slug "foo" :status :active :priority :high :path "/p/foo2"}]
        (should-throw Exception (new/add-to-registry registry entry))))))
