(ns braids.migrate-command-spec
  (:require [speclj.core :refer :all]
            [braids.migration :as mig]
            [braids.registry :as registry]
            [braids.project-config :as pc]
            [clojure.edn :as edn]))

(describe "braids.migration/migrate-registry"

  (it "converts parsed registry md to EDN string that round-trips"
    (let [md (str "# Projects\n\n"
                  "| Slug | Status | Priority | Path |\n"
                  "|------|--------|----------|------|\n"
                  "| my-proj | active | normal | ~/Projects/my-proj |\n"
                  "| other | paused | high | ~/Projects/other |\n")
          parsed (mig/parse-registry-md md)
          edn-str (registry/registry->edn-string parsed)
          round-tripped (registry/parse-registry edn-str)]
      (should= 2 (count (:projects round-tripped)))
      (should= "my-proj" (:slug (first (:projects round-tripped))))
      (should= :active (:status (first (:projects round-tripped))))
      (should= :high (:priority (second (:projects round-tripped)))))))

(describe "braids.migration/migrate-project-config"

  (it "converts parsed project md to EDN string that round-trips"
    (let [md (str "# My Project\n\n"
                  "- **Status:** active\n"
                  "- **Priority:** high\n"
                  "- **Autonomy:** full\n"
                  "- **Checkin:** daily\n"
                  "- **Channel:** 123456\n"
                  "- **MaxWorkers:** 3\n")
          parsed (mig/parse-project-md md)
          edn-str (pc/project-config->edn-string parsed)
          round-tripped (pc/parse-project-config edn-str)]
      (should= "My Project" (:name round-tripped))
      (should= :active (:status round-tripped))
      (should= :high (:priority round-tripped))
      (should= :full (:autonomy round-tripped))
      (should= :daily (:checkin round-tripped))
      (should= "123456" (:channel round-tripped))
      (should= 3 (:max-workers round-tripped)))))

(describe "braids.migration/migrate-install"

  (it "returns actions for registry migration when registry.md exists"
    (let [state-home "/tmp/test-state"
          registry-md (str "# Projects\n\n"
                           "| Slug | Status | Priority | Path |\n"
                           "|------|--------|----------|------|\n"
                           "| foo | active | normal | ~/Projects/foo |\n")
          fs-state {"/tmp/test-state/registry.md" registry-md
                    "/tmp/test-state/registry.edn" nil}
          actions (mig/plan-migration {:state-home state-home
                                       :read-file (fn [p] (get fs-state p))
                                       :file-exists? (fn [p] (some? (get fs-state p)))})]
      (should-contain {:type :write-registry-edn :path "/tmp/test-state/registry.edn"} 
                      (map #(select-keys % [:type :path]) actions))))

  (it "skips registry migration when registry.edn already exists"
    (let [state-home "/tmp/test-state"
          fs-state {"/tmp/test-state/registry.edn" "{:projects []}"
                    "/tmp/test-state/registry.md" "old stuff"}
          actions (mig/plan-migration {:state-home state-home
                                       :read-file (fn [p] (get fs-state p))
                                       :file-exists? (fn [p] (some? (get fs-state p)))})]
      (should-not-contain :write-registry-edn (map :type actions))))

  (it "plans config.edn migration from PROJECT.md"
    (let [state-home "/tmp/test-state"
          registry-md (str "# Projects\n\n"
                           "| Slug | Status | Priority | Path |\n"
                           "|------|--------|----------|------|\n"
                           "| foo | active | normal | /projects/foo |\n")
          project-md (str "# Foo\n\n"
                          "- **Status:** active\n"
                          "- **Priority:** normal\n"
                          "- **Autonomy:** full\n")
          fs-state {"/tmp/test-state/registry.md" registry-md
                    "/tmp/test-state/registry.edn" nil
                    "/projects/foo/.braids/PROJECT.md" project-md
                    "/projects/foo/.braids/config.edn" nil
                    "/projects/foo/.braids/project.edn" nil}
          actions (mig/plan-migration {:state-home state-home
                                       :read-file (fn [p] (get fs-state p))
                                       :file-exists? (fn [p] (some? (get fs-state p)))})]
      (should-contain {:type :write-config-edn :path "/projects/foo/.braids/config.edn"}
                      (map #(select-keys % [:type :path]) actions))))

  (it "plans config.edn migration from legacy project.edn"
    (let [state-home "/tmp/test-state"
          fs-state {"/tmp/test-state/registry.edn" (pr-str {:projects [{:slug "foo" :status :active :priority :normal :path "/projects/foo"}]})
                    "/projects/foo/.braids/config.edn" nil
                    "/projects/foo/.braids/project.edn" (pr-str {:name "Foo" :status :active :priority :normal :autonomy :full})}
          actions (mig/plan-migration {:state-home state-home
                                       :read-file (fn [p] (get fs-state p))
                                       :file-exists? (fn [p] (some? (get fs-state p)))})]
      (should-contain {:type :write-config-edn :path "/projects/foo/.braids/config.edn"}
                      (map #(select-keys % [:type :path]) actions))))

  (it "skips config.edn when it already exists"
    (let [state-home "/tmp/test-state"
          fs-state {"/tmp/test-state/registry.edn" (pr-str {:projects [{:slug "foo" :status :active :priority :normal :path "/projects/foo"}]})
                    "/projects/foo/.braids/config.edn" (pr-str {:name "Foo"})}
          actions (mig/plan-migration {:state-home state-home
                                       :read-file (fn [p] (get fs-state p))
                                       :file-exists? (fn [p] (some? (get fs-state p)))})]
      (should-not-contain :write-config-edn (map :type actions))))

  (it "formats migration report"
    (let [actions [{:type :write-registry-edn :path "/state/registry.edn"}
                   {:type :write-config-edn :path "/proj/foo/.braids/config.edn" :slug "foo"}]]
      (should-contain "registry.edn" (mig/format-migration-report actions))
      (should-contain "foo" (mig/format-migration-report actions))))

  (it "reports nothing to do when no actions"
    (should-contain "Nothing to migrate" (mig/format-migration-report []))))
