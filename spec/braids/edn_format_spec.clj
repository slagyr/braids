(ns braids.edn-format-spec
  (:require [speclj.core :refer :all]
            [braids.edn-format :refer :all]))

(describe "edn-format"

  (it "formats a simple map with each key on its own line"
    (let [result (edn-format {:name "Test" :status :active})]
      (should-contain ":name" result)
      (should-contain ":status" result)
      (should-contain "\n" result)))

  (it "formats nested maps with indentation"
    (let [result (edn-format {:name "Test"
                              :notifications {:a true :b true}})]
      (should-contain ":notifications" result)
      (should-contain ":a true" result)))

  (it "round-trips through read-string"
    (let [data {:name "Braids"
                :status :active
                :priority :high
                :max-workers 1
                :notifications {:bead-start true
                                :bead-complete true}}
          formatted (edn-format data)
          parsed (read-string formatted)]
      (should= data parsed)))

  (it "handles vectors"
    (let [data {:stories ["a" "b" "c"]}
          formatted (edn-format data)
          parsed (read-string formatted)]
      (should= data parsed)))

  (it "handles empty maps"
    (let [result (edn-format {})]
      (should= "{}" (clojure.string/trim result))))

  (it "handles nested vectors of maps"
    (let [data {:stories [{:id "abc" :title "Do thing"}
                          {:id "def" :title "Other thing"}]}
          formatted (edn-format data)
          parsed (read-string formatted)]
      (should= data parsed)))

  (it "ends with a newline"
    (let [result (edn-format {:a 1})]
      (should (.endsWith result "\n"))))
  )
