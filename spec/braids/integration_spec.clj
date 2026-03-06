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

(defn cleanup-test-env [env]
  (when (:root env)
    (proc/shell {:continue true} "rm" "-rf" (:root env))))

;; ── Integration tests that shell out to `bd` ──
;; NOTE: These tests are PENDING — they shell out to `bd` (beads CLI) which
;; can hang or be slow, blocking `bb test` from completing quickly.
;; Move to a separate `bb test:integration` task when ready.
;; See: braids-kog

(describe "Integration: Project Creation"
  (xit "creates a new project with correct structure")
  (xit "creates project config with correct values")
  (xit "adds project to registry")
  (xit "creates initial iteration in planning status")
  (xit "creates initial git commit")
  (xit "rejects duplicate project slug")
  (xit "creates a high-priority project"))


(describe "Integration: Bead CRUD Workflow"
  (xit "creates beads with bd q")
  (xit "shows bead details with bd show")
  (xit "lists beads with bd list --json")
  (xit "shows ready beads with bd ready --json")
  (xit "closes a bead with bd close")
  (xit "claims a bead with bd update --claim"))


(describe "Integration: Dependency Management"
  (xit "adds and lists dependencies")
  (xit "dependent bead is not ready until dependency is closed"))


(describe "Integration: Iteration Lifecycle"
  (xit "starts with iteration 001 in planning status")
  (xit "can activate an iteration by updating iteration.edn")
  (xit "can complete an iteration after closing all beads")
  (xit "can create a second iteration"))


;; ── Fast tests that don't shell out ──

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
  (xit "simulates a full worker cycle: create -> claim -> work -> close -> commit"))


(describe "Integration: Multi-Project Registry"
  (xit "manages multiple projects in registry")
  (xit "preserves priority ordering in registry"))
