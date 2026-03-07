(ns braids.integration-spec
  (:require [speclj.core :refer :all]
            [braids.core :as core]))

(describe "Integration: braids CLI dispatch"

  (it "help command returns 0"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run ["help"])))
      (should= 0 @result)))

  (it "unknown command returns 1"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run ["nonexistent"])))
      (should= 1 @result)))

  (it "--help flag returns 0"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run ["--help"])))
      (should= 0 @result)))

  (it "no args returns 0 (shows help)"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run nil)))
      (should= 0 @result))))


