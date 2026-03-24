(ns braids.features.steps.orch-runner
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [braids.features.harness :as h]
            [clojure.string :as str]
            [speclj.core :refer :all]))

;; --- Given steps ---

(defgiven spawn-entry-path-bead "a spawn entry with path \"{path}\" and bead \"{bead}\""
  [path bead]
  (h/set-spawn-entry {:path path :bead bead}))

(defgiven spawn-iteration-channel "iteration \"{iteration}\" and channel \"{channel}\""
  [iteration channel]
  (h/update-spawn-entry {:iteration iteration :channel channel}))

(defgiven spawn-entry-bead "a spawn entry with bead \"{bead}\""
  [bead]
  (h/set-spawn-entry {:bead bead}))

(defgiven no-worker-agent "no custom worker agent"
  []
  nil)

(defgiven worker-agent "worker agent \"{agent}\""
  [agent]
  (h/set-worker-agent agent))

(defgiven no-cli-args "no CLI arguments"
  []
  (h/set-cli-args []))

(defgiven cli-args "CLI arguments \"{args}\""
  [args]
  (h/set-cli-args [args]))

(defgiven spawn-tick-result "a spawn tick result with {count:int} workers"
  [count]
  (h/set-spawn-tick-result count []))

(defgiven spawn-beads "beads \"{b1}\" and \"{b2}\""
  [b1 b2]
  (h/add-spawn-beads [b1 b2]))

(defgiven idle-tick-result "an idle tick result with reason \"{reason}\""
  [reason]
  (h/set-idle-tick-result reason))

(defgiven zombie-sessions #"^(\d+) zombie sessions with reasons \"([^\"]+)\" and \"([^\"]+)\"$"
  [count r1 r2]
  (h/set-zombie-sessions (parse-long count) [r1 r2]))

;; --- When steps ---

(defwhen build-worker-task "building the worker task"
  []
  (h/build-worker-task!))

(defwhen build-worker-args "building the worker args"
  []
  (h/build-worker-args!))

(defwhen parse-cli-args "parsing CLI args"
  []
  (h/parse-cli-args!))

(defwhen format-spawn-log "formatting the spawn log"
  []
  (h/format-spawn-log!))

(defwhen format-idle-log "formatting the idle log"
  []
  (h/format-idle-log!))

(defwhen format-zombie-log "formatting the zombie log"
  []
  (h/format-zombie-log!))

;; --- Then steps ---

(defthen assert-task-contains "the task should contain \"{expected}\""
  [expected]
  (should (str/includes? (h/worker-task) expected)))

(defthen assert-args-include "the args should include \"{expected}\""
  [expected]
  (should (some #(= expected %) (h/worker-args))))

(defthen assert-args-not-include "the args should not include \"{expected}\""
  [expected]
  (should-not (some #(= expected %) (h/worker-args))))

(defthen assert-agent-value "the agent value should be \"{expected}\""
  [expected]
  (let [args (h/worker-args)
        idx (.indexOf args "--agent")]
    (should (>= idx 0))
    (should= expected (nth args (inc idx)))))

(defthen assert-dry-run "dry-run should be {val}"
  [val]
  (should= (= val "true") (boolean (:dry-run (h/parsed-cli-args)))))

(defthen assert-verbose "verbose should be {val}"
  [val]
  (should= (= val "true") (boolean (:verbose (h/parsed-cli-args)))))

(defthen assert-parse-error "parsing should return an error"
  []
  (should (:error (h/parsed-cli-args))))

(defthen assert-error-contains "the error should contain \"{expected}\""
  [expected]
  (should (str/includes? (:error (h/parsed-cli-args)) expected)))

(defthen assert-log-contains "the log should contain \"{expected}\""
  [expected]
  (should (some #(str/includes? % expected) (h/runner-log))))
