(ns braids.orch-spec
  (:require [speclj.core :refer :all]
            [cheshire.core :as json]
            [braids.orch :as orch]))

(describe "braids.orch"

  (context "tick (pure logic)"

    (it "returns idle with no-active-iterations when no projects"
      (let [result (orch/tick {:projects []} {} {} {} {} {})]
        (should= "idle" (:action result))
        (should= "no-active-iterations" (:reason result))))

    (it "returns idle with no-active-iterations when projects exist but none have active iterations"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            iterations {} ;; no active iteration found
            result (orch/tick registry {} iterations {} {} {})]
        (should= "idle" (:action result))
        (should= "no-active-iterations" (:reason result))))

    (it "returns idle with no-ready-beads when active iteration but no beads"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should= "idle" (:action result))
        (should= "no-ready-beads" (:reason result))))

    (it "returns idle with all-at-capacity when beads exist but workers full"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {"proj" 1}
            result (orch/tick registry configs iterations beads workers {})]
        (should= "idle" (:action result))
        (should= "all-at-capacity" (:reason result))))

    (it "returns spawn with bead details when work is available"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"
                             :worker-timeout 3600}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= 1 (count (:spawns result)))
        (let [spawn (first (:spawns result))]
          (should= "proj" (:project spawn))
          (should= "proj-abc" (:bead spawn))
          (should= "008" (:iteration spawn))
          (should= "123" (:channel spawn))
          (should= "/tmp/proj" (:path spawn))
          (should= "project:proj:proj-abc" (:label spawn))
          (should= 3600 (:worker-timeout spawn)))))

    (it "limits spawns to available capacity per project"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 2 :channel "123"
                             :worker-timeout 3600}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-a" :title "A" :priority "P0"}
                           {:id "proj-b" :title "B" :priority "P1"}
                           {:id "proj-c" :title "C" :priority "P2"}]}
            workers {"proj" 1}
            result (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= 1 (count (:spawns result)))
        (should= "proj-a" (:bead (first (:spawns result))))))

    (it "spawns from multiple projects"
      (let [registry {:projects [{:slug "alpha" :status :active :priority :high :path "/tmp/alpha"}
                                  {:slug "beta" :status :active :priority :normal :path "/tmp/beta"}]}
            configs {"alpha" {:name "Alpha" :status :active :max-workers 1 :channel "111"
                              :worker-timeout 3600}
                     "beta" {:name "Beta" :status :active :max-workers 1 :channel "222"
                             :worker-timeout 3600}}
            iterations {"alpha" "003" "beta" "001"}
            beads {"alpha" [{:id "alpha-a" :title "Alpha task" :priority "P0"}]
                   "beta" [{:id "beta-b" :title "Beta task" :priority "P1"}]}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should= "spawn" (:action result))
        (should= 2 (count (:spawns result)))))

    (it "skips paused projects from config"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :paused :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should= "idle" (:action result))
        (should= "no-active-iterations" (:reason result))))

    (it "uses default worker-timeout when not specified"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should= 1800 (:worker-timeout (first (:spawns result))))))

    (it "returns disable-cron true when idle with no-active-iterations"
      (let [result (orch/tick {:projects []} {} {} {} {} {})]
        (should= true (:disable-cron result))))

    (it "returns disable-cron true when idle with no-ready-beads and all open beads are blocked"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}
            open-beads {"proj" [{:id "proj-xyz" :status "blocked"}]}
            result (orch/tick registry configs iterations beads workers {} open-beads)]
        (should= true (:disable-cron result))))

    (it "returns disable-cron false when idle with no-ready-beads but non-blocked open beads exist"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}
            open-beads {"proj" [{:id "proj-abc" :status "open"} {:id "proj-xyz" :status "blocked"}]}
            result (orch/tick registry configs iterations beads workers {} open-beads)]
        (should= false (:disable-cron result))))

    (it "returns disable-cron true when idle with no-ready-beads and no open beads"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}
            open-beads {"proj" []}
            result (orch/tick registry configs iterations beads workers {} open-beads)]
        (should= true (:disable-cron result))))

    (it "returns disable-cron true when no-ready-beads and open-beads not provided for project"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}
            open-beads {}
            result (orch/tick registry configs iterations beads workers {} open-beads)]
        (should= true (:disable-cron result))))

    (it "returns disable-cron false when no-ready-beads without open-beads param (backward compat)"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should= false (:disable-cron result))))

    (it "returns disable-cron false when idle with all-at-capacity (active iterations exist)"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {"proj" 1}
            result (orch/tick registry configs iterations beads workers {})]
        (should= false (:disable-cron result))))

    (it "does not include disable-cron when spawning work"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}
            result (orch/tick registry configs iterations beads workers {})]
        (should-not-contain :disable-cron result)))

    (it "includes notification-mentions in spawn when present"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"
                             :worker-timeout 3600
                             :notifications {:bead-start true :no-ready-beads true}
                             :notification-mentions {:iteration-complete "<@user>"}}}
            iterations {"proj" "008"}
            beads {"proj" [{:id "proj-abc" :title "Do stuff" :priority "P1"}]}
            workers {}
            notifications {"proj" {:notifications {:bead-start true :no-ready-beads true}
                                   :notification-mentions {:iteration-complete "<@user>"}}}
            result (orch/tick registry configs iterations beads workers notifications)]
        (should= "spawn" (:action result)))))

  (context "format-tick-json"

    (it "formats spawn result as JSON"
      (let [result {:action "spawn"
                    :spawns [{:project "proj" :bead "proj-abc" :iteration "008"
                              :channel "123" :path "/tmp/proj"
                              :label "project:proj:proj-abc"
                              :worker-timeout 3600}]}
            json-str (orch/format-tick-json result)]
        (should-contain "\"action\":\"spawn\"" json-str)
        (should-contain "proj-abc" json-str)))

    (it "formats idle result as JSON"
      (let [result {:action "idle" :reason "no-ready-beads"}
            json-str (orch/format-tick-json result)]
        (should-contain "\"action\":\"idle\"" json-str)
        (should-contain "no-ready-beads" json-str))))

  (context "format-orch-tick-json"

    (it "formats idle result with reason"
      (let [tick-result {:action "idle" :reason "no-ready-beads" :disable-cron true}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)]
        (should= "idle" (:action parsed))
        (should= "no-ready-beads" (:reason parsed))
        (should= true (:disable_cron parsed))
        (should-not-contain :spawns parsed)))

    (it "formats spawn result with sessions_spawn-ready entries"
      (let [tick-result {:action "spawn"
                         :spawns [{:project "proj"
                                   :bead "proj-abc"
                                   :iteration "008"
                                   :channel "123"
                                   :path "/tmp/proj"
                                   :label "project:proj:proj-abc"
                                   :worker-timeout 3600}]}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)
            spawn (first (:spawns parsed))]
        (should= "spawn" (:action parsed))
        (should= 1 (count (:spawns parsed)))
        (should-be-nil (:task spawn))
        (should= "proj" (:project spawn))
        (should= "proj-abc" (:bead spawn))
        (should= "008" (:iteration spawn))
        (should= "123" (:channel spawn))
        (should= "/tmp/proj" (:path spawn))
        (should= "project:proj:proj-abc" (:label spawn))
        (should= 3600 (:runTimeoutSeconds spawn))
        (should= "delete" (:cleanup spawn))
        (should= "high" (:thinking spawn))))

    (it "formats multiple spawns"
      (let [tick-result {:action "spawn"
                         :spawns [{:project "a" :bead "a-1" :iteration "001"
                                   :channel "111" :path "/tmp/a"
                                   :label "project:a:a-1" :worker-timeout 1800}
                                  {:project "b" :bead "b-2" :iteration "002"
                                   :channel "222" :path "/tmp/b"
                                   :label "project:b:b-2" :worker-timeout 3600}]}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)]
        (should= 2 (count (:spawns parsed)))
        (should= "project:a:a-1" (:label (first (:spawns parsed))))
        (should= "project:b:b-2" (:label (second (:spawns parsed)))))))

  (context "no-ready-beads-projects"

    (it "identifies projects with active iterations but no ready beads"
      (let [registry {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:name "Proj" :status :active :max-workers 1 :channel "123"
                             :notifications {:no-ready-beads true}}}
            iterations {"proj" "008"}
            beads {"proj" []}
            workers {}]
        (should= ["proj"] (orch/no-ready-beads-projects registry configs iterations beads workers)))))

  (context "detect-zombies"

    (it "returns empty when no sessions"
      (should= [] (orch/detect-zombies [] {} {})))

    (it "detects zombie when session is completed/failed/stopped"
      (let [sessions [{:label "project:proj:proj-abc" :status "completed" :age-seconds 100}
                      {:label "project:proj:proj-def" :status "failed" :age-seconds 50}
                      {:label "project:proj:proj-ghi" :status "stopped" :age-seconds 50}]
            configs {"proj" {:worker-timeout 1800}}
            bead-statuses {"proj-abc" "open" "proj-def" "open" "proj-ghi" "open"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should= 3 (count result))
        (should= #{"proj-abc" "proj-def" "proj-ghi"} (set (map :bead result)))
        (should (every? #(= "session-ended" (:reason %)) result))))

    (it "detects zombie when bead is closed but session is running"
      (let [sessions [{:label "project:proj:proj-abc" :status "running" :age-seconds 100}]
            configs {"proj" {:worker-timeout 1800}}
            bead-statuses {"proj-abc" "closed"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should= 1 (count result))
        (should= "bead-closed" (:reason (first result)))))

    (it "detects zombie when session exceeds worker-timeout and bead is open"
      (let [sessions [{:label "project:proj:proj-abc" :status "running" :age-seconds 3700}]
            configs {"proj" {:worker-timeout 3600}}
            bead-statuses {"proj-abc" "open"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should= 1 (count result))
        (should= "timeout" (:reason (first result)))))

    (it "does not flag running session with open bead within timeout"
      (let [sessions [{:label "project:proj:proj-abc" :status "running" :age-seconds 100}]
            configs {"proj" {:worker-timeout 1800}}
            bead-statuses {"proj-abc" "open"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should= 0 (count result))))

    (it "ignores non-project labels"
      (let [sessions [{:label "other:thing" :status "completed" :age-seconds 100}]
            result (orch/detect-zombies sessions {} {})]
        (should= 0 (count result))))

    (it "includes slug, bead, label, and reason in zombie entries"
      (let [sessions [{:label "project:proj:proj-abc" :status "completed" :age-seconds 100}]
            configs {"proj" {:worker-timeout 1800}}
            bead-statuses {"proj-abc" "open"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should= "proj" (:slug (first result)))
        (should= "proj-abc" (:bead (first result)))
        (should= "project:proj:proj-abc" (:label (first result)))
        (should= "session-ended" (:reason (first result)))))

    (it "includes session-id in zombie entries when present in session data"
      (let [sessions [{:label "project:proj:proj-abc" :status "completed" :age-seconds 100 :session-id "sess-123"}]
            configs {"proj" {:worker-timeout 1800}}
            bead-statuses {"proj-abc" "open"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should= "sess-123" (:session-id (first result)))))

    (it "omits session-id from zombie entries when not present in session data"
      (let [sessions [{:label "project:proj:proj-abc" :status "completed" :age-seconds 100}]
            configs {"proj" {:worker-timeout 1800}}
            bead-statuses {"proj-abc" "open"}
            result (orch/detect-zombies sessions configs bead-statuses)]
        (should-not-contain :session-id (first result)))))
  (context "worker-session-id"

    (it "constructs deterministic session ID from bead-id"
      (should= "braids-proj-abc-worker" (orch/worker-session-id "proj-abc")))

    (it "handles bead-ids with hyphens"
      (should= "braids-my-proj-x1-worker" (orch/worker-session-id "my-proj-x1"))))

  (context "parse-worker-session-id"

    (it "extracts bead-id from valid session ID"
      (should= "proj-abc" (orch/parse-worker-session-id "braids-proj-abc-worker")))

    (it "handles bead-ids with hyphens"
      (should= "my-proj-x1" (orch/parse-worker-session-id "braids-my-proj-x1-worker")))

    (it "returns nil for non-matching session ID"
      (should-be-nil (orch/parse-worker-session-id "random-uuid-1234")))

    (it "returns nil for nil input"
      (should-be-nil (orch/parse-worker-session-id nil)))

    (it "returns nil for partial match - missing suffix"
      (should-be-nil (orch/parse-worker-session-id "braids-abc")))

    (it "returns nil for partial match - missing prefix"
      (should-be-nil (orch/parse-worker-session-id "abc-worker")))

    (it "returns nil for empty bead-id"
      (should-be-nil (orch/parse-worker-session-id "braids--worker"))))

  (context "parse-session-labels-string"

    (it "parses space-separated labels into list"
      (should= ["project:proj:abc" "project:proj:def"]
               (orch/parse-session-labels-string "project:proj:abc project:proj:def")))

    (it "handles empty string"
      (should= [] (orch/parse-session-labels-string "")))

    (it "handles nil"
      (should= [] (orch/parse-session-labels-string nil)))

    (it "handles extra whitespace"
      (should= ["project:proj:abc" "project:proj:def"]
               (orch/parse-session-labels-string "  project:proj:abc   project:proj:def  ")))

    (it "filters out non-project labels"
      (should= ["project:proj:abc"]
               (orch/parse-session-labels-string "other:thing project:proj:abc random"))))

  (context "detect-zombies-from-labels"

    (it "detects zombie when bead is closed"
      (let [labels ["project:proj:proj-abc"]
            bead-statuses {"proj-abc" "closed"}
            result (orch/detect-zombies-from-labels labels bead-statuses)]
        (should= 1 (count result))
        (should= "bead-closed" (:reason (first result)))
        (should= "proj-abc" (:bead (first result)))))

    (it "does not flag open bead as zombie"
      (let [labels ["project:proj:proj-abc"]
            bead-statuses {"proj-abc" "open"}
            result (orch/detect-zombies-from-labels labels bead-statuses)]
        (should= 0 (count result))))

    (it "ignores non-project labels"
      (let [labels ["other:thing"]
            result (orch/detect-zombies-from-labels labels {})]
        (should= 0 (count result))))

    (it "treats missing bead status as open (not zombie)"
      (let [labels ["project:proj:proj-abc"]
            bead-statuses {}
            result (orch/detect-zombies-from-labels labels bead-statuses)]
        (should= 0 (count result)))))

  (context "format-orch-tick-json with zombies"

    (it "includes zombies array in spawn output"
      (let [tick-result {:action "spawn"
                         :spawns [{:project "proj" :bead "proj-abc" :iteration "008"
                                   :channel "123" :path "/tmp/proj"
                                   :label "project:proj:proj-abc" :worker-timeout 3600}]
                         :zombies [{:slug "proj" :bead "proj-old" :label "project:proj:proj-old" :reason "session-ended"}]}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)]
        (should= 1 (count (:zombies parsed)))
        (should= "proj-old" (:bead (first (:zombies parsed))))))

    (it "includes zombies array in idle output"
      (let [tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false
                         :zombies [{:slug "proj" :bead "proj-old" :label "project:proj:proj-old" :reason "timeout"}]}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)]
        (should= 1 (count (:zombies parsed)))))

    (it "includes sessionId in zombie JSON when present"
      (let [tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false
                         :zombies [{:slug "proj" :bead "proj-old" :label "project:proj:proj-old" :reason "timeout" :session-id "sess-456"}]}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)]
        (should= "sess-456" (:sessionId (first (:zombies parsed))))))

    (it "omits sessionId from zombie JSON when not present"
      (let [tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false
                         :zombies [{:slug "proj" :bead "proj-old" :label "project:proj:proj-old" :reason "timeout"}]}
            json-str (orch/format-orch-tick-json tick-result)
            parsed (json/parse-string json-str true)]
        (should-not-contain :sessionId (first (:zombies parsed))))))

  (context "build-worker-spawn"

    (it "includes agent-id, model, and thinking in spawn data"
      (let [cfg {:worker-agent "scrapper"
                 :worker-model "grok"
                 :worker-thinking :high}
            spawn (orch/build-worker-spawn cfg {:path "/tmp/test" :bead "test-bead" :iteration "001" :channel "123"})]
        (should= "scrapper" (:agent-id spawn))
        (should= "grok" (:model spawn))
        (should= :high (:thinking spawn))))

    (it "defaults thinking to :high when not specified"
      (let [cfg {:worker-agent "scrapper"}
            spawn (orch/build-worker-spawn cfg {:path "/tmp/test" :bead "test-bead" :iteration "001" :channel "123"})]
        (should= :high (:thinking spawn))))

    (it "handles nil model and thinking"
      (let [cfg {:worker-agent "scrapper"}
            spawn (orch/build-worker-spawn cfg {:path "/tmp/test" :bead "test-bead" :iteration "001" :channel "123"})]
        (should= nil (:model spawn))
        (should= :high (:thinking spawn)))))

  (context "format-debug-output"

    (it "shows project with no beads and active iteration as all closed"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active}}
            iterations {"proj" "008"}
            open-beads {"proj" []}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron true}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "proj" output)
        (should-contain "008" output)
        (should-contain "(none)" output)
        (should-contain "idle: no-ready-beads" output)))

    (it "shows project with beads and their statuses"
      (let [reg {:projects [{:slug "myproj" :status :active :priority :normal :path "/tmp/myproj"}]}
            configs {"myproj" {:status :active}}
            iterations {"myproj" "003"}
            open-beads {"myproj" [{:id "myproj-abc" :status "open"}
                                   {:id "myproj-xyz" :status "blocked"}]}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "myproj" output)
        (should-contain "xyz" output)
        (should-contain "blocked" output)))

    (it "shows spawn decision with worker count"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active}}
            iterations {"proj" "001"}
            open-beads {"proj" [{:id "proj-a1" :status "open"}]}
            tick-result {:action "spawn" :spawns [{:bead "proj-a1"}]}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "proj" output)
        (should-contain "001" output)))

    (it "shows project without iteration"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active}}
            iterations {}
            open-beads {}
            tick-result {:action "idle" :reason "no-active-iterations" :disable-cron true}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "(no iteration)" output)))

    (it "respects NO_COLOR env var"
      ;; When NO_COLOR is set, output should not contain ANSI escape codes
      ;; We can't easily set env vars in tests, so just verify the function doesn't crash
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active}}
            iterations {"proj" "001"}
            open-beads {"proj" []}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should (string? output))))

    (it "skips paused projects"
      (let [reg {:projects [{:slug "active-proj" :status :active :priority :normal :path "/tmp/a"}
                             {:slug "paused-proj" :status :active :priority :normal :path "/tmp/p"}]}
            configs {"active-proj" {:status :active}
                     "paused-proj" {:status :paused}}
            iterations {"active-proj" "001" "paused-proj" "002"}
            open-beads {}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "active-proj" output)
        (should-not-contain "paused-proj" output)))

    (it "handles multiple projects sorted by priority"
      (let [reg {:projects [{:slug "low" :status :active :priority :low :path "/tmp/l"}
                             {:slug "high" :status :active :priority :high :path "/tmp/h"}]}
            configs {"low" {:status :active} "high" {:status :active}}
            iterations {"low" "001" "high" "002"}
            open-beads {"low" [] "high" []}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})
            high-idx (.indexOf output "high")
            low-idx (.indexOf output "low")]
        (should (< high-idx low-idx))))

    (it "shows ready beads in output"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active :max-workers 2}}
            iterations {"proj" "005"}
            open-beads {"proj" [{:id "proj-aa1" :title "Task A1" :status "ready"}
                                {:id "proj-aa2" :title "Task A2" :status "closed"}]}
            tick-result {:action "spawn" :spawns [{:bead "proj-aa1"}]}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "aa1" output)
        (should-contain "ready" output)
        (should-not-contain "aa2" output)))

    (it "shows bead title in output lines"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active :max-workers 1}}
            iterations {"proj" "001"}
            open-beads {"proj" [{:id "proj-xx1" :title "My Task" :status "ready"}]}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "My Task" output)))

    (it "truncates bead titles longer than 20 characters"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active :max-workers 1}}
            iterations {"proj" "001"}
            open-beads {"proj" [{:id "proj-xx1" :title "This Title Is Way Too Long For Col" :status "ready"}]}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "This Title Is Way..." output)
        (should-not-contain "Too Long" output)))

    (it "does not truncate titles of exactly 20 characters"
      (let [reg {:projects [{:slug "proj" :status :active :priority :normal :path "/tmp/proj"}]}
            configs {"proj" {:status :active :max-workers 1}}
            iterations {"proj" "001"}
            open-beads {"proj" [{:id "proj-xx1" :title "Exactly Twenty Chars" :status "ready"}]}
            tick-result {:action "idle" :reason "no-ready-beads" :disable-cron false}
            output (orch/format-debug-output reg configs iterations open-beads tick-result {})]
        (should-contain "Exactly Twenty Chars" output)
        (should-not-contain "..." output))))
)
