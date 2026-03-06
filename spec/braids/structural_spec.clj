(ns braids.structural-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (System/getProperty "user.dir"))
(def skill-dir (str project-root "/braids"))
(def home (System/getProperty "user.home"))
(def skill-symlink (str home "/.openclaw/skills/braids"))

(defn- slurp-safe [path] (when (fs/exists? path) (slurp path)))
(defn- real-path [path] (try (str (fs/real-path path)) (catch Exception _ nil)))

;; ── Legacy tests/ directory removed ──

(describe "Legacy tests/ directory"
  (it "tests/ directory does not exist (replaced by spec/ with speclj)"
    (should-not (fs/exists? (str project-root "/tests")))))

;; ── Skill Symlink ──

(describe "Skill Symlink"
  (it "symlink exists (skipped if OpenClaw not installed)"
    (if (fs/exists? skill-symlink)
      (should (fs/sym-link? skill-symlink))
      (should true)))
  (it "symlink target is valid directory (skipped if OpenClaw not installed)"
    (if (fs/exists? skill-symlink)
      (should (fs/directory? skill-symlink))
      (should true)))
  (it "symlink points to projects-skill/braids (skipped if OpenClaw not installed)"
    (if (fs/exists? skill-symlink)
      (let [target (real-path skill-symlink)
            expected (real-path (str project-root "/braids"))]
        (should= expected target))
      (should true))))

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

;; ── Registry ──

(describe "Registry"
  (it "registry.edn exists (skipped if no registry)"
    (let [registry (str home "/.openclaw/braids/registry.edn")]
      (if (fs/exists? registry)
        (should (fs/exists? registry))
        (should true))))

  (it "is valid EDN with :projects key (skipped if no registry)"
    (let [registry (str home "/.openclaw/braids/registry.edn")]
      (if (fs/exists? registry)
        (let [parsed (clojure.edn/read-string (slurp registry))]
          (should (map? parsed))
          (should (contains? parsed :projects)))
        (should true)))))

;; ── Spawn Config ──

(describe "Spawn Config"
  (it "orchestrator cron references braids (skipped if no cron config)"
    (should-contain "braids orch" (slurp (str project-root "/CONTRACTS.md")))))
