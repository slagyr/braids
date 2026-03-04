(ns braids.integration-spec
  "Comprehensive integration tests that create real projects, iterations, and beads,
   then run all CLI commands to verify end-to-end workflows."
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [braids.core :as core]
            [braids.new :as new]
            [braids.new-io :as new-io]
            [braids.config :as config]
            [braids.config-io :as config-io]
            [braids.init :as init]
            [braids.ready :as ready]
            [braids.list :as list]
            [braids.iteration :as iter]
            [braids.orch :as orch]
            [braids.edn-format :refer [edn-format]]
            [braids.registry :as registry]))

;; ── Helpers ──

(def ^:dynamic *test-dir* nil)

(defn temp-dir
  "Create a temporary directory for testing."
  [prefix]
  (str (fs/create-temp-dir {:prefix prefix})))

(defn setup-test-env
  "Create isolated test environment with its own registry, config, and braids-home."
  []
  (let [root (temp-dir "braids-integration-")
        braids-home (str root "/projects")
        state-dir (str root "/state")
        registry-path (str state-dir "/registry.edn")
        config-path (str state-dir "/config.edn")]
    (fs/create-dirs braids-home)
    (fs/create-dirs state-dir)
    (spit config-path (edn-format {:braids-home braids-home}))
    (spit registry-path (edn-format {:projects []}))
    {:root root
     :braids-home braids-home
     :state-dir state-dir
     :registry-path registry-path
     :config-path config-path}))

(defn create-test-project
  "Create a project using new-io/run-new with isolated paths."
  [env slug & {:keys [name goal priority] :or {name nil goal nil priority "normal"}}]
  (let [project-name (or name (str "Test " slug))
        project-goal (or goal (str "Goal for " slug))]
    (new-io/run-new
      [slug
       "--name" project-name
       "--goal" project-goal
       "--priority" priority
       "--braids-home" (:braids-home env)]
      {:registry-file (:registry-path env)})))

(defn project-dir [env slug]
  (str (:braids-home env) "/" slug))

(defn read-registry [env]
  (registry/parse-registry (slurp (:registry-path env))))

(defn read-project-config [env slug]
  (edn/read-string (slurp (str (project-dir env slug) "/.braids/config.edn"))))

(defn read-iteration-edn [env slug iter-num]
  (let [padded (format "%03d" iter-num)]
    (edn/read-string (slurp (str (project-dir env slug) "/.braids/iterations/" padded "/iteration.edn")))))

(defn bd-in-project
  "Run bd command in a project directory and return {:exit :out :err}."
  [env slug & args]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" (str/join " " args))]
    {:exit (:exit result) :out (:out result) :err (:err result)}))

(defn bd-create-bead
  "Create a bead using bd q and return the bead id."
  [env slug title & {:keys [priority] :or {priority 2}}]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "q" title "-p" (str priority))]
    (when (zero? (:exit result))
      (str/trim (:out result)))))

(defn bd-show
  "Run bd show and return output."
  [env slug bead-id]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "show" bead-id)]
    (when (zero? (:exit result))
      (:out result))))

(defn bd-list-json
  "Run bd list --json and return parsed result."
  [env slug]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "list" "--all" "--json")]
    (when (zero? (:exit result))
      (json/parse-string (:out result) true))))

(defn bd-ready-json
  "Run bd ready --json and return parsed result."
  [env slug]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "ready" "--json")]
    (when (zero? (:exit result))
      (json/parse-string (:out result) true))))

(defn bd-update
  "Run bd update on a bead."
  [env slug bead-id & args]
  (let [dir (project-dir env slug)
        cmd (concat ["bd" "update" bead-id] args)
        result (apply proc/shell {:dir dir :out :string :err :string :continue true} cmd)]
    {:exit (:exit result) :out (:out result) :err (:err result)}))

(defn bd-close
  "Close a bead."
  [env slug bead-id]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "close" bead-id)]
    {:exit (:exit result) :out (:out result)}))

(defn bd-dep-add
  "Add a dependency: bead-id depends on depends-on."
  [env slug bead-id depends-on]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "dep" "add" bead-id depends-on)]
    {:exit (:exit result) :out (:out result)}))

(defn bd-dep-list
  "List dependencies of a bead."
  [env slug bead-id]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "bd" "dep" "list" bead-id)]
    {:exit (:exit result) :out (str/trim (:out result))}))

(defn git-commit [env slug message]
  (let [dir (project-dir env slug)]
    (proc/shell {:dir dir :out :string :err :string :continue true} "git" "add" "-A")
    (proc/shell {:dir dir :out :string :err :string :continue true} "git" "commit" "-m" message "--allow-empty")))

(defn git-log-oneline [env slug]
  (let [dir (project-dir env slug)
        result (proc/shell {:dir dir :out :string :err :string :continue true}
                           "git" "log" "--oneline")]
    (when (zero? (:exit result))
      (str/split-lines (str/trim (:out result))))))

