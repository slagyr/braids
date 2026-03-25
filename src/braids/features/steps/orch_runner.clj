(ns braids.features.steps.orch-runner
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.orch-runner :as orch-runner]
            [braids.orch :as orch]
            [clojure.string :as str]
            [speclj.core :refer :all]))

;; --- Helper functions ---

(defn- build-worker-task-impl! []
  (let [entry (g/get :runner-spawn-entry)
        task (orch-runner/build-worker-task entry)]
    (g/assoc! :runner-worker-task task)))

(defn- build-worker-args-impl! []
  (let [entry (g/get :runner-spawn-entry)
        config (g/get :runner-config)
        args (orch-runner/build-worker-args config entry)
        label-idx (when args (.indexOf ^java.util.List args "--label"))
        label (when (and label-idx (>= label-idx 0) (< (inc label-idx) (count args)))
                (nth args (inc label-idx)))]
    (g/assoc! :runner-worker-args args
              :label-result label)))

(defn- parse-cli-args-impl! []
  (let [args (g/get :runner-cli-args)
        result (orch-runner/parse-cli-args args)]
    (g/assoc! :runner-parsed-cli-args result)))

(defn- format-spawn-log-impl! []
  (let [result (g/get :runner-tick-result)
        config (g/get :runner-config)
        log (orch-runner/format-spawn-log (or config {}) result)]
    (g/assoc! :runner-log log)))

(defn- format-idle-log-impl! []
  (let [result (g/get :runner-tick-result)
        log (orch-runner/format-idle-log result)]
    (g/assoc! :runner-log log)))

(defn- format-zombie-log-impl! []
  (let [zombies (g/get :runner-zombies)
        log (orch-runner/format-zombie-log zombies)]
    (g/assoc! :runner-log log)))

;; --- Given steps ---

(defgiven spawn-entry-path-bead "a spawn entry with path {path:string} and bead {bead:string}"
  [path bead]
  (g/assoc! :runner-spawn-entry {:path path :bead bead}))

(defgiven spawn-iteration-channel "iteration {iteration:string} and channel {channel:string}"
  [iteration channel]
  (g/update! :runner-spawn-entry merge {:iteration iteration :channel channel}))

(defgiven spawn-entry-bead "a spawn entry with bead {bead:string}"
  [bead]
  (g/assoc! :runner-spawn-entry {:bead bead}))

(defgiven no-worker-agent "no custom worker agent"
  []
  nil)

(defgiven worker-agent "worker agent {agent:string}"
  [agent]
  (g/assoc-in! [:runner-spawn-entry :worker-agent] agent))

(defgiven no-cli-args "no CLI arguments"
  []
  (g/assoc! :runner-cli-args []))

(defgiven cli-args "CLI arguments {args:string}"
  [args]
  (g/assoc! :runner-cli-args [args]))

(defgiven spawn-tick-result "a spawn tick result with {count:int} workers"
  [count]
  (let [spawns (mapv (fn [b] {:bead b :worker-agent nil}) [])]
    (g/assoc! :runner-tick-result {:action "spawn" :spawns spawns})))

(defgiven spawn-beads "beads {b1:string} and {b2:string}"
  [b1 b2]
  (g/assoc-in! [:runner-tick-result :spawns]
               (mapv (fn [b] {:bead b :worker-agent nil}) [b1 b2])))

(defgiven idle-tick-result "an idle tick result with reason {reason:string}"
  [reason]
  (g/assoc! :runner-tick-result {:action "idle" :reason reason}))

(defgiven zombie-sessions #"^(\d+) zombie sessions with reasons \"([^\"]+)\" and \"([^\"]+)\"$"
  [count r1 r2]
  (let [zombies (mapv (fn [r] {:bead (str "z-" r) :reason r}) [r1 r2])]
    (g/assoc! :runner-zombies zombies)))

;; --- When steps ---

(defwhen build-worker-task "building the worker task"
  []
  (build-worker-task-impl!))

(defwhen build-worker-args "building the worker args"
  []
  (build-worker-args-impl!))

(defwhen parse-cli-args "parsing CLI args"
  []
  (parse-cli-args-impl!))

(defwhen format-spawn-log "formatting the spawn log"
  []
  (format-spawn-log-impl!))

(defwhen format-idle-log "formatting the idle log"
  []
  (format-idle-log-impl!))

(defwhen format-zombie-log "formatting the zombie log"
  []
  (format-zombie-log-impl!))

;; --- Then steps ---

(defthen assert-task-contains "the task should contain {expected:string}"
  [expected]
  (should (str/includes? (g/get :runner-worker-task) expected)))

(defthen assert-args-include "the args should include {expected:string}"
  [expected]
  (should (some #(= expected %) (g/get :runner-worker-args))))

(defthen assert-args-not-include "the args should not include {expected:string}"
  [expected]
  (should-not (some #(= expected %) (g/get :runner-worker-args))))

(defthen assert-agent-value "the agent value should be {expected:string}"
  [expected]
  (let [args (g/get :runner-worker-args)
        idx (.indexOf ^java.util.List args "--agent")]
    (should (>= idx 0))
    (should= expected (nth args (inc idx)))))

(defthen assert-dry-run "dry-run should be {val:string}"
  [val]
  (should= (= val "true") (boolean (:dry-run (g/get :runner-parsed-cli-args)))))

(defthen assert-verbose "verbose should be {val:string}"
  [val]
  (should= (= val "true") (boolean (:verbose (g/get :runner-parsed-cli-args)))))

(defthen assert-parse-error "parsing should return an error"
  []
  (should (:error (g/get :runner-parsed-cli-args))))

(defthen assert-error-contains "the error should contain {expected:string}"
  [expected]
  (should (str/includes? (:error (g/get :runner-parsed-cli-args)) expected)))

(defthen assert-log-contains "the log should contain {expected:string}"
  [expected]
  (should (some #(str/includes? % expected) (g/get :runner-log))))
