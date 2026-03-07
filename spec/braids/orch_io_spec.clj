(ns braids.orch-io-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]
            [braids.orch-io :as oio]
            [braids.ready-io :as rio]
            [braids.config-io :as config-io]
            [braids.sys :as sys]))

(def test-tmp (str (fs/create-temp-dir {:prefix "orch-io-test"})))

(defn make-iteration-dir! [project iter-num edn-data]
  (let [dir (str project "/.braids/iterations/" iter-num)]
    (fs/create-dirs dir)
    (spit (str dir "/iteration.edn") (pr-str edn-data))
    dir))

(describe "braids.orch-io"

  (context "find-active-iteration"

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

  (after-all (proc/shell {:continue true} "rm" "-rf" test-tmp)))

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
      (proc/shell {:continue true} "rm" "-rf" dir)))

  (it "returns empty vector when no session stores exist"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-empty"}))]
      (should= [] (oio/load-sessions-from-stores dir))
      (proc/shell {:continue true} "rm" "-rf" dir)))

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
      (proc/shell {:continue true} "rm" "-rf" dir)))

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
      (proc/shell {:continue true} "rm" "-rf" dir)))

  (it "handles malformed JSON gracefully"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-bad"}))]
      (fs/create-dirs (str dir "/agents/a1/sessions"))
      (spit (str dir "/agents/a1/sessions/sessions.json") "not json")
      (should= [] (oio/load-sessions-from-stores dir))
      (proc/shell {:continue true} "rm" "-rf" dir)))

  (it "detects worker sessions by deterministic session-id pattern"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-sid"}))]
      (fs/create-dirs (str dir "/agents/worker1/sessions"))
      (spit (str dir "/agents/worker1/sessions/sessions.json")
            (json/generate-string
              {"braids-proj-abc-worker"
               {"sessionId" "braids-proj-abc-worker"
                "updatedAt" (- (System/currentTimeMillis) 60000)}
               "agent:worker1:discord:channel:123"
               {"sessionId" "sid-2"
                "updatedAt" (System/currentTimeMillis)}}))
      (let [sessions (oio/load-sessions-from-stores dir)]
        (should= 1 (count sessions))
        (should= "proj-abc" (:worker-bead-id (first sessions)))
        (should= "braids-proj-abc-worker" (:session-id (first sessions))))
      (proc/shell {:continue true} "rm" "-rf" dir)))

  (it "returns both label-matched and session-id-matched sessions"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-both"}))]
      (fs/create-dirs (str dir "/agents/a1/sessions"))
      (spit (str dir "/agents/a1/sessions/sessions.json")
            (json/generate-string
              {"agent:a1:subagent:u1"
               {"label" "project:proj:proj-x1"
                "updatedAt" (- (System/currentTimeMillis) 30000)
                "sessionId" "s1"}
               "braids-proj-y2-worker"
               {"sessionId" "braids-proj-y2-worker"
                "updatedAt" (- (System/currentTimeMillis) 60000)}}))
      (let [sessions (oio/load-sessions-from-stores dir)]
        (should= 2 (count sessions))
        (should= 1 (count (filter #(= "project:proj:proj-x1" (:label %)) sessions)))
        (should= 1 (count (filter #(= "proj-y2" (:worker-bead-id %)) sessions))))
      (proc/shell {:continue true} "rm" "-rf" dir)))

  (it "does not duplicate sessions matched by both label and session-id"
    (let [dir (str (fs/create-temp-dir {:prefix "sess-dedup"}))]
      (fs/create-dirs (str dir "/agents/a1/sessions"))
      (spit (str dir "/agents/a1/sessions/sessions.json")
            (json/generate-string
              {"braids-proj-abc-worker"
               {"label" "project:proj:proj-abc"
                "sessionId" "braids-proj-abc-worker"
                "updatedAt" (- (System/currentTimeMillis) 30000)}}))
      (let [sessions (oio/load-sessions-from-stores dir)]
        (should= 1 (count sessions))
        (should= "project:proj:proj-abc" (:label (first sessions))))
      (proc/shell {:continue true} "rm" "-rf" dir))))

;; ── Subprocess-based functions ──

(def sample-config {:env-path nil :bd-bin "bd" :openclaw-bin "openclaw"})

(defn stub-shell-with-beads
  "Returns a proc/shell stub that produces the given bead list as JSON."
  [beads]
  (fn [opts & _args]
    {:out (json/generate-string beads) :err ""}))

(describe "load-bead-statuses"

  (it "returns map of bead-id to lowercase status"
    (let [beads [{:id "b1" :status "Open"} {:id "b2" :status "Closed"}]]
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {})
                    sys/bd-bin (fn [_] "bd")
                    proc/shell (stub-shell-with-beads beads)]
        (should= {"b1" "open" "b2" "closed"} (oio/load-bead-statuses "/tmp/proj")))))

  (it "defaults missing status to open"
    (let [beads [{:id "b1"}]]
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {})
                    sys/bd-bin (fn [_] "bd")
                    proc/shell (stub-shell-with-beads beads)]
        (should= {"b1" "open"} (oio/load-bead-statuses "/tmp/proj")))))

  (it "returns empty map when bd returns non-sequential JSON"
    (with-redefs [config-io/load-config (fn [] sample-config)
                  sys/subprocess-env (fn [_] {})
                  sys/bd-bin (fn [_] "bd")
                  proc/shell (fn [opts & _] {:out "{\"error\": \"oops\"}" :err ""})]
      (should= {} (oio/load-bead-statuses "/tmp/proj"))))

  (it "returns empty map when bd command throws"
    (with-redefs [config-io/load-config (fn [] sample-config)
                  sys/subprocess-env (fn [_] {})
                  sys/bd-bin (fn [_] "bd")
                  proc/shell (fn [& _] (throw (Exception. "bd not found")))]
      (should= {} (oio/load-bead-statuses "/tmp/proj"))))

  (it "passes correct dir and args to shell"
    (let [captured (atom nil)]
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {})
                    sys/bd-bin (fn [_] "bd")
                    proc/shell (fn [opts & args]
                                 (reset! captured {:dir (:dir opts) :args (vec args)})
                                 {:out "[]" :err ""})]
        (oio/load-bead-statuses "/tmp/my-project")
        (should= "/tmp/my-project" (:dir @captured))
        (should= ["bd" "list" "--json"] (:args @captured))))))

