(ns braids.orch-io-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [braids.orch-io :as oio]))

(def test-tmp (str (fs/create-temp-dir {:prefix "orch-io-test"})))

(defn make-iteration-dir! [project iter-num edn-data]
  (let [dir (str project "/.braids/iterations/" iter-num)]
    (fs/create-dirs dir)
    (spit (str dir "/iteration.edn") (pr-str edn-data))
    dir))

(describe "braids.orch-io"

  (describe "find-active-iteration"

    (it "finds active iteration"
      (let [project (str test-tmp "/proj1")]
        (make-iteration-dir! project "001" {:number 1 :status :active :stories []})
        (should= "001" (oio/find-active-iteration project))))

    (it "skips non-active iterations"
      (let [project (str test-tmp "/proj3")]
        (make-iteration-dir! project "001" {:number 1 :status :complete :stories []})
        (make-iteration-dir! project "002" {:number 2 :status :active :stories []})
        (should= "002" (oio/find-active-iteration project))))

    (it "returns nil when no active iteration"
      (let [project (str test-tmp "/proj4")]
        (make-iteration-dir! project "001" {:number 1 :status :complete :stories []})
        (should-be-nil (oio/find-active-iteration project)))))

  (after-all (fs/delete-tree test-tmp)))

(describe "parse-session-labels (via gather-and-tick-with-zombies)"
  ;; We test the public interface rather than the private parse fn
  ;; The gather-and-tick-with-zombies function handles JSON parsing internally
  )
