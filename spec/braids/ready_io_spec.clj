(ns braids.ready-io-spec
  (:require [speclj.core :refer :all]
            [braids.ready-io :as rio]
            [babashka.fs :as fs]))

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
          (should= [] (:projects result)))))))
