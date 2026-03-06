(ns braids.ready-spec
  (:require [speclj.core :refer :all]
            [braids.ready :as ready]))

(describe "braids.ready"

  (context "ready-beads (pure logic)"

    (it "returns empty when no projects"
      (should= [] (ready/ready-beads {:projects []} {} {} {})))

    (it "returns empty when no active projects"
      (let [registry {:projects [{:slug "proj" :status :paused :priority :normal :path "/tmp/proj"}]}]
        (should= [] (ready/ready-beads registry {} {} {}))))

    (it "returns beads for active project with capacity"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1}}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}]
        (should= [{:project "proj" :id "proj-abc" :title "Do stuff" :priority "P1"}]
                 (ready/ready-beads registry configs beads workers))))

    (it "skips project at worker capacity"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1}}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {"proj" 1}]
        (should= [] (ready/ready-beads registry configs beads workers))))

    (it "returns beads when under capacity"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 3}}
            beads {"proj" [{:id "proj-abc" :title "Task A" :priority "P0"}
                           {:id "proj-def" :title "Task B" :priority "P1"}]}
            workers {"proj" 2}]
        (should= [{:project "proj" :id "proj-abc" :title "Task A" :priority "P0"}
                   {:project "proj" :id "proj-def" :title "Task B" :priority "P1"}]
                 (ready/ready-beads registry configs beads workers))))

    (it "uses default max-workers of 1 when not specified"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active}}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {"proj" 1}]
        (should= [] (ready/ready-beads registry configs beads workers))))

    (it "skips project whose config says paused even if registry says active"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :paused :max-workers 1}}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}]
        (should= [] (ready/ready-beads registry configs beads workers))))

    (it "handles multiple active projects"
      (let [registry {:projects [{:slug "alpha" :status :active :priority :high :path "/tmp/alpha"}
                                  {:slug "beta" :status :active :priority :normal :path "/tmp/beta"}]}
            configs {"alpha" {:name "Alpha" :status :active :max-workers 1}
                     "beta" {:name "Beta" :status :active :max-workers 1}}
            beads {"alpha" [{:id "alpha-aaa" :title "Alpha task" :priority "P0"}]
                   "beta" [{:id "beta-bbb" :title "Beta task" :priority "P1"}]}
            workers {}]
        (should= [{:project "alpha" :id "alpha-aaa" :title "Alpha task" :priority "P0"}
                   {:project "beta" :id "beta-bbb" :title "Beta task" :priority "P1"}]
                 (ready/ready-beads registry configs beads workers))))

    (it "orders results by project priority (high first)"
      (let [registry {:projects [{:slug "low" :status :active :priority :low :path "/tmp/low"}
                                  {:slug "high" :status :active :priority :high :path "/tmp/high"}
                                  {:slug "norm" :status :active :priority :normal :path "/tmp/norm"}]}
            configs {"low" {:name "Low" :status :active :max-workers 1}
                     "high" {:name "High" :status :active :max-workers 1}
                     "norm" {:name "Norm" :status :active :max-workers 1}}
            beads {"low" [{:id "low-aaa" :title "Low task" :priority "P2"}]
                   "high" [{:id "high-bbb" :title "High task" :priority "P0"}]
                   "norm" [{:id "norm-ccc" :title "Norm task" :priority "P1"}]}
            workers {}]
        (should= "high" (:project (first (ready/ready-beads registry configs beads workers))))
        (should= "norm" (:project (second (ready/ready-beads registry configs beads workers))))
        (should= "low" (:project (nth (ready/ready-beads registry configs beads workers) 2))))))

  (context "format-ready-output"

    (it "returns 'No ready beads.' when empty"
      (should= "No ready beads." (ready/format-ready-output [])))

    (it "formats beads as a list"
      (let [beads [{:project "proj" :id "proj-abc" :title "Do stuff" :priority "P0"}]]
        (should-contain "proj-abc" (ready/format-ready-output beads))
        (should-contain "Do stuff" (ready/format-ready-output beads))))

    (it "colorizes P1 priority in red"
      (let [beads [{:project "proj" :id "proj-abc" :title "Task" :priority "P1"}]
            output (ready/format-ready-output beads)]
        (should-contain "\033[31mP1\033[0m" output)))

    (it "colorizes P2 priority in yellow"
      (let [beads [{:project "proj" :id "proj-abc" :title "Task" :priority "P2"}]
            output (ready/format-ready-output beads)]
        (should-contain "\033[33mP2\033[0m" output)))

    (it "colorizes P3 priority in green"
      (let [beads [{:project "proj" :id "proj-abc" :title "Task" :priority "P3"}]
            output (ready/format-ready-output beads)]
        (should-contain "\033[32mP3\033[0m" output)))

    (it "colorizes P0 priority in red (high priority)"
      (let [beads [{:project "proj" :id "proj-abc" :title "Task" :priority "P0"}]
            output (ready/format-ready-output beads)]
        (should-contain "\033[31mP0\033[0m" output)))

    (it "renders bead title in bold white"
      (let [beads [{:project "proj" :id "proj-abc" :title "Do stuff" :priority "P1"}]
            output (ready/format-ready-output beads)]
        (should-contain "\033[1;37mDo stuff\033[0m" output)))

    (it "colorizes project name in cyan"
      (let [beads [{:project "proj" :id "proj-abc" :title "Task" :priority "P1"}]
            output (ready/format-ready-output beads)]
        (should-contain "\033[36mproj\033[0m" output)))))
