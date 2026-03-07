(ns braids.status-io-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]
            [braids.status-io :as sio]
            [braids.iteration-io :as iio]
            [braids.orch-io :as oio]
            [braids.ready-io :as rio]))

(defn make-iteration-dir!
  "Create a project with an iteration directory and iteration.edn."
  [project-path iter-num edn-data]
  (let [dir (str project-path "/.braids/iterations/" iter-num)]
    (fs/create-dirs dir)
    (spit (str dir "/iteration.edn") (pr-str edn-data))
    dir))

(describe "braids.status-io"

  (context "load-iteration-data"

    (it "returns nil when no active iteration exists"
      (with-redefs [oio/find-active-iteration (fn [_] nil)]
        (should-be-nil (sio/load-iteration-data "/tmp/fake-project"))))

    (it "loads iteration data with annotated stories and stats"
      (let [project (str (fs/create-temp-dir {:prefix "status-io-iter"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "bead-1" :title "First"}
                                        {:id "bead-2" :title "Second"}]})
        (let [beads [{"id" "bead-1" "title" "First" "status" "closed" "priority" "1" "dependencies" []}
                     {"id" "bead-2" "title" "Second" "status" "open" "priority" "2" "dependencies" []}]]
          (with-redefs [oio/find-active-iteration (fn [_] "001")
                        iio/load-all-beads (fn [_] beads)]
            (let [result (sio/load-iteration-data project)]
              (should= 1 (:number result))
              (should= "active" (:status result))
              (should= 2 (count (:stories result)))
              (should= 1 (:closed (:stats result)))
              (should= 2 (:total (:stats result)))
              (should= 50 (:percent (:stats result))))))
        (fs/delete-tree project)))

    (it "annotates stories with bead status"
      (let [project (str (fs/create-temp-dir {:prefix "status-io-annot"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "bead-x" :title "Task X"}]})
        (with-redefs [oio/find-active-iteration (fn [_] "001")
                      iio/load-all-beads (fn [_] [{"id" "bead-x" "title" "Task X" "status" "in_progress"
                                                    "priority" "1" "dependencies" []}])]
          (let [result (sio/load-iteration-data project)
                story (first (:stories result))]
            (should= "bead-x" (:id story))
            (should= "in_progress" (:status story))))
        (fs/delete-tree project)))

    (it "shows unknown status for stories without matching beads"
      (let [project (str (fs/create-temp-dir {:prefix "status-io-miss"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "gone-bead" :title "Missing"}]})
        (with-redefs [oio/find-active-iteration (fn [_] "001")
                      iio/load-all-beads (fn [_] [])]
          (let [result (sio/load-iteration-data project)
                story (first (:stories result))]
            (should= "unknown" (:status story))))
        (fs/delete-tree project)))

    (it "handles iteration with no stories"
      (let [project (str (fs/create-temp-dir {:prefix "status-io-empty"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories []})
        (with-redefs [oio/find-active-iteration (fn [_] "001")
                      iio/load-all-beads (fn [_] [])]
          (let [result (sio/load-iteration-data project)]
            (should= 1 (:number result))
            (should= [] (:stories result))
            (should= {:total 0 :closed 0 :percent 0} (:stats result))))
        (fs/delete-tree project)))

    (it "uses iteration number from edn data"
      (let [project (str (fs/create-temp-dir {:prefix "status-io-num"}))]
        (make-iteration-dir! project "003"
                             {:number 3
                              :status :active
                              :stories []})
        (with-redefs [oio/find-active-iteration (fn [_] "003")
                      iio/load-all-beads (fn [_] [])]
          (should= 3 (:number (sio/load-iteration-data project))))
        (fs/delete-tree project))))

  (context "load-and-status"

    (it "formats dashboard for all projects"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "proj-a" :status :active :priority :normal :path "/tmp/proj-a"}]})
                    rio/load-project-config (fn [_] {:max-workers 2})
                    rio/count-workers (fn [_] {})
                    sio/load-iteration-data (fn [_] {:number 1 :status "active"
                                                     :stories [{:id "b1" :title "T" :status "open" :priority nil :deps []}]
                                                     :stats {:total 1 :closed 0 :percent 0}})]
        (let [result (sio/load-and-status {})]
          (should-contain "BRAIDS STATUS" result)
          (should-contain "proj-a" result))))

    (it "formats JSON dashboard when json? is true"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "proj-b" :status :active :priority :normal :path "/tmp/proj-b"}]})
                    rio/load-project-config (fn [_] {:max-workers 1})
                    rio/count-workers (fn [_] {})
                    sio/load-iteration-data (fn [_] {:number 2 :status "active"
                                                     :stories [] :stats {:total 0 :closed 0 :percent 0}})]
        (let [result (sio/load-and-status {:json? true})
              parsed (json/parse-string result true)]
          (should= 1 (count (:projects parsed)))
          (should= "proj-b" (:slug (first (:projects parsed)))))))

    (it "filters to single project when project-slug given"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "alpha" :status :active :priority :normal :path "/tmp/alpha"}
                                                          {:slug "beta" :status :active :priority :normal :path "/tmp/beta"}]})
                    rio/load-project-config (fn [_] {:max-workers 1})
                    rio/count-workers (fn [_] {})
                    sio/load-iteration-data (fn [_] {:number 1 :status "active"
                                                     :stories [] :stats {:total 0 :closed 0 :percent 0}})]
        (let [result (sio/load-and-status {:project-slug "alpha"})]
          (should-contain "alpha" result)
          (should-not-contain "beta" result))))

    (it "returns not-found message for unknown project-slug"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "exists" :status :active :priority :normal :path "/tmp/exists"}]})
                    rio/load-project-config (fn [_] {:max-workers 1})
                    rio/count-workers (fn [_] {})
                    sio/load-iteration-data (fn [_] nil)]
        (should= "Project not found: nope"
                 (sio/load-and-status {:project-slug "nope"}))))

    (it "includes worker counts in dashboard"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "proj-w" :status :active :priority :normal :path "/tmp/proj-w"}]})
                    rio/load-project-config (fn [_] {:max-workers 3})
                    rio/count-workers (fn [_] {"proj-w" 2})
                    sio/load-iteration-data (fn [_] {:number 1 :status "active"
                                                     :stories [] :stats {:total 0 :closed 0 :percent 0}})]
        (let [result (sio/load-and-status {})]
          (should-contain "workers:2/3" result))))

    (it "handles empty registry"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects []})
                    rio/count-workers (fn [_] {})]
        (let [result (sio/load-and-status {})]
          (should-contain "No projects registered" result))))

    (it "handles projects without active iterations"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "dormant" :status :active :priority :normal :path "/tmp/dormant"}]})
                    rio/load-project-config (fn [_] {:max-workers 1})
                    rio/count-workers (fn [_] {})
                    sio/load-iteration-data (fn [_] nil)]
        (let [result (sio/load-and-status {})]
          (should-contain "dormant" result)
          (should-not-contain "iter:" result))))

    (it "formats single project JSON when project-slug and json? given"
      (with-redefs [rio/resolve-state-home (fn [] "/tmp/state")
                    rio/load-registry (fn [_] {:projects [{:slug "jp" :status :active :priority :normal :path "/tmp/jp"}]})
                    rio/load-project-config (fn [_] {:max-workers 2})
                    rio/count-workers (fn [_] {})
                    sio/load-iteration-data (fn [_] {:number 1 :status "active"
                                                     :stories [] :stats {:total 0 :closed 0 :percent 0}})]
        (let [result (sio/load-and-status {:project-slug "jp" :json? true})
              parsed (json/parse-string result true)]
          (should= 1 (count (:projects parsed)))
          (should= "jp" (:slug (first (:projects parsed)))))))))
