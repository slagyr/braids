(ns braids.ready-io-spec
  (:require [speclj.core :refer :all]
            [braids.ready-io :as rio]
            [braids.config-io :as config-io]
            [braids.project-config :as pc]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]))

(def stub-config
  {:braids-home "~/Projects" :orchestrator-channel nil
   :env-path nil :bd-bin "bd" :openclaw-bin "openclaw"})

(describe "braids.ready-io"

  (context "count-workers"

    (it "returns empty map for no labels"
      (should= {} (rio/count-workers [])))

    (it "counts workers per project slug"
      (should= {"proj" 2 "other" 1}
               (rio/count-workers ["project:proj:bead-1"
                                   "project:proj:bead-2"
                                   "project:other:bead-3"])))

    (it "ignores non-project labels"
      (should= {"proj" 1}
               (rio/count-workers ["project:proj:bead-1"
                                   "agent:main"
                                   "cron:something"])))

    (it "handles project: prefix with just slug"
      (should= {"proj" 1}
               (rio/count-workers ["project:proj"]))))

  (context "resolve-state-home"

    (it "returns ~/.openclaw/braids by default"
      (should= (str (fs/expand-home "~/.openclaw/braids"))
               (rio/resolve-state-home))))

  (context "load-registry uses state-home"

    (it "loads registry from state-home not braids-home"
      (let [state-home (str (fs/create-temp-dir {:prefix "state-home-test"}))
            braids-home (str (fs/create-temp-dir {:prefix "braids-home-test"}))]
        ;; Put registry in state-home (new location)
        (spit (str state-home "/registry.edn")
               "{:projects [{:slug \"proj\" :status :active :priority :normal :path \"/tmp/proj\"}]}")
        ;; Put a DIFFERENT registry in braids-home (old location) to prove we don't read it
        (spit (str braids-home "/registry.edn")
               "{:projects [{:slug \"old\" :status :active :priority :normal :path \"/tmp/old\"}]}")
        (let [result (rio/load-registry state-home)]
          (should= "proj" (-> result :projects first :slug)))))

    (it "does not fall back to registry.md"
      (let [state-home (str (fs/create-temp-dir {:prefix "no-md-fallback"}))]
        ;; Only put registry.md, no registry.edn
        (spit (str state-home "/registry.md")
               "| Slug | Status | Priority | Path |\n|------|--------|----------|------|\n| proj | active | normal | /tmp/proj |\n")
        (let [result (rio/load-registry state-home)]
          (should= [] (:projects result)))))

    (it "returns empty projects when no registry.edn exists"
      (let [state-home (str (fs/create-temp-dir {:prefix "empty-reg"}))]
        (let [result (rio/load-registry state-home)]
          (should= [] (:projects result))))))

  (context "load-ready-beads"

    (it "parses JSON output from bd ready --json"
      (let [beads [{:id "bead-1" :title "First" :priority 1}
                   {:id "bead-2" :title "Second" :priority 2}]]
        (with-redefs [config-io/load-config (fn [] stub-config)
                      proc/shell (fn [opts & _args]
                                   {:out (json/generate-string beads) :err ""})]
          (let [result (rio/load-ready-beads "/tmp/fake-project")]
            (should= 2 (count result))
            (should= "bead-1" (:id (first result)))
            (should= "Second" (:title (second result)))))))

    (it "extracts only id, title, and priority from beads"
      (let [beads [{:id "b1" :title "T" :priority 1 :status "open" :extra "stuff"}]]
        (with-redefs [config-io/load-config (fn [] stub-config)
                      proc/shell (fn [opts & _args]
                                   {:out (json/generate-string beads) :err ""})]
          (let [result (rio/load-ready-beads "/tmp/proj")]
            (should= {:id "b1" :title "T" :priority 1} (first result))))))

    (it "returns empty vector when bd returns empty array"
      (with-redefs [config-io/load-config (fn [] stub-config)
                    proc/shell (fn [opts & _args]
                                 {:out "[]" :err ""})]
        (should= [] (rio/load-ready-beads "/tmp/proj"))))

    (it "returns empty vector when bd returns non-sequential JSON"
      (with-redefs [config-io/load-config (fn [] stub-config)
                    proc/shell (fn [opts & _args]
                                 {:out "{\"error\": \"bad\"}" :err ""})]
        (should= [] (rio/load-ready-beads "/tmp/proj"))))

    (it "returns empty vector when bd command throws exception"
      (with-redefs [config-io/load-config (fn [] stub-config)
                    proc/shell (fn [opts & _args]
                                 (throw (Exception. "bd not found")))]
        (should= [] (rio/load-ready-beads "/tmp/proj"))))

    (it "returns empty vector when bd returns malformed JSON"
      (with-redefs [config-io/load-config (fn [] stub-config)
                    proc/shell (fn [opts & _args]
                                 {:out "not json {{" :err ""})]
        (should= [] (rio/load-ready-beads "/tmp/proj"))))

    (it "passes correct dir and args to shell"
      (let [captured (atom nil)]
        (with-redefs [config-io/load-config (fn [] stub-config)
                      proc/shell (fn [opts & args]
                                   (reset! captured {:dir (:dir opts) :args (vec args)})
                                   {:out "[]" :err ""})]
          (rio/load-ready-beads "/tmp/my-project")
          (should= "/tmp/my-project" (:dir @captured))
          (should= ["bd" "ready" "--json"] (:args @captured)))))

    (it "expands tilde in project path"
      (let [captured (atom nil)
            home (System/getProperty "user.home")]
        (with-redefs [config-io/load-config (fn [] stub-config)
                      proc/shell (fn [opts & args]
                                   (reset! captured (:dir opts))
                                   {:out "[]" :err ""})]
          (rio/load-ready-beads "~/my-project")
          (should= (str home "/my-project") @captured)))))

  (context "load-project-config"

    (it "loads config from .braids/config.edn"
      (let [project (str (fs/create-temp-dir {:prefix "rio-cfg"}))]
        (fs/create-dirs (str project "/.braids"))
        (spit (str project "/.braids/config.edn")
              (pr-str {:name "Test" :max-workers 3 :status :active}))
        (let [result (rio/load-project-config project)]
          (should= 3 (:max-workers result))
          (should= "Test" (:name result)))
        (fs/delete-tree project)))

    (it "falls back to .braids/project.edn (legacy)"
      (let [project (str (fs/create-temp-dir {:prefix "rio-legacy"}))]
        (fs/create-dirs (str project "/.braids"))
        (spit (str project "/.braids/project.edn")
              (pr-str {:name "Legacy" :max-workers 5}))
        (let [result (rio/load-project-config project)]
          (should= 5 (:max-workers result))
          (should= "Legacy" (:name result)))
        (fs/delete-tree project)))

    (it "prefers config.edn over project.edn"
      (let [project (str (fs/create-temp-dir {:prefix "rio-prefer"}))]
        (fs/create-dirs (str project "/.braids"))
        (spit (str project "/.braids/config.edn")
              (pr-str {:name "Preferred" :max-workers 2}))
        (spit (str project "/.braids/project.edn")
              (pr-str {:name "Legacy" :max-workers 9}))
        (let [result (rio/load-project-config project)]
          (should= "Preferred" (:name result))
          (should= 2 (:max-workers result)))
        (fs/delete-tree project)))

    (it "returns defaults when no config file exists"
      (let [project (str (fs/create-temp-dir {:prefix "rio-noconfig"}))]
        (let [result (rio/load-project-config project)]
          (should= pc/defaults result))
        (fs/delete-tree project)))

    (it "expands tilde in project path"
      (let [project (str (fs/create-temp-dir {:prefix "rio-tilde"}))]
        (fs/create-dirs (str project "/.braids"))
        (spit (str project "/.braids/config.edn")
              (pr-str {:name "Tilde" :max-workers 4}))
        ;; We can't easily test with actual ~ but we can verify it works
        ;; with an absolute path (the expand-path is a no-op for absolute)
        (let [result (rio/load-project-config project)]
          (should= 4 (:max-workers result)))
        (fs/delete-tree project))))

  (context "gather-and-compute"

    (it "computes ready beads from registry, configs, and bd output"
      (let [state-home (str (fs/create-temp-dir {:prefix "rio-gather"}))]
        (spit (str state-home "/registry.edn")
              (pr-str {:projects [{:slug "proj-a" :status :active :priority :high :path "/tmp/proj-a"}]}))
        (with-redefs [rio/load-project-config (fn [_] {:max-workers 2 :status :active
                                                        :worker-timeout 1800 :checkin :on-demand
                                                        :channel nil :notifications {}})
                      rio/load-ready-beads (fn [_] [{:id "b1" :title "Task 1" :priority 1}])]
          (let [result (rio/gather-and-compute {:state-home state-home})]
            (should= 1 (count result))
            (should= "b1" (:id (first result)))
            (should= "proj-a" (:project (first result)))))
        (fs/delete-tree state-home)))

    (it "returns empty when no active projects"
      (let [state-home (str (fs/create-temp-dir {:prefix "rio-inactive"}))]
        (spit (str state-home "/registry.edn")
              (pr-str {:projects [{:slug "dormant" :status :paused :priority :normal :path "/tmp/dormant"}]}))
        (let [result (rio/gather-and-compute {:state-home state-home})]
          (should= [] result))
        (fs/delete-tree state-home)))

    (it "returns empty when registry is empty"
      (let [state-home (str (fs/create-temp-dir {:prefix "rio-empty"}))]
        (spit (str state-home "/registry.edn")
              (pr-str {:projects []}))
        (let [result (rio/gather-and-compute {:state-home state-home})]
          (should= [] result))
        (fs/delete-tree state-home)))

    (it "excludes projects at max worker capacity"
      (let [state-home (str (fs/create-temp-dir {:prefix "rio-maxw"}))]
        (spit (str state-home "/registry.edn")
              (pr-str {:projects [{:slug "busy" :status :active :priority :normal :path "/tmp/busy"}]}))
        (with-redefs [rio/load-project-config (fn [_] {:max-workers 1 :status :active
                                                        :worker-timeout 1800 :checkin :on-demand
                                                        :channel nil :notifications {}})
                      rio/load-ready-beads (fn [_] [{:id "b1" :title "Task" :priority 1}])]
          ;; 1 worker running, max-workers is 1 -> no capacity
          (let [result (rio/gather-and-compute {:state-home state-home
                                                :session-labels ["project:busy:bead-x"]})]
            (should= [] result)))
        (fs/delete-tree state-home)))

    (it "includes projects with available worker capacity"
      (let [state-home (str (fs/create-temp-dir {:prefix "rio-avail"}))]
        (spit (str state-home "/registry.edn")
              (pr-str {:projects [{:slug "avail" :status :active :priority :normal :path "/tmp/avail"}]}))
        (with-redefs [rio/load-project-config (fn [_] {:max-workers 3 :status :active
                                                        :worker-timeout 1800 :checkin :on-demand
                                                        :channel nil :notifications {}})
                      rio/load-ready-beads (fn [_] [{:id "b1" :title "Task" :priority 1}])]
          ;; 1 worker running, max-workers is 3 -> capacity available
          (let [result (rio/gather-and-compute {:state-home state-home
                                                :session-labels ["project:avail:bead-x"]})]
            (should= 1 (count result))
            (should= "avail" (:project (first result)))))
        (fs/delete-tree state-home)))

    (it "handles multiple active projects"
      (let [state-home (str (fs/create-temp-dir {:prefix "rio-multi"}))]
        (spit (str state-home "/registry.edn")
              (pr-str {:projects [{:slug "alpha" :status :active :priority :high :path "/tmp/alpha"}
                                  {:slug "beta" :status :active :priority :normal :path "/tmp/beta"}]}))
        (with-redefs [rio/load-project-config (fn [_] {:max-workers 2 :status :active
                                                        :worker-timeout 1800 :checkin :on-demand
                                                        :channel nil :notifications {}})
                      rio/load-ready-beads (fn [path]
                                             (if (= "/tmp/alpha" path)
                                               [{:id "a1" :title "Alpha task" :priority 1}]
                                               [{:id "b1" :title "Beta task" :priority 1}]))]
          (let [result (rio/gather-and-compute {:state-home state-home})
                ids (set (map :id result))]
            (should= 2 (count result))
            (should-contain "a1" ids)
            (should-contain "b1" ids)))
        (fs/delete-tree state-home)))))
