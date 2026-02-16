(ns braids.project-config-spec
  (:require [speclj.core :refer :all]
            [braids.project-config :as pc]))

(describe "braids.project-config"

  (describe "parse-project-config"

    (it "parses a valid config.edn"
      (let [edn-str (pr-str {:name "My Project"
                              :status :active
                              :priority :normal
                              :autonomy :full
                              :checkin :on-demand
                              :channel "1234567890"
                              :max-workers 1
                              :worker-timeout 3600})
            result (pc/parse-project-config edn-str)]
        (should= "My Project" (:name result))
        (should= :active (:status result))
        (should= 1 (:max-workers result))))

    (it "applies defaults for missing fields"
      (let [edn-str (pr-str {:name "Minimal" :status :active :priority :normal :autonomy :full})
            result (pc/parse-project-config edn-str)]
        (should= 1 (:max-workers result))
        (should= 3600 (:worker-timeout result))
        (should= :on-demand (:checkin result))
        (should= nil (:channel result))))

    (it "preserves notifications table"
      (let [edn-str (pr-str {:name "Test"
                              :status :active
                              :priority :normal
                              :autonomy :full
                              :notifications {:iteration-start true
                                              :bead-complete false}})
            result (pc/parse-project-config edn-str)]
        (should= true (get-in result [:notifications :iteration-start]))
        (should= false (get-in result [:notifications :bead-complete]))))

    (it "defaults all notifications to true when missing"
      (let [result (pc/parse-project-config (pr-str {:name "T" :status :active :priority :normal :autonomy :full}))]
        (should= true (get-in result [:notifications :iteration-start]))
        (should= true (get-in result [:notifications :bead-complete]))
        (should= true (get-in result [:notifications :blocker]))))

    (it "does not include goal or guardrails (those belong in AGENTS.md)"
      (let [edn-str (pr-str {:name "Test" :status :active :priority :normal :autonomy :full})
            result (pc/parse-project-config edn-str)]
        (should-not-contain :goal result)
        (should-not-contain :guardrails result))))

  (describe "notification-mentions"

    (it "supports multiple mentions per event as vectors"
      (let [edn-str (pr-str {:name "Test"
                              :status :active
                              :priority :normal
                              :autonomy :full
                              :notification-mentions {:iteration-complete ["<@123>" "<@456>"]
                                                      :blocker ["<@789>"]}})
            result (pc/parse-project-config edn-str)]
        (should= ["<@123>" "<@456>"] (get-in result [:notification-mentions :iteration-complete]))
        (should= ["<@789>"] (get-in result [:notification-mentions :blocker]))))

    (it "normalizes single mention string to vector"
      (let [edn-str (pr-str {:name "Test"
                              :status :active
                              :priority :normal
                              :autonomy :full
                              :notification-mentions {:blocker "<@123>"}})
            result (pc/parse-project-config edn-str)]
        (should= ["<@123>"] (get-in result [:notification-mentions :blocker]))))

    (it "preserves empty notification-mentions when not specified"
      (let [edn-str (pr-str {:name "Test" :status :active :priority :normal :autonomy :full})
            result (pc/parse-project-config edn-str)]
        (should= nil (:notification-mentions result)))))

  (describe "validate-project-config"

    (it "validates a correct config"
      (let [cfg {:name "Test" :status :active :priority :normal :autonomy :full}]
        (should= [] (pc/validate-project-config cfg))))

    (it "rejects invalid status"
      (let [cfg {:name "Test" :status :done :priority :normal :autonomy :full}]
        (should-not= [] (pc/validate-project-config cfg))))

    (it "rejects invalid autonomy"
      (let [cfg {:name "Test" :status :active :priority :normal :autonomy :yolo}]
        (should-not= [] (pc/validate-project-config cfg))))

    (it "rejects non-positive max-workers"
      (let [cfg {:name "Test" :status :active :priority :normal :autonomy :full :max-workers 0}]
        (should-not= [] (pc/validate-project-config cfg))))

    (it "rejects missing name"
      (let [cfg {:status :active :priority :normal :autonomy :full}]
        (should-not= [] (pc/validate-project-config cfg)))))

  (describe "project-config->edn-string"

    (it "round-trips through parse"
      (let [cfg {:name "Test"
                 :status :active
                 :priority :normal
                 :autonomy :full
                 :checkin :on-demand
                 :channel nil
                 :max-workers 1
                 :worker-timeout 3600
                 :notifications {:iteration-start true
                                 :bead-start true
                                 :bead-complete true
                                 :iteration-complete true
                                 :no-ready-beads true
                                 :question true
                                 :blocker true}}
            edn-str (pc/project-config->edn-string cfg)
            parsed (pc/parse-project-config edn-str)]
        (should= cfg parsed)))))
