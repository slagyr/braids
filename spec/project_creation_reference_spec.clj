(ns project-creation-reference-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (str (System/getProperty "user.dir")))
(def ref-file (str project-root "/braids/references/project-creation.md"))
(def skill-file (str project-root "/braids/SKILL.md"))
(def init-file (str project-root "/braids/references/init.md"))

(describe "project-creation.md Reference"
  (it "exists"
    (should (fs/exists? ref-file)))

  (it "has all required sections"
    (let [content (slurp ref-file)]
      (doseq [section ["Gather Information" "Suggest Defaults" "Discord Channel"
                        "Scaffold" "Generate config.edn" "AGENTS.md"
                        "Iteration 001" "Seed Stories" "Registry" "Review with the Human"]]
        (should-contain section content))))

  (context "Key content"
    (it "references git init"
      (should-contain "git init" (slurp ref-file)))
    (it "references bd init"
      (should-contain "bd init" (slurp ref-file)))
    (it "references agents-template"
      (should-contain "agents-template" (slurp ref-file)))
    (it "has slug validation"
      (should-contain "slug" (slurp ref-file)))
    (it "no TODO placeholders"
      (should-contain "not a placeholder" (slurp ref-file)))
    (it "references config.edn format"
      (should-contain ":status" (slurp ref-file)))
    (it "references notifications config"
      (should-contain "notifications" (slurp ref-file))))

  (context "Old script removed"
    (it "projects-init script removed"
      (should-not (fs/exists? (str project-root "/braids/bin/projects-init"))))
    (it "test_projects_init.sh removed"
      (should-not (fs/exists? (str project-root "/tests/test_projects_init.sh")))))

  (context "SKILL.md updated"
    (it "references project-creation.md"
      (should-contain "project-creation.md" (slurp skill-file)))
    (it "no longer references projects-init"
      (should-not (str/includes? (slurp skill-file) "projects-init"))))

  (context "init.md updated"
    (it "references project-creation.md"
      (should-contain "project-creation.md" (slurp init-file)))
    (it "no longer references projects-init"
      (should-not (str/includes? (slurp init-file) "projects-init")))))
