(ns braids.status-spec
  (:require [speclj.core :refer :all]
            [braids.status :as status]
            [cheshire.core :as json]))

(def sample-registry
  {:projects [{:slug "alpha" :status :active :priority :high :path "~/Projects/alpha"}
              {:slug "beta" :status :paused :priority :normal :path "~/Projects/beta"}
              {:slug "gamma" :status :active :priority :low :path "~/Projects/gamma"}]})

(def sample-configs
  {"alpha" {:status :active :max-workers 2 :channel "123"}
   "beta"  {:status :paused :max-workers 1 :channel ""}
   "gamma" {:status :active :max-workers 1 :channel "456"}})

(def sample-iterations
  {"alpha" {:number "009" :status "active"
            :stories [{:id "a-001" :title "Do thing" :status "closed"}
                      {:id "a-002" :title "Do other" :status "in_progress"}
                      {:id "a-003" :title "Do last" :status "open"}]
            :stats {:total 3 :closed 1 :percent 33}}
   "gamma" {:number "002" :status "active"
            :stories [{:id "g-001" :title "First" :status "closed"}
                      {:id "g-002" :title "Second" :status "closed"}]
            :stats {:total 2 :closed 2 :percent 100}}})

(def sample-workers {"alpha" 1 "gamma" 0})

(describe "braids.status"

  (context "build-dashboard"

    (it "includes all projects from registry"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)]
        (should= 3 (count (:projects dash)))))

    (it "marks active projects with iteration data"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            alpha (first (filter #(= "alpha" (:slug %)) (:projects dash)))]
        (should= "active" (:status alpha))
        (should= "009" (get-in alpha [:iteration :number]))
        (should= 33 (get-in alpha [:iteration :stats :percent]))))

    (it "marks paused projects without iteration"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            beta (first (filter #(= "beta" (:slug %)) (:projects dash)))]
        (should= "paused" (:status beta))
        (should-be-nil (:iteration beta))))

    (it "includes worker counts"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            alpha (first (filter #(= "alpha" (:slug %)) (:projects dash)))]
        (should= 1 (:workers alpha))
        (should= 2 (:max-workers alpha))))

    (it "returns empty dashboard for empty registry"
      (let [dash (status/build-dashboard {:projects []} {} {} {})]
        (should= [] (:projects dash)))))

  (context "format-dashboard"

    (it "shows project names and status"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            output (status/format-dashboard dash)]
        (should-contain "alpha" output)
        (should-contain "beta" output)
        (should-contain "gamma" output)))

    (it "shows iteration progress"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            output (status/format-dashboard dash)]
        (should-contain "33%" output)
        (should-contain "100%" output)))

    (it "shows worker counts for active projects"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            output (status/format-dashboard dash)]
        (should-contain "1/2" output)))

    (it "returns message for empty dashboard"
      (let [dash (status/build-dashboard {:projects []} {} {} {})
            output (status/format-dashboard dash)]
        (should= "No projects registered." output)))

    (it "shows paused indicator"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            output (status/format-dashboard dash)]
        (should-contain "paused" output))))

  (context "format-dashboard-json"

    (it "returns valid JSON"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            output (status/format-dashboard-json dash)
            parsed (json/parse-string output true)]
        (should= 3 (count (:projects parsed)))))

    (it "includes iteration stats in JSON"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            output (status/format-dashboard-json dash)
            parsed (json/parse-string output true)
            alpha (first (filter #(= "alpha" (:slug %)) (:projects parsed)))]
        (should= 33 (get-in alpha [:iteration :stats :percent])))))

  (context "format-project-detail"

    (it "shows detailed view for a single project"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            alpha (first (filter #(= "alpha" (:slug %)) (:projects dash)))
            output (status/format-project-detail alpha)]
        (should-contain "alpha" output)
        (should-contain "a-001" output)
        (should-contain "a-002" output)
        (should-contain "Do thing" output)))

    (it "shows completion stats"
      (let [dash (status/build-dashboard sample-registry sample-configs sample-iterations sample-workers)
            alpha (first (filter #(= "alpha" (:slug %)) (:projects dash)))
            output (status/format-project-detail alpha)]
        (should-contain "1/3" output)
        (should-contain "33%" output)))))