(defn cleanup-test-env [env]
  (when (:root env)
    (proc/shell {:continue true} "rm" "-rf" (:root env))))

;; ── Test Suites ──

(describe "Integration: Project Creation"

  (with-all env (setup-test-env))
  (after-all (cleanup-test-env @env))

  (it "creates a new project with correct structure"
    (let [result (create-test-project @env "alpha" :name "Alpha Project" :goal "Test alpha")]
      (should= 0 (:exit result))
      (should (str/includes? (:message result) "Created project: alpha"))
      ;; Verify directory structure
      (should (fs/directory? (project-dir @env "alpha")))
      (should (fs/exists? (str (project-dir @env "alpha") "/.braids/config.edn")))
      (should (fs/exists? (str (project-dir @env "alpha") "/.braids/iterations/001/iteration.edn")))
      (should (fs/exists? (str (project-dir @env "alpha") "/AGENTS.md")))
      (should (fs/directory? (str (project-dir @env "alpha") "/.git")))))

  (it "creates project config with correct values"
    (let [cfg (read-project-config @env "alpha")]
      (should= "Alpha Project" (:name cfg))
      (should= :active (:status cfg))
      (should= :normal (:priority cfg))
      (should= :full (:autonomy cfg))))

  (it "adds project to registry"
    (let [reg (read-registry @env)]
      (should= 1 (count (:projects reg)))
      (should= "alpha" (:slug (first (:projects reg))))
      (should= :active (:status (first (:projects reg))))))

  (it "creates initial iteration in planning status"
    (let [iter (read-iteration-edn @env "alpha" 1)]
      (should= :planning (:status iter))
      (should= [] (:stories iter))))

  (it "creates initial git commit"
    (let [logs (git-log-oneline @env "alpha")]
      (should (>= (count logs) 1))
      (should (some #(str/includes? % "alpha") logs))))

  (it "rejects duplicate project slug"
    (let [result (create-test-project @env "alpha" :name "Duplicate" :goal "Dup")]
      (should= 1 (:exit result))
      (should (str/includes? (:message result) "already exists"))))

  (it "creates a high-priority project"
    (let [result (create-test-project @env "beta" :name "Beta Project" :goal "Test beta" :priority "high")]
      (should= 0 (:exit result))
      (let [reg (read-registry @env)]
        (should= 2 (count (:projects reg)))
        (let [beta (first (filter #(= "beta" (:slug %)) (:projects reg)))]
          (should= :high (:priority beta)))))))


(describe "Integration: Bead CRUD Workflow"

  (with-all env (setup-test-env))
  (after-all (cleanup-test-env @env))
  (before-all (create-test-project @env "crud-test" :name "CRUD Test" :goal "Test CRUD"))

  (it "creates beads with bd q"
    (let [bead-id (bd-create-bead @env "crud-test" "First bead")]
      (should-not-be-nil bead-id)
      (should-not (str/blank? bead-id))))

  (it "shows bead details with bd show"
    (let [bead-id (bd-create-bead @env "crud-test" "Showable bead")
          output (bd-show @env "crud-test" bead-id)]
      (should-not-be-nil output)
      (should (str/includes? output "Showable bead"))))

  (it "lists beads with bd list --json"
    (let [_ (bd-create-bead @env "crud-test" "Listed bead")
          beads (bd-list-json @env "crud-test")]
      (should (sequential? beads))
      (should (>= (count beads) 1))))

  (it "shows ready beads with bd ready --json"
    (let [_ (bd-create-bead @env "crud-test" "Ready bead")
          ready (bd-ready-json @env "crud-test")]
      (should (sequential? ready))
      (should (>= (count ready) 1))))

  (it "closes a bead with bd close"
    (let [bead-id (bd-create-bead @env "crud-test" "Closeable bead")
          result (bd-close @env "crud-test" bead-id)]
      (should= 0 (:exit result))
      ;; Verify it shows as closed
      (let [output (bd-show @env "crud-test" bead-id)]
        (should (str/includes? (str/upper-case output) "CLOSED")))))

  (it "claims a bead with bd update --claim"
    (let [bead-id (bd-create-bead @env "crud-test" "Claimable bead")
          result (bd-update @env "crud-test" bead-id "--claim")]
      (should= 0 (:exit result)))))


(describe "Integration: Dependency Management"

  (with-all env (setup-test-env))
  (after-all (cleanup-test-env @env))
  (before-all (create-test-project @env "dep-test" :name "Dep Test" :goal "Test deps"))

  (it "adds and lists dependencies"
    (let [bead-a (bd-create-bead @env "dep-test" "Dependency A")
          bead-b (bd-create-bead @env "dep-test" "Depends on A")
          add-result (bd-dep-add @env "dep-test" bead-b bead-a)
          list-result (bd-dep-list @env "dep-test" bead-b)]
      (should= 0 (:exit add-result))
      (should (str/includes? (:out list-result) bead-a))))

  (it "dependent bead is not ready until dependency is closed"
    (let [bead-a (bd-create-bead @env "dep-test" "Blocker bead")
          bead-b (bd-create-bead @env "dep-test" "Blocked bead")
          _ (bd-dep-add @env "dep-test" bead-b bead-a)
          ready-before (bd-ready-json @env "dep-test")
          ready-ids-before (set (map :id ready-before))]
      ;; bead-b should NOT be ready (has unresolved dep)
      (should-not (contains? ready-ids-before bead-b))
      ;; bead-a SHOULD be ready (no deps)
      (should (contains? ready-ids-before bead-a))
      ;; Close the dependency
      (bd-close @env "dep-test" bead-a)
      ;; Now bead-b should be ready
      (let [ready-after (bd-ready-json @env "dep-test")
            ready-ids-after (set (map :id ready-after))]
        (should (contains? ready-ids-after bead-b))))))


(describe "Integration: Iteration Lifecycle"

  (with-all env (setup-test-env))
  (after-all (cleanup-test-env @env))
  (before-all (create-test-project @env "iter-test" :name "Iter Test" :goal "Test iterations"))

  (it "starts with iteration 001 in planning status"
    (let [iter (read-iteration-edn @env "iter-test" 1)]
      (should= :planning (:status iter))
      (should= [] (:stories iter))))

  (it "can activate an iteration by updating iteration.edn"
    (let [bead-id (bd-create-bead @env "iter-test" "Iteration story")
          iter-path (str (project-dir @env "iter-test") "/.braids/iterations/001/iteration.edn")]
      (spit iter-path (edn-format {:number 1 :status :active :stories [bead-id] :notes []}))
      (let [iter (read-iteration-edn @env "iter-test" 1)]
        (should= :active (:status iter))
        (should= [bead-id] (:stories iter)))))

  (it "can complete an iteration after closing all beads"
    (let [iter-path (str (project-dir @env "iter-test") "/.braids/iterations/001/iteration.edn")
          iter (read-iteration-edn @env "iter-test" 1)
          bead-id (first (:stories iter))]
      ;; Close the bead
      (bd-close @env "iter-test" bead-id)
      ;; Mark iteration complete
      (spit iter-path (edn-format {:number 1 :status :complete :stories [bead-id] :notes []}))
      (let [completed (read-iteration-edn @env "iter-test" 1)]
        (should= :complete (:status completed)))))

  (it "can create a second iteration"
    (let [iter-dir (str (project-dir @env "iter-test") "/.braids/iterations/002")]
      (fs/create-dirs iter-dir)
      (spit (str iter-dir "/iteration.edn")
            (edn-format {:number 2 :status :active :stories [] :notes []}))
      (let [iter (read-iteration-edn @env "iter-test" 2)]
        (should= :active (:status iter))
        (should= 2 (:number iter))))))


(describe "Integration: braids CLI dispatch"

  (it "help command returns 0"
    (should= 0 (core/run ["help"])))

  (it "unknown command returns 1"
    (should= 1 (core/run ["nonexistent"])))

  (it "--help flag returns 0"
    (should= 0 (core/run ["--help"])))

  (it "no args returns 0 (shows help)"
    (should= 0 (core/run nil))))


(describe "Integration: Pure function contracts"

  (it "ready-beads returns empty for empty registry"
    (let [result (ready/ready-beads {:projects []} {} {} {})]
      (should= [] result)))

  (it "ready-beads respects max-workers"
    (let [reg {:projects [{:slug "p1" :status :active :priority :normal}]}
          configs {"p1" {:status :active :max-workers 1}}
          beads {"p1" [{:id "b1" :title "B1" :priority 2}]}
          ;; Already at max workers
          workers {"p1" 1}
          result (ready/ready-beads reg configs beads workers)]
      (should= [] result)))

  (it "ready-beads returns beads when capacity available"
    (let [reg {:projects [{:slug "p1" :status :active :priority :normal}]}
          configs {"p1" {:status :active :max-workers 2}}
          beads {"p1" [{:id "b1" :title "B1" :priority 2}]}
          workers {"p1" 0}
          result (ready/ready-beads reg configs beads workers)]
      (should= 1 (count result))
      (should= "b1" (:id (first result)))))

  (it "orch/tick returns idle with disable-cron when no projects"
    (let [result (orch/tick {:projects []} {} {} {} {} {})]
      (should= "idle" (:action result))
      (should= true (:disable-cron result))))

  (it "list/format-list handles empty projects"
    (should= "No projects registered." (list/format-list {:projects []})))

  (it "list/format-list formats projects as table"
    (let [output (list/format-list {:projects [{:slug "test" :status :active :priority :normal :path "/tmp/test"}]})]
      (should (str/includes? output "test"))
      (should (str/includes? output "active"))))

  (it "iteration/parse-iteration-edn applies defaults"
    (let [parsed (iter/parse-iteration-edn "{:number 1 :status :active :stories []}")]
      (should= 1 (:number parsed))
      (should= :active (:status parsed))
      (should= [] (:stories parsed))))

  (it "iteration/completion-stats calculates correctly"
    (let [stats (iter/completion-stats [{:status "closed"} {:status "open"} {:status "closed"}])]
      (should= 3 (:total stats))
      (should= 2 (:closed stats))
      (should= 66 (:percent stats))))

  (it "new/validate-slug accepts valid slugs"
    (should= [] (new/validate-slug "my-project"))
    (should= [] (new/validate-slug "abc123"))
    (should= [] (new/validate-slug "a")))

  (it "new/validate-slug rejects invalid slugs"
    (should (seq (new/validate-slug nil)))
    (should (seq (new/validate-slug "")))
    (should (seq (new/validate-slug "UPPERCASE")))
    (should (seq (new/validate-slug "-leading-hyphen")))
    (should (seq (new/validate-slug "trailing-hyphen-"))))

  (it "config/config-get returns value for known key"
    (let [result (config/config-get {:braids-home "/tmp"} "braids-home")]
      (should= "/tmp" (:ok result))))

  (it "config/config-get returns error for unknown key"
    (let [result (config/config-get {:braids-home "/tmp"} "nonexistent")]
      (should-not-be-nil (:error result))))

  (it "config/config-set updates config"
    (let [updated (config/config-set {:braids-home "/tmp"} "braids-home" "/new")]
      (should= "/new" (:braids-home updated)))))


(describe "Integration: End-to-End Worker Workflow"

  (with-all env (setup-test-env))
  (after-all (cleanup-test-env @env))
  (before-all (create-test-project @env "e2e" :name "E2E Test" :goal "End-to-end workflow"))

  (it "simulates a full worker cycle: create → claim → work → close → commit"
    (let [;; 1. Create a bead
          bead-id (bd-create-bead @env "e2e" "Implement feature X")
          _ (should-not-be-nil bead-id)

          ;; 2. Set up the iteration with the bead
          iter-path (str (project-dir @env "e2e") "/.braids/iterations/001/iteration.edn")
          _ (spit iter-path (edn-format {:number 1 :status :active :stories [bead-id] :notes []}))

          ;; 3. Verify bead is ready
          ready (bd-ready-json @env "e2e")
          _ (should (some #(= bead-id (:id %)) ready))

          ;; 4. Claim the bead
          claim-result (bd-update @env "e2e" bead-id "--claim")
          _ (should= 0 (:exit claim-result))

          ;; 5. Do work (write a deliverable)
          suffix (last (str/split bead-id #"-"))
          deliverable-path (str (project-dir @env "e2e") "/.braids/iterations/001/" suffix "-feature-x.md")
          _ (spit deliverable-path "# Feature X\n\nImplemented feature X successfully.\n")

          ;; 6. Close the bead
          close-result (bd-close @env "e2e" bead-id)
          _ (should= 0 (:exit close-result))

          ;; 7. Git commit
          _ (git-commit @env "e2e" (str "Implement feature X (" bead-id ")"))

          ;; 8. Verify git log contains the bead id
          logs (git-log-oneline @env "e2e")]
      (should (some #(str/includes? % bead-id) logs))

      ;; 9. Verify deliverable exists
      (should (fs/exists? deliverable-path))

      ;; 10. Verify bead is closed
      (let [show-output (bd-show @env "e2e" bead-id)]
        (should (str/includes? (str/upper-case show-output) "CLOSED"))))))


(describe "Integration: Multi-Project Registry"

  (with-all env (setup-test-env))
  (after-all (cleanup-test-env @env))

  (it "manages multiple projects in registry"
    (create-test-project @env "proj-a" :name "Project A" :goal "Goal A" :priority "high")
    (create-test-project @env "proj-b" :name "Project B" :goal "Goal B" :priority "normal")
    (create-test-project @env "proj-c" :name "Project C" :goal "Goal C" :priority "low")

    (let [reg (read-registry @env)]
      (should= 3 (count (:projects reg)))
      (should= #{"proj-a" "proj-b" "proj-c"} (set (map :slug (:projects reg))))))

  (it "preserves priority ordering in registry"
    (let [reg (read-registry @env)
          projects (:projects reg)
          priorities (into {} (map (fn [p] [(:slug p) (:priority p)]) projects))]
      (should= :high (get priorities "proj-a"))
      (should= :normal (get priorities "proj-b"))
      (should= :low (get priorities "proj-c")))))
