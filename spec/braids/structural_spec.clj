(ns braids.structural-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (System/getProperty "user.dir"))
(def skill-dir (str project-root "/braids"))
(defn- slurp-safe [path] (when (fs/exists? path) (slurp path)))

;; ── Legacy tests/ directory removed ──

(describe "Legacy tests/ directory"
  (it "tests/ directory does not exist (replaced by spec/ with speclj)"
    (should-not (fs/exists? (str project-root "/tests")))))

;; ── Skill Directory ──

(describe "Skill Directory"
  (it "SKILL.md exists"
    (should (fs/exists? (str skill-dir "/SKILL.md"))))
  (it "references/ directory exists"
    (should (fs/directory? (str skill-dir "/references"))))
  (it "all reference files exist"
    (doseq [ref ["orchestrator.md" "worker.md" "agents-template.md"
                  "migration.md"]]
      (should (fs/exists? (str skill-dir "/references/" ref))))))

;; ── SKILL.md Format ──

(describe "SKILL.md Format"
  (it "has YAML frontmatter"
    (should (str/starts-with? (or (slurp-safe (str skill-dir "/SKILL.md")) "") "---")))
  (it "frontmatter has name field"
    (should-contain "name:" (slurp-safe (str skill-dir "/SKILL.md"))))
  (it "frontmatter has description field"
    (should-contain "description:" (slurp-safe (str skill-dir "/SKILL.md")))))

;; ── Spawn Config ──

(describe "Spawn Config"
  (it "orchestrator cron references braids (skipped if no cron config)"
    (should-contain "braids orch" (slurp (str project-root "/CONTRACTS.md")))))