(describe "load-open-beads"

  (it "returns non-closed beads"
    (let [beads [{:id "b1" :status "open" :title "Task 1"}
                 {:id "b2" :status "closed" :title "Done"}
                 {:id "b3" :status "in_progress" :title "Working"}]]
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {})
                    sys/bd-bin (fn [_] "bd")
                    proc/shell (stub-shell-with-beads beads)]
        (let [result (oio/load-open-beads "/tmp/proj")]
          (should= 2 (count result))
          (should= #{"b1" "b3"} (set (map :id result)))))))

  (it "returns empty vector when all beads are closed"
    (let [beads [{:id "b1" :status "closed"}]]
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {})
                    sys/bd-bin (fn [_] "bd")
                    proc/shell (stub-shell-with-beads beads)]
        (should= [] (oio/load-open-beads "/tmp/proj")))))

  (it "returns empty vector when bd throws"
    (with-redefs [config-io/load-config (fn [] sample-config)
                  sys/subprocess-env (fn [_] {})
                  sys/bd-bin (fn [_] "bd")
                  proc/shell (fn [& _] (throw (Exception. "fail")))]
      (should= [] (oio/load-open-beads "/tmp/proj"))))

  (it "treats missing status as open (non-closed)"
    (let [beads [{:id "b1"}]]
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {})
                    sys/bd-bin (fn [_] "bd")
                    proc/shell (stub-shell-with-beads beads)]
        (should= 1 (count (oio/load-open-beads "/tmp/proj")))))))

;; ── gather-and-tick variants ──

(def empty-registry {:projects []})
(def single-project-registry
  {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]})

