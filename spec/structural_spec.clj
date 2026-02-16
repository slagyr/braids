(ns structural-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(def home (System/getProperty "user.home"))
(def projects-home (or (System/getenv "PROJECTS_HOME") (str home "/Projects")))
(def skill-symlink (str home "/.openclaw/skills/projects"))
(def skill-source (str projects-home "/projects-skill/projects"))
;; Use project-relative path for file checks (doesn't require OpenClaw installed)
(def skill-dir skill-source)
(def registry (str projects-home "/registry.md"))

(defn slurp-safe [path] (when (fs/exists? path) (slurp path)))
(defn real-path [path] (try (str (fs/real-path path)) (catch Exception _ nil)))

;; ── Legacy tests/ directory removed ──

(describe "Legacy tests/ directory"
  (it "tests/ directory does not exist (replaced by spec/ with speclj)"
    (should-not (fs/exists? (str projects-home "/projects-skill/tests")))))

;; ── Skill Symlink ──

(describe "Skill Symlink"
  ;; These tests only run when OpenClaw is installed (symlink exists)
  (it "symlink exists (skipped if OpenClaw not installed)"
    (if (fs/exists? skill-symlink)
      (should (fs/sym-link? skill-symlink))
      (should true)))
  (it "symlink target is valid directory (skipped if OpenClaw not installed)"
    (if (fs/exists? skill-symlink)
      (should (fs/directory? skill-symlink))
      (should true)))
  (it "symlink points to projects-skill/projects (skipped if OpenClaw not installed)"
    (if (fs/exists? skill-symlink)
      (should= (real-path skill-symlink) (real-path skill-source))
      (should true))))

;; ── Skill Directory ──

(describe "Skill Directory"
  (it "SKILL.md exists"
    (should (fs/exists? (str skill-dir "/SKILL.md"))))
  (it "references/ directory exists"
    (should (fs/directory? (str skill-dir "/references"))))
  (it "all reference files exist"
    (doseq [ref ["orchestrator.md" "worker.md" "agents-template.md"
                  "status-dashboard.md" "migration.md"]]
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
  (it "registry.md exists"
    (should (fs/exists? registry)))

  (it "has table header"
    (should-contain "| Slug | Status | Priority | Path |" (slurp-safe registry)))

  (it "all registered projects are valid"
    (let [content (slurp-safe registry)
          lines (str/split-lines (or content ""))
          data-lines (->> lines
                         (filter #(str/includes? % "|"))
                         (drop 2)
                         (remove #(str/starts-with? (str/trim %) "|-")))]
      (doseq [line data-lines]
        (let [cols (->> (str/split line #"\|")
                        (map str/trim)
                        (remove str/blank?))
              [slug status priority path] cols
              resolved (str/replace (or path "") "~" home)]
          (when (and slug (not= slug "Slug") (not (str/starts-with? slug "-")))
            ;; Project structure
            (should (fs/directory? resolved))
            ;; PROJECT.md in .project/ (new) or root (legacy)
            (let [project-md-path (if (fs/exists? (str resolved "/.project/PROJECT.md"))
                                    (str resolved "/.project/PROJECT.md")
                                    (str resolved "/PROJECT.md"))
                  iterations-dir (if (fs/directory? (str resolved "/.project/iterations"))
                                   (str resolved "/.project/iterations")
                                   (str resolved "/iterations"))]
            (should (fs/exists? project-md-path))
            (should (fs/exists? (str resolved "/AGENTS.md")))
            (should (fs/directory? (str resolved "/.beads")))
            (should (fs/directory? iterations-dir))
            (should (fs/directory? (str resolved "/.git")))

            ;; PROJECT.md fields
            (let [pmd (slurp-safe project-md-path)]
              (should (re-find #"(?im)(^\- \*\*Status:\*\*|^Status:)" pmd))
              (should-contain "## Goal" pmd)
              (should-contain "## Guardrails" pmd))

            ;; Registry status/priority validation
            (should (contains? #{"active" "paused" "blocked"} status))
            (should (contains? #{"high" "normal" "low"} priority))

            ;; Iteration validation
            (when (fs/directory? iterations-dir)
              (doseq [iter-dir (sort (fs/list-dir iterations-dir))]
                (let [iter-name (str (fs/file-name iter-dir))
                      iter-md (str iter-dir "/ITERATION.md")]
                  (when (and (fs/directory? iter-dir) (re-matches #"\d{3}" iter-name) (fs/exists? iter-md))
                    (let [icontent (slurp iter-md)
                          iter-status (some-> (re-find #"(?i)Status:\*\*\s*(.*)|Status:\s*(.*)" icontent)
                                              rest
                                              (->> (remove nil?) first str/trim))]
                      (should (re-find #"(?i)Status:" icontent))
                      (should-contain "## Stories" icontent)
                      (should (contains? #{"planning" "active" "complete"} iter-status))
                      (when (= iter-status "active")
                        (doseq [story-line (->> (str/split-lines icontent)
                                                (filter #(re-find #"^- [a-z]" %))
                                                (remove #(str/includes? % "**")))]
                          (when-let [bead-id (some-> (re-find #"([a-z]+-[a-z0-9-]+)" story-line) second)]
                            (let [r (p/shell {:dir resolved :out :string :err :string :continue true}
                                             "bd" "show" bead-id)]
                              (should= 0 (:exit r))))))))))))))))))

;; ── Spawn Config ──

(describe "Spawn Config"
  (it "orchestrator cron job exists"
    (let [cron-output (try (:out (p/shell {:out :string :err :string :continue true} "openclaw" "cron" "list"))
                           (catch Exception _ nil))]
      (if cron-output
        (should-contain "projects" cron-output)
        (should true)))) ;; skip if openclaw not in PATH

  (it "active projects have valid spawn config"
    (let [content (slurp-safe registry)
          lines (str/split-lines (or content ""))
          data-lines (->> lines (filter #(str/includes? % "|")) (drop 2) (remove #(str/starts-with? (str/trim %) "|-")))]
      (doseq [line data-lines]
        (let [cols (->> (str/split line #"\|") (map str/trim) (remove str/blank?))
              [slug status _priority path] cols
              resolved (str/replace (or path "") "~" home)]
          (when (and slug (= status "active"))
            (let [pmd-path (if (fs/exists? (str resolved "/.project/PROJECT.md"))
                             (str resolved "/.project/PROJECT.md")
                             (str resolved "/PROJECT.md"))]
            (when (fs/exists? pmd-path)
            (let [pmd (slurp pmd-path)]
              ;; MaxWorkers
              (let [mw (some-> (re-find #"MaxWorkers.*?(\d+)" pmd) second)]
                (when mw (should (pos? (parse-long mw)))))
              ;; Channel (optional — some projects skip notifications)
              ;; just verify it's parseable if present
              ;; iteration-complete mention
              (let [ic-line (some->> (str/split-lines pmd) (filter #(str/includes? % "iteration-complete")) first)]
                (when (and ic-line (re-find #"(?i)on" ic-line))
                  (should (re-find #"mention.*<@\d+>" ic-line)))))))))))))
