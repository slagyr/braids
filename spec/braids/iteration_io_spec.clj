(ns braids.iteration-io-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]
            [braids.iteration-io :as iio]
            [braids.orch-io :as oio]))

(defn make-iteration-dir!
  "Create a project with an iteration directory and iteration.edn."
  [project-path iter-num edn-data]
  (let [dir (str project-path "/.braids/iterations/" iter-num)]
    (fs/create-dirs dir)
    (spit (str dir "/iteration.edn") (pr-str edn-data))
    dir))

(describe "braids.iteration-io"

  (context "load-all-beads"

    (it "parses JSON output from bd list --all --json"
      (let [beads [{"id" "bead-1" "title" "First" "status" "open"}
                   {"id" "bead-2" "title" "Second" "status" "closed"}]]
        (with-redefs [proc/shell (fn [opts & _args]
                                   {:out (json/generate-string beads)
                                    :err ""})]
          (should= beads (iio/load-all-beads "/tmp/fake-project")))))

    (it "returns empty vector when bd returns empty array"
      (with-redefs [proc/shell (fn [opts & _args]
                                 {:out "[]" :err ""})]
        (should= [] (iio/load-all-beads "/tmp/fake-project"))))

    (it "returns empty vector when bd returns non-sequential JSON"
      (with-redefs [proc/shell (fn [opts & _args]
                                 {:out "{\"error\": \"not a list\"}" :err ""})]
        (should= [] (iio/load-all-beads "/tmp/fake-project"))))

    (it "returns empty vector when bd command throws exception"
      (with-redefs [proc/shell (fn [opts & _args]
                                 (throw (Exception. "bd not found")))]
        (should= [] (iio/load-all-beads "/tmp/fake-project"))))

    (it "returns empty vector when bd returns malformed JSON"
      (with-redefs [proc/shell (fn [opts & _args]
                                 {:out "not valid json {{{" :err ""})]
        (should= [] (iio/load-all-beads "/tmp/fake-project"))))

    (it "passes correct args to shell"
      (let [captured (atom nil)]
        (with-redefs [proc/shell (fn [opts & args]
                                   (reset! captured {:opts opts :args (vec args)})
                                   {:out "[]" :err ""})]
          (iio/load-all-beads "/tmp/my-project")
          (should= "/tmp/my-project" (:dir (:opts @captured)))
          (should= ["bd" "list" "--all" "--json"] (:args @captured)))))

    (it "expands tilde in project path"
      (let [captured (atom nil)
            home (System/getProperty "user.home")]
        (with-redefs [proc/shell (fn [opts & args]
                                   (reset! captured (:dir opts))
                                   {:out "[]" :err ""})]
          (iio/load-all-beads "~/my-project")
          (should= (str home "/my-project") @captured)))))

  (context "load-and-show"

    (it "returns 'No active iteration found.' when no active iteration exists"
      (with-redefs [oio/find-active-iteration (fn [_] nil)]
        (should= "No active iteration found."
                 (iio/load-and-show {:project-path "/tmp/fake"}))))

    (it "formats text output for active iteration with beads"
      (let [project (str (fs/create-temp-dir {:prefix "iter-io-show"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "bead-1" :title "First task"}
                                        {:id "bead-2" :title "Second task"}]})
        (let [beads [{"id" "bead-1" "title" "First task" "status" "closed" "priority" "1" "dependencies" []}
                     {"id" "bead-2" "title" "Second task" "status" "open" "priority" "2" "dependencies" []}]]
          (with-redefs [proc/shell (fn [opts & _args]
                                     {:out (json/generate-string beads) :err ""})]
            (let [result (iio/load-and-show {:project-path project})]
              (should-contain "Iteration 1" result)
              (should-contain "active" result)
              (should-contain "1/2 done" result)
              (should-contain "bead-1" result)
              (should-contain "bead-2" result))))
        (fs/delete-tree project)))

    (it "formats JSON output when json? is true"
      (let [project (str (fs/create-temp-dir {:prefix "iter-io-json"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "bead-1" :title "Task one"}]})
        (let [beads [{"id" "bead-1" "title" "Task one" "status" "open" "priority" "1" "dependencies" []}]]
          (with-redefs [proc/shell (fn [opts & _args]
                                     {:out (json/generate-string beads) :err ""})]
            (let [result (iio/load-and-show {:project-path project :json? true})
                  parsed (json/parse-string result true)]
              (should= 1 (:number parsed))
              (should= "active" (:status parsed))
              (should= 1 (count (:stories parsed)))
              (should= "bead-1" (:id (first (:stories parsed)))))))
        (fs/delete-tree project)))

    (it "shows unknown status for stories without matching beads"
      (let [project (str (fs/create-temp-dir {:prefix "iter-io-unknown"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "missing-bead" :title "Gone"}]})
        (with-redefs [proc/shell (fn [opts & _args]
                                   {:out "[]" :err ""})]
          (let [result (iio/load-and-show {:project-path project})]
            (should-contain "missing-bead" result)
            (should-contain "unknown" result)))
        (fs/delete-tree project)))

    (it "handles iteration with no stories"
      (let [project (str (fs/create-temp-dir {:prefix "iter-io-empty"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories []})
        (with-redefs [proc/shell (fn [opts & _args]
                                   {:out "[]" :err ""})]
          (let [result (iio/load-and-show {:project-path project})]
            (should-contain "Iteration 1" result)
            (should-contain "0/0 done" result)))
        (fs/delete-tree project)))

    (it "handles beads with dependencies"
      (let [project (str (fs/create-temp-dir {:prefix "iter-io-deps"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "bead-a" :title "First"}
                                        {:id "bead-b" :title "Second"}]})
        (let [beads [{"id" "bead-a" "title" "First" "status" "closed" "priority" "1" "dependencies" []}
                     {"id" "bead-b" "title" "Second" "status" "open" "priority" "2"
                      "dependencies" [{"depends_on_id" "bead-a"}]}]]
          (with-redefs [proc/shell (fn [opts & _args]
                                     {:out (json/generate-string beads) :err ""})]
            (let [result (iio/load-and-show {:project-path project})]
              (should-contain "bead-a" result))))
        (fs/delete-tree project)))

    (it "gracefully handles bd failure during load-and-show"
      (let [project (str (fs/create-temp-dir {:prefix "iter-io-fail"}))]
        (make-iteration-dir! project "001"
                             {:number 1
                              :status :active
                              :stories [{:id "bead-1" :title "Task"}]})
        (with-redefs [proc/shell (fn [opts & _args]
                                   (throw (Exception. "bd crashed")))]
          (let [result (iio/load-and-show {:project-path project})]
            ;; load-all-beads catches exception and returns []
            ;; so stories show as unknown status
            (should-contain "unknown" result)))
        (fs/delete-tree project)))))
