(ns integration-smoke-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]
            [cheshire.core :as json]
            [braids.orch :as orch]))

(def home (System/getProperty "user.home"))
(def braids-home (or (System/getenv "BRAIDS_HOME") (str home "/Projects")))
(def state-home (str home "/.openclaw/braids"))
(def registry (str state-home "/registry.edn"))

(defn slurp-safe [path] (when (fs/exists? path) (slurp path)))

(defn extract-bead-ids-edn [iter-data]
  (mapv :id (:stories iter-data)))

(defn get-bead-status [project-dir bead-id]
  (try
    (let [r (p/shell {:dir project-dir :out :string :err :string :continue true} "bd" "show" bead-id)]
      (if (zero? (:exit r))
        (or (some-> (re-find #"\b(OPEN|CLOSED|BLOCKED|IN_PROGRESS)\b" (:out r)) first) "UNKNOWN")
        "UNKNOWN"))
    (catch Exception _ "UNKNOWN")))

(defn bead-suffix [bead-id] (last (str/split bead-id #"-")))

(defn find-deliverable [iter-dir bead-id]
  (let [suffix (bead-suffix bead-id)
        files (fs/glob iter-dir "*.md")
        names (map #(str (fs/file-name %)) files)]
    (some (fn [n]
            (or (str/starts-with? n (str suffix "-"))
                (= n (str bead-id ".md"))))
          names)))

(defn git-has-commit? [project-dir bead-id]
  (try
    (let [r (p/shell {:dir project-dir :out :string :err :string :continue true}
                     "git" "log" "--oneline" "--all" (str "--grep=" bead-id))]
      (and (zero? (:exit r)) (not (str/blank? (:out r)))))
    (catch Exception _ false)))

(defn resolve-iterations-dir [resolved]
  (cond
    (fs/directory? (str resolved "/.braids/iterations")) (str resolved "/.braids/iterations")
    (fs/directory? (str resolved "/.project/iterations")) (str resolved "/.project/iterations")
    :else (str resolved "/iterations")))

(defn resolve-config-edn [resolved]
  (cond
    (fs/exists? (str resolved "/.braids/config.edn")) (str resolved "/.braids/config.edn")
    :else nil))

(defn load-iteration-edn [path]
  (when (fs/exists? path)
    (try (clojure.edn/read-string (slurp path)) (catch Exception _ nil))))

;; ── Integration tests per project ──

(when (fs/exists? registry)
  (let [reg (clojure.edn/read-string (slurp registry))]

    (doseq [{:keys [slug status _priority path]} (:projects reg)]
      (let [resolved (str/replace (or path "") "~" home)]
        (when (and slug
                   (fs/directory? resolved)
                   (fs/exists? (resolve-config-edn resolved)))

          (describe (str "Integration: " slug)

            ;; Git state
            (it "git state is clean"
              (when (fs/directory? (str resolved "/.git"))
                (let [unpushed (try
                                 (let [r (p/shell {:dir resolved :out :string :err :string :continue true}
                                                  "git" "log" "--oneline" "@{u}..HEAD")]
                                   (if (zero? (:exit r))
                                     (count (remove str/blank? (str/split-lines (:out r))))
                                     0))
                                 (catch Exception _ 0))]
                  (should (<= unpushed 5)))
                (let [branch (try
                               (str/trim (:out (p/shell {:dir resolved :out :string :err :string :continue true}
                                                        "git" "branch" "--show-current")))
                               (catch Exception _ ""))]
                  (should-not (str/blank? branch)))))

            ;; Iteration checks
            (it "iterations are valid"
              (let [iterations-dir (resolve-iterations-dir resolved)]
                (when (fs/directory? iterations-dir)
                  (doseq [iter-dir (sort (fs/list-dir iterations-dir))]
                    (let [iter-name (str (fs/file-name iter-dir))
                          iter-edn-path (str iter-dir "/iteration.edn")]
                      (when (and (fs/directory? iter-dir) (re-matches #"\d{3}" iter-name) (fs/exists? iter-edn-path))
                        (let [iter-data (load-iteration-edn iter-edn-path)
                              iter-status (when iter-data (name (:status iter-data)))]

                          ;; Completed iteration checks
                          (when (= iter-status "complete")
                            (let [bead-ids (extract-bead-ids-edn iter-data)]
                              (doseq [bid bead-ids]
                                (should (find-deliverable iter-dir bid))
                                (let [bd-status (get-bead-status resolved bid)]
                                  (when (not= bd-status "UNKNOWN")
                                    (should= "CLOSED" bd-status)))
                                (should (git-has-commit? resolved bid)))))

                          ;; Active iteration checks
                          (when (= iter-status "active")
                            (let [bead-ids (extract-bead-ids-edn iter-data)
                                  statuses (map (fn [bid] [bid (get-bead-status resolved bid)]) bead-ids)
                                  closed-count (count (filter #(= "CLOSED" (second %)) statuses))
                                  total-count (count bead-ids)]
                              (doseq [[bid bd-status] statuses]
                                (when (= bd-status "CLOSED")
                                  (should (find-deliverable iter-dir bid))
                                  (should (git-has-commit? resolved bid)))
                                (should-not= "UNKNOWN" bd-status))
                              ;; All beads closed but iteration still active = bad
                              (when (and (pos? total-count) (= closed-count total-count))
                                (should false)))))))))))

            ;; At most one active iteration
            (it "at most one active iteration"
              (let [idir (resolve-iterations-dir resolved)
                    active-count (if (fs/directory? idir)
                                   (->> (fs/glob idir "*/iteration.edn")
                                        (map #(load-iteration-edn (str %)))
                                        (filter #(= :active (:status %)))
                                        count)
                                   0)]
                (should (<= active-count 1))))

            ;; Orphaned deliverables
            (it "no orphaned deliverables"
              (let [idir (resolve-iterations-dir resolved)]
                (when (fs/directory? idir)
                  (doseq [iter-dir (sort (fs/list-dir idir))]
                    (let [iter-name (str (fs/file-name iter-dir))
                          iter-edn-path (str iter-dir "/iteration.edn")]
                      (when (and (fs/directory? iter-dir) (re-matches #"\d{3}" iter-name) (fs/exists? iter-edn-path))
                        (let [iter-data (load-iteration-edn iter-edn-path)
                              bead-ids (extract-bead-ids-edn iter-data)
                              suffixes (set (map bead-suffix bead-ids))
                              full-ids (set bead-ids)]
                          (doseq [f (fs/glob iter-dir "*.md")]
                            (let [fname (str (fs/file-name f))
                                  prefix (first (str/split fname #"-"))
                                  no-ext (str/replace fname #"\.md$" "")]
                              (should (or (contains? suffixes prefix)
                                          (contains? full-ids no-ext)))))))))))))))))

  ;; ── Cross-project checks (skipped if no registry) ──

  (describe "Cross-Project Checks"
    (it "STATUS.md exists and is fresh (skipped if no registry)"
      (when (fs/exists? registry)
        (let [status-file (str state-home "/STATUS.md")]
          (should (fs/exists? status-file))
          (let [mtime (.toMillis (fs/last-modified-time status-file))
                now (System/currentTimeMillis)
                age-s (/ (- now mtime) 1000)]
            (should (< age-s 86400))))))

    (it "orchestrator self-disable: orch-tick idle results include disable-cron (skipped if no registry)"
      (when (fs/exists? registry)
        ;; Self-disable replaced .orchestrator-state.json — verify the tick contract
        (let [result (orch/tick {:projects []} {} {} {} {} {})]
          (should= true (:disable-cron result)))))))
