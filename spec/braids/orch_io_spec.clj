(ns braids.orch-io-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [cheshire.core :as json]
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

(describe "load-sessions-from-stores"

  (it "extracts project labels from session store files"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-test"}))]
      (fs/create-dirs (str dir "/agents/worker1/sessions"))
      (spit (str dir "/agents/worker1/sessions/sessions.json")
            (json/generate-string
              {"agent:worker1:subagent:uuid-1"
               {"label" "project:myproj:myproj-abc"
                "updatedAt" (- (System/currentTimeMillis) 60000)
                "sessionId" "sid-1"}
               "agent:worker1:discord:channel:123"
               {"sessionId" "sid-2"
                "updatedAt" (System/currentTimeMillis)}}))
      (let [sessions (oio/load-sessions-from-stores dir)]
        (should= 1 (count sessions))
        (should= "project:myproj:myproj-abc" (:label (first sessions)))
        (should (pos? (:age-seconds (first sessions))))
        (should= "sid-1" (:session-id (first sessions))))
      (fs/delete-tree dir)))

  (it "returns empty vector when no session stores exist"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-empty"}))]
      (should= [] (oio/load-sessions-from-stores dir))
      (fs/delete-tree dir)))

  (it "handles multiple agents with project sessions"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-multi"}))]
      (fs/create-dirs (str dir "/agents/a1/sessions"))
      (fs/create-dirs (str dir "/agents/a2/sessions"))
      (spit (str dir "/agents/a1/sessions/sessions.json")
            (json/generate-string
              {"agent:a1:subagent:u1"
               {"label" "project:proj1:proj1-x1"
                "updatedAt" (- (System/currentTimeMillis) 30000)
                "sessionId" "s1"}}))
      (spit (str dir "/agents/a2/sessions/sessions.json")
            (json/generate-string
              {"agent:a2:subagent:u2"
               {"label" "project:proj2:proj2-y2"
                "updatedAt" (- (System/currentTimeMillis) 120000)
                "sessionId" "s2"}}))
      (let [sessions (oio/load-sessions-from-stores dir)]
        (should= 2 (count sessions))
        (should= #{"project:proj1:proj1-x1" "project:proj2:proj2-y2"}
                 (set (map :label sessions))))
      (fs/delete-tree dir)))

  (it "skips sessions without project labels"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-noproj"}))]
      (fs/create-dirs (str dir "/agents/a1/sessions"))
      (spit (str dir "/agents/a1/sessions/sessions.json")
            (json/generate-string
              {"agent:a1:subagent:u1"
               {"label" "other:something"
                "updatedAt" (System/currentTimeMillis)
                "sessionId" "s1"}
               "agent:a1:subagent:u2"
               {"label" "project:braids:braids-42h"
                "updatedAt" (System/currentTimeMillis)
                "sessionId" "s2"}}))
      (let [sessions (oio/load-sessions-from-stores dir)]
        (should= 1 (count sessions))
        (should= "project:braids:braids-42h" (:label (first sessions))))
      (fs/delete-tree dir)))

  (it "handles malformed JSON gracefully"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-bad"}))]
      (fs/create-dirs (str dir "/agents/a1/sessions"))
      (spit (str dir "/agents/a1/sessions/sessions.json") "not json")
      (should= [] (oio/load-sessions-from-stores dir))
      (fs/delete-tree dir))))
