(ns braids.list-io-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [braids.list-io :as lio]
            [braids.ready-io :as rio]
            [braids.status-io :as sio]
            [braids.list :as list]))

(def sample-registry
  {:projects [{:slug "alpha" :status :active :priority :high :path "/tmp/alpha"}
              {:slug "beta" :status :paused :priority :normal :path "/tmp/beta"}
              {:slug "gamma" :status :active :priority :low :path "/tmp/gamma"}]})

(def sample-config-alpha {:max-workers 3})
(def sample-config-beta {:max-workers 1})
(def sample-config-gamma {:max-workers 2})

(def sample-iteration-alpha
  {:number "001" :status "active"
   :stories [{:id "b1" :title "Task 1" :status "closed"}
             {:id "b2" :title "Task 2" :status "open"}]
   :stats {:total 2 :closed 1 :percent 50}})

(def sample-iteration-gamma
  {:number "003" :status "active"
   :stories [{:id "b3" :title "Task 3" :status "closed"}]
   :stats {:total 1 :closed 1 :percent 100}})

(defn stub-io
  "Set up with-redefs stubs for load-and-list IO dependencies.
   Takes an options map to override default stubs."
  [{:keys [state-home registry configs iterations workers]
    :or {state-home "/fake/state"
         registry sample-registry
         configs {"alpha" sample-config-alpha
                  "beta" sample-config-beta
                  "gamma" sample-config-gamma}
         iterations {"alpha" sample-iteration-alpha
                     "gamma" sample-iteration-gamma}
         workers {"alpha" 2 "gamma" 1}}}]
  {#'rio/resolve-state-home (fn [] state-home)
   #'rio/load-registry (fn [_] registry)
   #'rio/load-project-config (fn [path]
(let [slug (last (str/split path #"/"))]
                                  (get configs slug {})))
   #'sio/load-iteration-data (fn [path]
                                 (let [slug (last (str/split path #"/"))]
                                  (get iterations slug)))
   #'rio/count-workers (fn [_] workers)})

(describe "braids.list-io"

  (context "load-and-list"

    (context "composition and data flow"

      (it "passes state-home from resolve-state-home to load-registry"
        (let [registry-home (atom nil)]
          (with-redefs [rio/resolve-state-home (fn [] "/my/state/home")
                        rio/load-registry (fn [home]
                                            (reset! registry-home home)
                                            {:projects []})
                        rio/load-project-config (fn [_] {})
                        sio/load-iteration-data (fn [_] nil)
                        rio/count-workers (fn [_] {})]
            (lio/load-and-list {})
            (should= "/my/state/home" @registry-home))))

      (it "loads config for every project in registry"
        (let [config-paths (atom [])]
          (with-redefs [rio/resolve-state-home (fn [] "/state")
                        rio/load-registry (fn [_] sample-registry)
                        rio/load-project-config (fn [path]
                                                  (swap! config-paths conj path)
                                                  {})
                        sio/load-iteration-data (fn [_] nil)
                        rio/count-workers (fn [_] {})]
            (lio/load-and-list {})
            (should= #{"/tmp/alpha" "/tmp/beta" "/tmp/gamma"} (set @config-paths)))))

      (it "loads iteration data only for active projects"
        (let [iter-paths (atom [])]
          (with-redefs [rio/resolve-state-home (fn [] "/state")
                        rio/load-registry (fn [_] sample-registry)
                        rio/load-project-config (fn [_] {})
                        sio/load-iteration-data (fn [path]
                                                  (swap! iter-paths conj path)
                                                  nil)
                        rio/count-workers (fn [_] {})]
            (lio/load-and-list {})
            ;; beta is :paused so should NOT have iteration loaded
            (should= #{"/tmp/alpha" "/tmp/gamma"} (set @iter-paths)))))

      (it "passes session-labels to count-workers"
        (let [captured-labels (atom nil)]
          (with-redefs [rio/resolve-state-home (fn [] "/state")
                        rio/load-registry (fn [_] {:projects []})
                        rio/load-project-config (fn [_] {})
                        sio/load-iteration-data (fn [_] nil)
                        rio/count-workers (fn [labels]
                                            (reset! captured-labels labels)
                                            {})]
            (lio/load-and-list {:session-labels ["project:alpha:b1" "project:gamma:b3"]})
            (should= ["project:alpha:b1" "project:gamma:b3"] @captured-labels))))

      (it "defaults session-labels to empty vector"
        (let [captured-labels (atom nil)]
          (with-redefs [rio/resolve-state-home (fn [] "/state")
                        rio/load-registry (fn [_] {:projects []})
                        rio/load-project-config (fn [_] {})
                        sio/load-iteration-data (fn [_] nil)
                        rio/count-workers (fn [labels]
                                            (reset! captured-labels labels)
                                            {})]
            (lio/load-and-list {})
            (should= [] @captured-labels)))))

    (context "enrichment"

      (it "enriches projects with worker counts and max-workers from config"
        (let [formatted (atom nil)]
          (with-redefs-fn (merge (stub-io {})
                                 {#'list/format-list (fn [data]
                                                       (reset! formatted data)
                                                       "")})
            #(lio/load-and-list {}))
          (let [projects (:projects @formatted)
                alpha (first (filter #(= "alpha" (:slug %)) projects))]
            (should= 2 (:workers alpha))
            (should= 3 (:max-workers alpha)))))

      (it "enriches active projects with iteration data"
        (let [formatted (atom nil)]
          (with-redefs-fn (merge (stub-io {})
                                 {#'list/format-list (fn [data]
                                                       (reset! formatted data)
                                                       "")})
            #(lio/load-and-list {}))
          (let [projects (:projects @formatted)
                alpha (first (filter #(= "alpha" (:slug %)) projects))]
            (should= sample-iteration-alpha (:iteration alpha)))))

      (it "does not enrich paused projects with iteration data"
        (let [formatted (atom nil)]
          (with-redefs-fn (merge (stub-io {})
                                 {#'list/format-list (fn [data]
                                                       (reset! formatted data)
                                                       "")})
            #(lio/load-and-list {}))
          (let [projects (:projects @formatted)
                beta (first (filter #(= "beta" (:slug %)) projects))]
            (should-be-nil (:iteration beta)))))

      (it "defaults worker count to 0 when no workers for a project"
        (let [formatted (atom nil)]
          (with-redefs-fn (merge (stub-io {:workers {"alpha" 2}})
                                 {#'list/format-list (fn [data]
                                                       (reset! formatted data)
                                                       "")})
            #(lio/load-and-list {}))
          (let [projects (:projects @formatted)
                beta (first (filter #(= "beta" (:slug %)) projects))]
            (should= 0 (:workers beta)))))

      (it "uses max-workers 1 as default when config has no max-workers"
        (let [formatted (atom nil)]
          (with-redefs-fn (merge (stub-io {:configs {"alpha" {} "beta" {} "gamma" {}}})
                                 {#'list/format-list (fn [data]
                                                       (reset! formatted data)
                                                       "")})
            #(lio/load-and-list {}))
          (let [projects (:projects @formatted)
                alpha (first (filter #(= "alpha" (:slug %)) projects))]
            (should= 1 (:max-workers alpha))))))

    (context "output format"

      (it "delegates to format-list for text output"
        (let [format-called (atom false)]
          (with-redefs-fn (merge (stub-io {})
                                 {#'list/format-list (fn [_]
                                                       (reset! format-called true)
                                                       "table output")})
            #(do
               (let [result (lio/load-and-list {})]
                 (should= "table output" result))))
          (should @format-called)))

      (it "delegates to format-list-json when json? is true"
        (let [json-called (atom false)]
          (with-redefs-fn (merge (stub-io {})
                                 {#'list/format-list-json (fn [_]
                                                            (reset! json-called true)
                                                            "{}")})
            #(do
               (let [result (lio/load-and-list {:json? true})]
                 (should= "{}" result))))
          (should @json-called)))

      (it "does not call format-list-json for non-json output"
        (let [json-called (atom false)]
          (with-redefs-fn (merge (stub-io {})
                                 {#'list/format-list (fn [_] "text")
                                  #'list/format-list-json (fn [_]
                                                            (reset! json-called true)
                                                            "{}")})
            #(lio/load-and-list {}))
          (should-not @json-called))))

    (context "edge cases"

      (it "handles empty registry with no projects"
        (with-redefs-fn (stub-io {:registry {:projects []}
                                  :configs {}
                                  :iterations {}
                                  :workers {}})
          #(let [result (lio/load-and-list {})]
             (should= "No projects registered." result))))

      (it "handles all projects being non-active (no iteration loading)"
        (let [iter-called (atom false)]
          (with-redefs [rio/resolve-state-home (fn [] "/state")
                        rio/load-registry (fn [_]
                                            {:projects [{:slug "x" :status :paused :priority :normal :path "/tmp/x"}]})
                        rio/load-project-config (fn [_] {:max-workers 1})
                        sio/load-iteration-data (fn [_]
                                                  (reset! iter-called true)
                                                  nil)
                        rio/count-workers (fn [_] {})]
            (lio/load-and-list {})
            (should-not @iter-called))))

      (it "handles iteration data returning nil for an active project"
        (let [formatted (atom nil)]
          (with-redefs-fn (merge (stub-io {:iterations {}})
                                 {#'list/format-list (fn [data]
                                                       (reset! formatted data)
                                                       "")})
            #(lio/load-and-list {}))
          (let [projects (:projects @formatted)
                alpha (first (filter #(= "alpha" (:slug %)) projects))]
            (should-be-nil (:iteration alpha))))))))
