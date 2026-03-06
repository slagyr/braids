(ns braids.rename-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (System/getProperty "user.dir"))

(defn completed-iteration? [path]
  (let [rel (str (fs/relativize project-root path))]
    (and (str/starts-with? rel ".braids/iterations/")
         (let [iter-num (some-> (re-find #"iterations/(\d+)" rel) second)]
           (when iter-num
             (let [iter-edn (str project-root "/.braids/iterations/" iter-num "/iteration.edn")]
               (when (fs/exists? iter-edn)
                 (let [data (clojure.edn/read-string (slurp iter-edn))]
                   (= :complete (:status data))))))))))

(defn mutable-files
  "All text files excluding completed iterations, .git, .beads, and this spec."
  [root]
  (->> (fs/glob root "**")
       (filter fs/regular-file?)
       (filter (fn [p]
                 (let [name (str (fs/file-name p))]
                   (some #(str/ends-with? name %) [".md" ".clj" ".edn"]))))
       (remove (fn [p]
                 (let [rel (str (fs/relativize root p))]
                   (or (str/starts-with? rel ".git/")
                       (str/starts-with? rel ".beads/")
                       (str/includes? rel "rename_spec")))))
       (remove completed-iteration?)))

(defn files-containing [root pattern]
  (->> (mutable-files root)
       (filter (fn [f]
                 (let [content (slurp (str f))]
                   (re-find pattern content))))
       (map #(str (fs/relativize root %)))))

(describe "Skill rename: projects → braids"

  (it "skill directory is braids/ not projects/"
    (should (fs/directory? (str project-root "/braids")))
    (should-not (fs/directory? (str project-root "/projects"))))

  (it "SKILL.md frontmatter name is 'braids'"
    (let [skill-md (slurp (str project-root "/braids/SKILL.md"))
          name-line (some->> (str/split-lines skill-md)
                             (filter #(re-find #"^name:" %))
                             first)]
      (should-contain "braids" name-line)))

  (it "no mutable files reference 'skills/projects' path"
    (let [matches (files-containing project-root #"skills/projects")]
      (should= [] matches)))

  (it "no mutable files reference '~/.openclaw/projects' state path"
    (let [matches (files-containing project-root #"~/\.openclaw/projects[/\s\)]")]
      (should= [] matches)))

  (it "no mutable files reference 'the projects skill'"
    (let [matches (files-containing project-root #"(?i)projects skill")]
      (should= [] matches)))

  (it "README install instructions reference braids/"
    (let [readme (slurp (str project-root "/README.md"))]
      (should-contain "braids" readme)
      (should= [] (files-containing project-root #"skills/projects"))))

  (it "cron job prompt references braids"
    (let [skill-md (slurp (str project-root "/braids/SKILL.md"))]
      (should (re-find #"references/orchestrator\.md" skill-md)))))

(describe "Directory rename: .project → .braids"

  (it ".braids directory exists"
    (should (fs/directory? (str project-root "/.braids"))))

  (it ".project directory does not exist"
    (should-not (fs/directory? (str project-root "/.project"))))

  (it "no mutable files reference '.project/' as primary path (except compat/migration code)"
    (let [matches (->> (files-containing project-root #"\.project/")
                       (remove #(str/includes? % "migration.md"))
                       ;; Source files with .project/ fallback for backward compatibility
                       (remove #(str/includes? % "ready_io.clj"))
                       (remove #(str/includes? % "orch_io.clj"))
                       (remove #(str/includes? % "status_io.clj"))
                       (remove #(str/includes? % "iteration_io.clj"))
                       ;; Specs with backward-compat fallback for .project/
                       (remove #(str/includes? % "structural_spec.clj"))
                       (remove #(str/includes? % "integration_smoke_spec.clj")))]
      (should= [] matches)))

  (it "source files use .braids/ as primary path (not .project/)"
    ;; Verify that .braids/ appears BEFORE .project/ in fallback chains
    (doseq [src-file ["src/braids/ready_io.clj" "src/braids/orch_io.clj"
                       "src/braids/status_io.clj" "src/braids/iteration_io.clj"]]
      (let [content (slurp (str project-root "/" src-file))
            braids-idx (str/index-of content ".braids/")
            project-idx (str/index-of content ".project/")]
        (when (and braids-idx project-idx)
          (should (< braids-idx project-idx)))))))

(run-specs)
