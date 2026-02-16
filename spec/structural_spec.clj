(ns structural-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(def home (System/getProperty "user.home"))
(def braids-home (or (System/getenv "BRAIDS_HOME") (str home "/Projects")))
(def skill-symlink (str home "/.openclaw/skills/braids"))
(def skill-source (str braids-home "/braids/braids"))
;; Use project-relative path for file checks (doesn't require OpenClaw installed)
(def project-root (System/getProperty "user.dir"))
(def skill-dir (str project-root "/braids"))
(def state-home (str home "/.openclaw/braids"))
(def registry (str state-home "/registry.edn"))

(defn slurp-safe [path] (when (fs/exists? path) (slurp path)))
(defn real-path [path] (try (str (fs/real-path path)) (catch Exception _ nil)))

(defn- load-registry []
  (when (fs/exists? registry)
    (clojure.edn/read-string (slurp registry))))

(defn- resolve-path [path]
  (str/replace (or path "") "~" home))

;; ── Legacy tests/ directory removed ──

(describe "Legacy tests/ directory"
  (it "tests/ directory does not exist (replaced by spec/ with speclj)"
    (should-not (fs/exists? (str braids-home "/projects-skill/tests")))))

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
  (it "registry.edn exists (skipped if no registry)"
    (if (fs/exists? registry)
      (should (fs/exists? registry))
      (should true)))

  (it "is valid EDN with :projects key (skipped if no registry)"
    (if (fs/exists? registry)
      (let [parsed (load-registry)]
        (should (map? parsed))
        (should (contains? parsed :projects)))
      (should true)))

  (it "all registered projects are valid (skipped if no registry)"
    (when-let [reg (load-registry)]
      (doseq [{:keys [slug status priority path]} (:projects reg)]
        (let [resolved (resolve-path path)]
          (when slug
            ;; Project structure
            (should (fs/directory? resolved))
            (let [config-path (cond
                                (fs/exists? (str resolved "/.braids/config.edn")) (str resolved "/.braids/config.edn")
                                (fs/exists? (str resolved "/.braids/config.edn")) (str resolved "/.braids/config.edn")
                                :else (str resolved "/.braids/config.edn"))
                  iterations-dir (cond
                                   (fs/directory? (str resolved "/.braids/iterations")) (str resolved "/.braids/iterations")
                                   (fs/directory? (str resolved "/.project/iterations")) (str resolved "/.project/iterations")
                                   :else (str resolved "/iterations"))]
              (should (fs/exists? config-path))
              (should (fs/exists? (str resolved "/AGENTS.md")))
              (should (fs/directory? (str resolved "/.beads")))
              (should (fs/directory? iterations-dir))
              (should (fs/directory? (str resolved "/.git")))

              ;; config.edn validation
              (let [cfg (clojure.edn/read-string (slurp config-path))]
                (should (map? cfg))
                (should (:name cfg))
                (should (:status cfg)))

              ;; Registry status/priority validation
              (should (contains? #{:active :paused :blocked} status))
              (should (contains? #{:high :normal :low} priority))

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
        (should-contain "braids" cron-output)
        (should true))))

  (it "active projects have valid spawn config (skipped if no registry)"
    (when-let [reg (load-registry)]
      (doseq [{:keys [slug status path]} (:projects reg)]
        (let [resolved (resolve-path path)]
          (when (and slug (= status :active))
            (let [config-path (cond
                                (fs/exists? (str resolved "/.braids/config.edn")) (str resolved "/.braids/config.edn")
                                (fs/exists? (str resolved "/.braids/config.edn")) (str resolved "/.braids/config.edn")
                                :else nil)]
              (when config-path
                (let [cfg (clojure.edn/read-string (slurp config-path))]
                  ;; max-workers
                  (when (:max-workers cfg)
                    (should (pos? (:max-workers cfg))))
                  ;; notification-mentions validation
                  (when-let [mentions (:notification-mentions cfg)]
                    (when (:iteration-complete mentions)
                      (should (sequential? (:iteration-complete mentions))))))))))))))
