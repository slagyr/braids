(ns braids.integration-smoke-spec
  "Integration smoke tests using temp project directories.
   No external tools (bd, git, openclaw) — pure filesystem and data checks."
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [braids.orch :as orch]))

;; ── Helper: create a realistic temp project ──

(defn- setup-smoke-project!
  "Creates a temp project with realistic structure. Returns [tmp-dir project-dir]."
  []
  (let [tmp (str (fs/create-temp-dir {:prefix "smoke-test"}))
        proj (str tmp "/test-smoke")]
    (fs/create-dirs (str proj "/.braids/iterations/001"))
    (fs/create-dirs (str proj "/.braids/iterations/002"))
    (spit (str proj "/.braids/config.edn")
      (pr-str {:name "Smoke Project" :status :active :priority :high}))
    (spit (str proj "/AGENTS.md") "# Smoke Project\n\n## Goal\nTest.\n")
    (spit (str proj "/.braids/iterations/001/iteration.edn")
      (pr-str {:number 1 :status :active
               :stories [{:id "smoke-aaa" :title "First"}
                         {:id "smoke-bbb" :title "Second"}]}))
    (spit (str proj "/.braids/iterations/002/iteration.edn")
      (pr-str {:number 2 :status :planning :stories [] :notes []}))
    ;; Deliverable for smoke-aaa
    (spit (str proj "/.braids/iterations/001/aaa-first.md")
      "# First\n\n## Summary\nDone.\n")
    [tmp proj]))

;; ── Integration smoke tests (pure, no external tools) ──

(describe "Integration smoke tests"
  (with-all env (setup-smoke-project!))

  (it "project config is valid EDN for test project"
    (let [[_ proj] @env
          cfg (clojure.edn/read-string (slurp (str proj "/.braids/config.edn")))]
      (should (map? cfg))
      (should= :active (:status cfg))))

  (it "iterations are valid EDN for test project"
    (let [[_ proj] @env
          iter-files (fs/glob proj ".braids/iterations/*/iteration.edn")]
      (should (>= (count iter-files) 1))
      (doseq [f iter-files]
        (let [parsed (clojure.edn/read-string (slurp (str f)))]
          (should (map? parsed))
          (should (contains? parsed :status))))))

  (it "at most one active iteration per project"
    (let [[_ proj] @env
          active-count (->> (fs/glob proj ".braids/iterations/*/iteration.edn")
                            (map #(clojure.edn/read-string (slurp (str %))))
                            (filter #(= :active (:status %)))
                            count)]
      (should (<= active-count 1))))

  (it "no orphaned deliverables (each .md matches a story id)"
    (let [[_ proj] @env
          iter-edn (clojure.edn/read-string
                     (slurp (str proj "/.braids/iterations/001/iteration.edn")))
          story-ids (set (map :id (:stories iter-edn)))
          md-files (->> (fs/glob proj ".braids/iterations/001/*.md")
                        (map #(fs/file-name %))
                        (filter #(not= "iteration.edn" %)))]
      ;; Each deliverable .md should correspond to a story
      (doseq [md md-files]
        ;; Extract the short-id prefix (e.g., "aaa" from "aaa-first.md")
        (let [short-id (first (str/split md #"-"))]
          (should (some #(str/ends-with? % short-id) story-ids)))))))

;; ── Cross-project checks (pure orch/tick) ──

(describe "Cross-Project Checks"
  (it "orch-tick idle result includes disable-cron when no active iterations"
    (let [registry {:projects [{:slug "empty-proj" :status :active :priority :high :path "/tmp/x"}]}
          configs {"empty-proj" {:status :active}}
          iterations {}  ;; no active iterations
          beads {}
          workers {}
          notifications {}
          result (orch/tick registry configs iterations beads workers notifications)]
      (should= "idle" (:action result))
      (should= "no-active-iterations" (:reason result))
      (should= true (:disable-cron result))))

  (it "orch-tick idle result includes disable-cron=false when at capacity"
    (let [registry {:projects [{:slug "busy-proj" :status :active :priority :high :path "/tmp/x"}]}
          configs {"busy-proj" {:status :active :max-workers 1}}
          iterations {"busy-proj" "001"}
          beads {"busy-proj" [{:id "b1" :status "open"}]}
          workers {"busy-proj" 1}  ;; at capacity
          notifications {}
          result (orch/tick registry configs iterations beads workers notifications)]
      (should= "idle" (:action result))
      (should= "all-at-capacity" (:reason result))
      (should= false (:disable-cron result)))))