(defn stub-gather-io
  "Common with-redefs stubs for gather-and-tick variants.
   configs: slug->config-map, iterations: slug->iteration-number,
   ready-beads: slug->beads-vec, open-beads: slug->beads-vec."
  [{:keys [state-home registry configs iterations ready-beads open-beads]
    :or {state-home "/fake/state"
         registry empty-registry
         configs {}
         iterations {}
         ready-beads {}
         open-beads {}}}]
  (let [path->slug (into {} (map (fn [{:keys [slug path]}] [path slug])
                                 (:projects registry)))]
    {#'rio/resolve-state-home (fn [] state-home)
     #'rio/load-registry (fn [_] registry)
     #'rio/load-project-config (fn [path]
                                   (let [slug (get path->slug path)]
                                     (get configs slug {:max-workers 1 :status :active})))
     #'oio/find-active-iteration (fn [path]
                                    (let [slug (get path->slug path)]
                                      (get iterations slug)))
     #'rio/load-ready-beads (fn [path]
                               (let [slug (get path->slug path)]
                                 (get ready-beads slug [])))
     #'oio/load-open-beads (fn [path]
                              (let [slug (get path->slug path)]
                                (get open-beads slug [])))
     #'rio/count-workers (fn [_] {})}))

(describe "gather-and-tick"

  (it "returns idle with no-active-iterations for empty registry"
    (with-redefs-fn (stub-gather-io {})
      #(let [result (oio/gather-and-tick)]
         (should= "idle" (:action result))
         (should= "no-active-iterations" (:reason result)))))

  (it "returns idle with no-active-iterations when no project has active iteration"
    (with-redefs-fn (stub-gather-io {:registry single-project-registry
                                     :iterations {}})
      #(let [result (oio/gather-and-tick)]
         (should= "idle" (:action result))
         (should= "no-active-iterations" (:reason result)))))

  (it "returns idle with no-ready-beads when active iteration but no beads"
    (with-redefs-fn (stub-gather-io {:registry single-project-registry
                                     :iterations {"proj" "001"}
                                     :ready-beads {}
                                     :open-beads {}})
      #(let [result (oio/gather-and-tick)]
         (should= "idle" (:action result))
         (should= "no-ready-beads" (:reason result)))))

  (it "returns spawn when project has iteration, capacity, and ready beads"
    (with-redefs-fn (stub-gather-io {:registry single-project-registry
                                     :configs {"proj" {:max-workers 2 :status :active}}
                                     :iterations {"proj" "001"}
                                     :ready-beads {"proj" [{:id "b1" :title "Task" :priority "1"}]}})
      #(let [result (oio/gather-and-tick)]
         (should= "spawn" (:action result))
         (should= 1 (count (:spawns result)))
         (should= "b1" (:bead (first (:spawns result)))))))

  (it "passes session-labels to count-workers"
    (let [captured-labels (atom nil)]
      (with-redefs-fn (merge (stub-gather-io {:registry single-project-registry
                                              :iterations {"proj" "001"}
                                              :ready-beads {"proj" [{:id "b1" :title "T" :priority "1"}]}})
                             {#'rio/count-workers (fn [labels]
                                                    (reset! captured-labels labels)
                                                    {})})
        #(oio/gather-and-tick {:session-labels ["project:proj:b1"]}))
      (should= ["project:proj:b1"] @captured-labels)))

  (it "loads ready beads only for projects with active iterations"
    (let [ready-called (atom [])
          reg {:projects [{:slug "a" :status :active :priority :normal :path "/tmp/a"}
                           {:slug "b" :status :active :priority :normal :path "/tmp/b"}]}]
      (with-redefs-fn (merge (stub-gather-io {:registry reg
                                              :iterations {"a" "001"}})
                             {#'rio/load-ready-beads (fn [path]
                                                       (swap! ready-called conj path)
                                                       [])})
        #(oio/gather-and-tick))
      ;; Only project "a" has an active iteration, so only it should have beads loaded
      (should= ["/tmp/a"] @ready-called))))

