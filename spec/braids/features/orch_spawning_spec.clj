(ns braids.features.orch-spawning-spec
  (:require [speclj.core :refer :all]
            [braids.orch :as orch]))

;; Background: project "alpha" with max-workers 2 and active iteration "003"
(def alpha-project {:slug "alpha" :status :active :priority :normal :path "/tmp/alpha"})
(def alpha-config {:name "Alpha" :status :active :max-workers 2 :channel "111"})

(defn- make-beads
  "Generate n beads with sequential alphabetic IDs for project alpha."
  [n]
  (mapv (fn [i] {:id (str "alpha-" (char (+ (int \a) i))) :title (str "Task " i) :priority "P0"})
        (range n)))

(describe "Orchestrator spawning behavior"

  (context "Spawn workers when beads ready and capacity available"
    (it "spawns 2 workers when 3 beads ready and max-workers is 2"
      (let [registry   {:projects [alpha-project]}
            configs    {"alpha" alpha-config}
            iterations {"alpha" "003"}
            beads      {"alpha" (make-beads 3)}
            workers    {"alpha" 0}
            result     (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= 2 (count (:spawns result))))))

  (context "Spawn fewer workers when fewer beads than capacity"
    (it "spawns 1 worker when only 1 bead ready and max-workers is 2"
      (let [registry   {:projects [alpha-project]}
            configs    {"alpha" alpha-config}
            iterations {"alpha" "003"}
            beads      {"alpha" (make-beads 1)}
            workers    {"alpha" 0}
            result     (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= 1 (count (:spawns result))))))

  (context "Idle when no ready beads"
    (it "returns idle with no-ready-beads when project has 0 ready beads"
      (let [registry   {:projects [alpha-project]}
            configs    {"alpha" alpha-config}
            iterations {"alpha" "003"}
            beads      {"alpha" []}
            workers    {"alpha" 0}
            result     (orch/tick registry configs iterations beads workers {})]
        (should= "idle" (:action result))
        (should= "no-ready-beads" (:reason result)))))

  (context "Idle when at capacity"
    (it "returns idle with all-at-capacity when workers equal max-workers"
      (let [registry   {:projects [alpha-project]}
            configs    {"alpha" alpha-config}
            iterations {"alpha" "003"}
            beads      {"alpha" (make-beads 3)}
            workers    {"alpha" 2}
            result     (orch/tick registry configs iterations beads workers {})]
        (should= "idle" (:action result))
        (should= "all-at-capacity" (:reason result)))))

  (context "Idle when no active iterations"
    (it "returns idle with no-active-iterations for project with no iteration"
      (let [beta-project {:slug "beta" :status :active :priority :normal :path "/tmp/beta"}
            beta-config  {:name "Beta" :status :active :max-workers 1 :channel "222"}
            registry     {:projects [beta-project]}
            configs      {"beta" beta-config}
            iterations   {}
            beads        {"beta" (make-beads 3)}
            workers      {"beta" 0}
            result       (orch/tick registry configs iterations beads workers {})]
        (should= "idle" (:action result))
        (should= "no-active-iterations" (:reason result)))))

  (context "Spawn respects per-project capacity independently"
    (it "spawns 3 total workers across alpha (2) and beta (1)"
      (let [beta-project {:slug "beta" :status :active :priority :normal :path "/tmp/beta"}
            beta-config  {:name "Beta" :status :active :max-workers 1 :channel "222"}
            registry     {:projects [alpha-project beta-project]}
            configs      {"alpha" alpha-config "beta" beta-config}
            iterations   {"alpha" "003" "beta" "001"}
            beads        {"alpha" (make-beads 2)
                          "beta"  [{:id "beta-a" :title "Beta task" :priority "P0"}]}
            workers      {"alpha" 0 "beta" 0}
            result       (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= 3 (count (:spawns result))))))

  (context "Spawn includes correct label format"
    (it "formats spawn label as project:slug:bead-id"
      (let [registry   {:projects [alpha-project]}
            configs    {"alpha" alpha-config}
            iterations {"alpha" "003"}
            beads      {"alpha" [{:id "alpha-abc" :title "Task ABC" :priority "P0"}]}
            workers    {"alpha" 0}
            result     (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= "project:alpha:alpha-abc" (:label (first (:spawns result))))))))
