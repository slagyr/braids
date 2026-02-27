(ns braids.orch-log-spec
  (:require [speclj.core :refer :all]
            [braids.orch-log :as log]))

(describe "orch-log"

  (describe "format-log-lines"

    (it "includes start header with timestamp"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "myproj" :status :active :path "/tmp/myproj"}]}
                     :configs {"myproj" {:status :active :max-workers 1 :worker-timeout 3600}}
                     :iterations {"myproj" "003"}
                     :open-beads {"myproj" [{:id "myproj-abc" :status "open" :title "Do stuff"}]}
                     :ready-beads {"myproj" [{:id "myproj-abc"}]}
                     :workers {"myproj" 0}
                     :zombies []
                     :tick-result {:action "spawn" :spawns [{:project "myproj" :bead "myproj-abc"}]}}
                    "2026-02-27T16:00:00")]
        (should-contain "=== orch-run 2026-02-27T16:00:00 ===" (first lines))))

    (it "logs registry project count"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "a" :status :active :path "/tmp/a"}
                                           {:slug "b" :status :paused :path "/tmp/b"}]}
                     :configs {}
                     :iterations {}
                     :open-beads {}
                     :ready-beads {}
                     :workers {}
                     :zombies []
                     :tick-result {:action "idle" :reason "no-active-iterations" :disable-cron true}}
                    "2026-02-27T16:00:00")
            text (apply str lines)]
        (should-contain "Registry: 2 projects" text)))

    (it "logs per-project details with iteration and bead statuses"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "proj1" :status :active :path "/tmp/p1"}]}
                     :configs {"proj1" {:status :active :max-workers 2}}
                     :iterations {"proj1" "005"}
                     :open-beads {"proj1" [{:id "proj1-x1" :status "open" :title "Task A"}
                                           {:id "proj1-x2" :status "blocked" :title "Task B"}]}
                     :ready-beads {"proj1" [{:id "proj1-x1"}]}
                     :workers {"proj1" 1}
                     :zombies []
                     :tick-result {:action "spawn" :spawns [{:project "proj1" :bead "proj1-x1"}]}}
                    "2026-02-27T16:00:00")
            text (clojure.string/join "\n" lines)]
        (should-contain "proj1" text)
        (should-contain "iteration=005" text)
        (should-contain "workers=1/2" text)
        (should-contain "proj1-x1" text)
        (should-contain "proj1-x2" text)
        (should-contain "blocked" text)))

    (it "logs spawn decisions"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "proj1" :status :active :path "/tmp/p1"}]}
                     :configs {"proj1" {:status :active :max-workers 1}}
                     :iterations {"proj1" "005"}
                     :open-beads {"proj1" [{:id "proj1-x1" :status "open"}]}
                     :ready-beads {"proj1" [{:id "proj1-x1"}]}
                     :workers {"proj1" 0}
                     :zombies []
                     :tick-result {:action "spawn" :spawns [{:project "proj1" :bead "proj1-x1"}]}}
                    "2026-02-27T16:00:00")
            text (clojure.string/join "\n" lines)]
        (should-contain "Spawn: proj1-x1" text)))

    (it "logs idle decisions with reason and disable_cron"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "proj1" :status :active :path "/tmp/p1"}]}
                     :configs {"proj1" {:status :active :max-workers 1}}
                     :iterations {"proj1" "005"}
                     :open-beads {"proj1" []}
                     :ready-beads {"proj1" []}
                     :workers {"proj1" 0}
                     :zombies []
                     :tick-result {:action "idle" :reason "no-ready-beads" :disable-cron true}}
                    "2026-02-27T16:00:00")
            text (clojure.string/join "\n" lines)]
        (should-contain "Decision: idle (no-ready-beads)" text)
        (should-contain "disable_cron=true" text)))

    (it "logs zombie detections"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "proj1" :status :active :path "/tmp/p1"}]}
                     :configs {"proj1" {:status :active :max-workers 1}}
                     :iterations {"proj1" "005"}
                     :open-beads {"proj1" []}
                     :ready-beads {"proj1" []}
                     :workers {"proj1" 0}
                     :zombies [{:slug "proj1" :bead "proj1-old" :label "project:proj1:proj1-old" :reason "bead-closed"}]
                     :tick-result {:action "idle" :reason "no-ready-beads" :disable-cron true}}
                    "2026-02-27T16:00:00")
            text (clojure.string/join "\n" lines)]
        (should-contain "Zombie: proj1-old (bead-closed)" text)))

    (it "logs project with no active iteration"
      (let [lines (log/format-log-lines
                    {:registry {:projects [{:slug "proj1" :status :active :path "/tmp/p1"}]}
                     :configs {"proj1" {:status :active}}
                     :iterations {}
                     :open-beads {}
                     :ready-beads {}
                     :workers {}
                     :zombies []
                     :tick-result {:action "idle" :reason "no-active-iterations" :disable-cron true}}
                    "2026-02-27T16:00:00")
            text (clojure.string/join "\n" lines)]
        (should-contain "proj1" text)
        (should-contain "no active iteration" text))))

  (describe "write-log!"

    (it "appends to the specified log file"
      (let [log-file (str "/tmp/braids-test-" (System/currentTimeMillis) ".log")]
        (log/write-log! log-file ["line1" "line2" "line3"])
        (let [content (slurp log-file)]
          (should-contain "line1\nline2\nline3\n" content))
        (log/write-log! log-file ["line4"])
        (let [content (slurp log-file)]
          (should-contain "line4" content)
          (should-contain "line1" content))
        (clojure.java.io/delete-file log-file true)))))