(describe "gather-and-tick-with-zombies"

  (it "returns tick result without zombies when no sessions"
    (with-redefs-fn (merge (stub-gather-io {})
                           {#'oio/load-bead-statuses (fn [_] {})})
      #(let [result (oio/gather-and-tick-with-zombies "[]")]
         (should= "idle" (:action result))
         (should-be-nil (:zombies result)))))

  (it "detects bead-closed zombies and includes them in result"
    (let [sessions-json (json/generate-string
                          [{:label "project:proj:b1"
                            :status "running"
                            :ageSeconds 100
                            :sessionId "braids-b1-worker"}])]
      (with-redefs-fn (merge (stub-gather-io {:registry single-project-registry
                                              :iterations {}
                                              :configs {"proj" {:max-workers 1 :worker-timeout 1800}}})
                             {#'oio/load-bead-statuses (fn [_] {"b1" "closed"})})
        #(let [result (oio/gather-and-tick-with-zombies sessions-json)]
           (should-not-be-nil (:zombies result))
           (should= 1 (count (:zombies result)))
           (should= "bead-closed" (:reason (first (:zombies result))))
           (should= "b1" (:bead (first (:zombies result))))))))

  (it "filters zombie labels from worker count"
    (let [worker-labels (atom nil)
          sessions-json (json/generate-string
                          [{:label "project:proj:b1"
                            :status "running"
                            :ageSeconds 100
                            :sessionId "s1"}
                           {:label "project:proj:b2"
                            :status "running"
                            :ageSeconds 50
                            :sessionId "s2"}])]
      (with-redefs-fn (merge (stub-gather-io {:registry single-project-registry
                                              :iterations {}
                                              :configs {"proj" {:max-workers 2 :worker-timeout 1800}}})
                             {#'oio/load-bead-statuses (fn [_] {"b1" "closed" "b2" "open"})
                              #'rio/count-workers (fn [labels]
                                                    (reset! worker-labels labels)
                                                    {})})
        #(oio/gather-and-tick-with-zombies sessions-json))
      ;; b1 is a zombie (bead-closed), so its label should be filtered out
      (should= ["project:proj:b2"] @worker-labels)))

  (it "handles empty JSON string gracefully"
    (with-redefs-fn (stub-gather-io {})
      #(let [result (oio/gather-and-tick-with-zombies "")]
         (should= "idle" (:action result)))))

  (it "handles malformed JSON string gracefully"
    (with-redefs-fn (stub-gather-io {})
      #(let [result (oio/gather-and-tick-with-zombies "not json")]
         (should= "idle" (:action result))))))

(describe "gather-and-tick-from-session-labels"

  (it "returns idle for empty session labels string"
    (with-redefs-fn (stub-gather-io {})
      #(let [result (oio/gather-and-tick-from-session-labels "")]
         (should= "idle" (:action result))
         (should-be-nil (:zombies result)))))

  (it "detects bead-closed zombies from label strings"
    (with-redefs-fn (merge (stub-gather-io {:registry single-project-registry
                                            :iterations {}
                                            :configs {"proj" {:max-workers 1 :worker-timeout 1800}}})
                           {#'oio/load-bead-statuses (fn [_] {"b1" "closed"})})
      #(let [result (oio/gather-and-tick-from-session-labels "project:proj:b1")]
         (should-not-be-nil (:zombies result))
         (should= 1 (count (:zombies result)))
         (should= "bead-closed" (:reason (first (:zombies result)))))))

  (it "filters zombie labels from worker count"
    (let [worker-labels (atom nil)]
      (with-redefs-fn (merge (stub-gather-io {:registry single-project-registry
                                              :iterations {}
                                              :configs {"proj" {:max-workers 2 :worker-timeout 1800}}})
                             {#'oio/load-bead-statuses (fn [_] {"b1" "closed" "b2" "open"})
                              #'rio/count-workers (fn [labels]
                                                    (reset! worker-labels labels)
                                                    {})})
        #(oio/gather-and-tick-from-session-labels "project:proj:b1 project:proj:b2"))
      ;; b1 is a zombie, should be filtered
      (should= ["project:proj:b2"] @worker-labels)))

  (it "only loads bead statuses for projects with active sessions"
    (let [status-paths (atom [])
          reg {:projects [{:slug "a" :status :active :priority :normal :path "/tmp/a"}
                           {:slug "b" :status :active :priority :normal :path "/tmp/b"}]}]
      (with-redefs-fn (merge (stub-gather-io {:registry reg})
                             {#'oio/load-bead-statuses (fn [path]
                                                         (swap! status-paths conj path)
                                                         {})})
        ;; Only project "a" has a session label
        #(oio/gather-and-tick-from-session-labels "project:a:bead1"))
      (should= ["/tmp/a"] @status-paths)))

  (it "does not load bead statuses when no project sessions"
    (let [status-called (atom false)]
      (with-redefs-fn (merge (stub-gather-io {:registry single-project-registry})
                             {#'oio/load-bead-statuses (fn [_]
                                                         (reset! status-called true)
                                                         {})})
        #(oio/gather-and-tick-from-session-labels ""))
      (should-not @status-called))))
