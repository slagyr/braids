(ns braids.core-spec
  (:require [speclj.core :refer :all]
            [braids.core :as core]))

(describe "braids.core"

  (describe "dispatch"

    (it "returns help text with no args"
      (let [result (core/dispatch [])]
        (should= :help (:command result))))

    (it "returns help text with --help flag"
      (let [result (core/dispatch ["--help"])]
        (should= :help (:command result))))

    (it "returns help text with -h flag"
      (let [result (core/dispatch ["-h"])]
        (should= :help (:command result))))

    (it "returns help text with help subcommand"
      (let [result (core/dispatch ["help"])]
        (should= :help (:command result))))

    (it "dispatches unknown subcommand as error"
      (let [result (core/dispatch ["nonexistent"])]
        (should= :unknown (:command result))
        (should= "nonexistent" (:input result))))

    (it "dispatches ready subcommand"
      (let [result (core/dispatch ["ready"])]
        (should= :ready (:command result))))

    (it "passes remaining args to subcommand"
      (let [result (core/dispatch ["ready" "--verbose"])]
        (should= :ready (:command result))
        (should= ["--verbose"] (:args result)))))

  (describe "help-text"

    (it "includes braids in the output"
      (should-contain "braids" (core/help-text)))

    (it "lists available commands"
      (let [text (core/help-text)]
        (should-contain "ready" text)
        (should-contain "help" text))))

  (describe "run"

    (it "prints help and returns 0 for help command"
      (let [output (with-out-str (core/run []))]
        (should-contain "braids" output)))

    (it "prints error for unknown command and returns 1"
      (let [result (core/run ["nonexistent"])]
        (should= 1 result)))))
