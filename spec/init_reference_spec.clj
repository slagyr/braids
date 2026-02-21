(ns init-reference-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (str (System/getProperty "user.dir")))
(def skill-dir (str project-root "/braids"))
(def init-ref (str skill-dir "/references/init.md"))

(describe "Init Reference (references/init.md)"
  (it "init.md exists"
    (should (fs/exists? init-ref)))

  (it "contains all required sections"
    (let [content (slurp init-ref)]
      (doseq [section ["Install the Skill" "Verify beads" "Create BRAIDS_HOME"
                        "Orchestrator Cron" "Verification"]]
        (should-contain section content))))

  (it "references project-creation.md"
    (should-contain "project-creation.md" (slurp init-ref)))

  (it "references orchestrator.md"
    (should-contain "orchestrator.md" (slurp init-ref)))

  (it "references registry.edn (not registry.md)"
    (let [content (slurp init-ref)]
      (should-contain "registry.edn" content)
      (should-not (str/includes? content "registry.md"))))

  (it "uses EDN {:projects []} format for registry"
    (should-contain "{:projects []}" (slurp init-ref)))

  (it "cron message says 'braids orchestrator' not 'projects orchestrator'"
    (let [content (slurp init-ref)]
      (should-contain "braids orchestrator" content)
      (should-not (str/includes? content "projects orchestrator"))))

  (it "SKILL.md references init.md"
    (should-contain "init.md" (slurp (str skill-dir "/SKILL.md"))))

  (it "README.md references init.md"
    (let [readme (str project-root "/README.md")]
      (should (and (fs/exists? readme) (str/includes? (slurp readme) "init.md"))))))
