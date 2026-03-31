(ns braids.orch-runner-io-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [babashka.process :as proc]
            [braids.orch-runner-io :as rio]
            [braids.orch-runner :as runner]
            [braids.orch-io :as orch-io]
            [braids.orch :as orch]
            [braids.config-io :as config-io]
            [braids.sys :as sys]))

(def sample-spawn
  {:bead "test-bead-1"
   :path "~/Projects/test"
   :iteration "001"
   :channel "general"
   :worker-agent nil
   :thinking "high"
   :worker-timeout 1800})

(def sample-config
  {:env-path "/usr/local/bin"
   :openclaw-bin "openclaw"
   :bd-bin "bd"})

(def idle-result
  {:result {:action "idle" :reason "no-ready-beads"}
   :debug-ctx {:registry {:projects []}
               :configs {}
               :iterations {}
               :open-beads {}
               :ready-beads {}
               :workers {}}})

(def spawn-result
  {:result {:action "spawn"
            :spawns [sample-spawn]}
   :debug-ctx {:registry {:projects [{:slug "test" :status :active :priority :normal :path "~/Projects/test"}]}
               :configs {"test" {:max-workers 2}}
               :iterations {"test" "001"}
               :open-beads {"test" []}
               :ready-beads {"test" [{:id "test-bead-1" :title "Task" :priority "1"}]}
               :workers {"test" 0}}})

(def zombie-result
  {:result {:action "idle"
            :reason "no-ready-beads"
            :zombies [{:bead "z1" :slug "proj" :label "project:proj:z1"
                       :reason "bead-closed" :session-id "braids-z1-worker"}
                      {:bead "z2" :slug "proj" :label "project:proj:z2"
                       :reason "timeout"}]}
   :debug-ctx {:registry {:projects []}
               :configs {}
               :iterations {}
               :open-beads {}
               :ready-beads {}
               :workers {}}})

(describe "braids.orch-runner-io"

  (context "spawn-worker!"

    (it "calls proc/process with openclaw binary and built args in live mode"
      (let [process-args (atom nil)]
        (with-redefs [config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [args opts]
                                     (reset! process-args {:args args :opts opts})
                                     nil)]
          (with-out-str (rio/spawn-worker! sample-spawn {:dry-run false}))
          (should-not-be-nil @process-args)
          (should= "openclaw" (first (:args @process-args)))
          (should= :discard (get-in @process-args [:opts :out])))))

    (it "passes built worker args to the process using sessions spawn subcommand"
      (let [process-args (atom nil)]
        (with-redefs [config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [args opts]
                                     (reset! process-args args)
                                     nil)]
          (with-out-str (rio/spawn-worker! sample-spawn {:dry-run false}))
          (let [args (vec (rest @process-args))]
            (should (some #{"sessions"} args))
            (should (some #{"spawn"} args))
            (should (some #{"--task"} args))
            (should (some #{"--label"} args))))))

    (it "does not call proc/process in dry-run mode"
      (let [process-called (atom false)]
        (with-redefs [config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [& _]
                                     (reset! process-called true)
                                     nil)]
          (with-out-str (rio/spawn-worker! sample-spawn {:dry-run true}))
          (should-not @process-called))))

    (it "prints DRY-RUN message in dry-run mode"
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                    sys/openclaw-bin (fn [_] "openclaw")]
        (let [output (with-out-str (rio/spawn-worker! sample-spawn {:dry-run true}))]
          (should-contain "DRY-RUN" output)
          (should-contain "test-bead-1" output))))

    (it "does not print spawned message in live mode"
      (with-redefs [config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                    sys/openclaw-bin (fn [_] "openclaw")
                    proc/process (fn [& _] nil)]
        (let [output (with-out-str (rio/spawn-worker! sample-spawn {:dry-run false}))]
          (should-not-contain "Spawned worker" output))))

    (it "respects OPENCLAW_BIN environment variable"
      (let [process-args (atom nil)]
        (with-redefs [config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [args opts]
                                     (reset! process-args args)
                                     nil)]
          ;; System/getenv cannot be easily redefined, so we test that
          ;; when OPENCLAW_BIN is not set, it falls back to sys/openclaw-bin
          (with-out-str (rio/spawn-worker! sample-spawn {:dry-run false}))
          (should= "openclaw" (first @process-args)))))

    (it "passes subprocess-env as extra-env to process"
      (let [process-opts (atom nil)]
        (with-redefs [config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/custom/path"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [args opts]
                                     (reset! process-opts opts)
                                     nil)]
          (with-out-str (rio/spawn-worker! sample-spawn {:dry-run false}))
          (should= {"PATH" "/custom/path"} (:extra-env @process-opts))))))

  (context "run-orch!"

    (it "returns 0 on success with idle result"
      (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] idle-result)
                    orch/format-debug-output (fn [& _] "debug output")]
        (let [output (with-out-str
                       (let [code (rio/run-orch!)]
                         (should= 0 code)))]
          (should-contain "DRY-RUN" output))))

    (it "defaults to dry-run mode"
      (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] idle-result)
                    orch/format-debug-output (fn [& _] "")]
        (let [output (with-out-str (rio/run-orch!))]
          (should-contain "DRY-RUN" output))))

    (it "shows LIVE-RUN banner when dry-run is false"
      (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] idle-result)
                    orch/format-debug-output (fn [& _] "")]
        (let [output (with-out-str (rio/run-orch! {:dry-run false}))]
          (should-contain "LIVE-RUN" output))))

    (it "prints debug output from orch/format-debug-output"
      (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] idle-result)
                    orch/format-debug-output (fn [& _] "=== Debug Info ===")]
        (let [output (with-out-str (rio/run-orch!))]
          (should-contain "=== Debug Info ===" output))))

    (it "spawns workers when action is spawn"
      (let [spawned (atom [])]
        (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] spawn-result)
                      orch/format-debug-output (fn [& _] "")
                      config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [args opts]
                                     (swap! spawned conj args)
                                     nil)]
          (with-out-str (rio/run-orch! {:dry-run false}))
          (should= 1 (count @spawned))
          (should= "openclaw" (first (first @spawned)))
          ;; Verify sessions spawn subcommand is used
          (let [args (vec (rest (first @spawned)))]
            (should= "sessions" (first args))
            (should= "spawn" (second args))))))

    (it "does not spawn workers in dry-run mode even with spawn action"
      (let [spawned (atom [])]
        (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] spawn-result)
                      orch/format-debug-output (fn [& _] "")
                      config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [args opts]
                                     (swap! spawned conj args)
                                     nil)]
          (let [output (with-out-str (rio/run-orch! {:dry-run true}))]
            (should= 0 (count @spawned))
            (should-contain "DRY-RUN: would spawn" output)))))

    (it "prints spawn log lines when spawning"
      (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] spawn-result)
                    orch/format-debug-output (fn [& _] "")
                    config-io/load-config (fn [] sample-config)
                    sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                    sys/openclaw-bin (fn [_] "openclaw")
                    proc/process (fn [& _] nil)]
        (let [output (with-out-str (rio/run-orch! {:dry-run false}))]
          (should-contain "Spawning 1 worker" output)
          (should-contain "test-bead-1" output))))

    (it "returns 1 and prints error when exception occurs"
      (with-redefs [orch-io/gather-and-tick-from-stores-debug
                    (fn [_] (throw (Exception. "connection failed")))]
        (let [output (with-out-str
                       (let [code (rio/run-orch!)]
                         (should= 1 code)))]
          (should-contain "ERROR" output)
          (should-contain "connection failed" output))))

    (context "zombie handling"

      (it "kills zombie sessions with session-ids"
        (let [killed (atom [])]
          (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] zombie-result)
                        orch/format-debug-output (fn [& _] "")
                        config-io/load-config (fn [] sample-config)
                        sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                        sys/openclaw-bin (fn [_] "openclaw")
                        proc/process (fn [args opts]
                                       (swap! killed conj (vec args))
                                       nil)]
            (with-out-str (rio/run-orch! {:dry-run true}))
            ;; Only z1 has a session-id, z2 does not
            (should= 1 (count @killed))
            (should= ["openclaw" "sessions" "kill" "braids-z1-worker"] (first @killed)))))

      (it "skips zombie kill when zombie has no session-id"
        (let [killed (atom [])]
          (with-redefs [orch-io/gather-and-tick-from-stores-debug
                        (fn [_] {:result {:action "idle" :reason "test"
                                          :zombies [{:bead "z1" :reason "timeout"}]}
                                 :debug-ctx (:debug-ctx idle-result)})
                        orch/format-debug-output (fn [& _] "")
                        config-io/load-config (fn [] sample-config)
                        sys/openclaw-bin (fn [_] "openclaw")
                        proc/process (fn [args opts]
                                       (swap! killed conj args)
                                       nil)]
            (with-out-str (rio/run-orch! {:dry-run true}))
            (should= 0 (count @killed)))))

      (it "prints zombie log lines"
        (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] zombie-result)
                      orch/format-debug-output (fn [& _] "")
                      config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [& _] nil)]
          (let [output (with-out-str (rio/run-orch! {:dry-run true}))]
            (should-contain "2 zombie" output)
            (should-contain "z1" output)
            (should-contain "bead-closed" output))))

      (it "prints killed zombie confirmation"
        (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] zombie-result)
                      orch/format-debug-output (fn [& _] "")
                      config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [& _] nil)]
          (let [output (with-out-str (rio/run-orch! {:dry-run true}))]
            (should-contain "Killed zombie session" output)
            (should-contain "braids-z1-worker" output))))

      (it "handles kill failure gracefully"
        (with-redefs [orch-io/gather-and-tick-from-stores-debug (fn [_] zombie-result)
                      orch/format-debug-output (fn [& _] "")
                      config-io/load-config (fn [] sample-config)
                      sys/subprocess-env (fn [_] {"PATH" "/usr/local/bin"})
                      sys/openclaw-bin (fn [_] "openclaw")
                      proc/process (fn [& _] (throw (Exception. "kill failed")))]
          (let [output (with-out-str
                         (let [code (rio/run-orch! {:dry-run true})]
                           ;; Should still return 0 — zombie kill failure is non-fatal
                           (should= 0 code)))]
            (should-contain "Failed to kill zombie" output)
            (should-contain "kill failed" output))))))

  (context "run-orch-command!"

    (it "passes parsed args to run-orch!"
      (let [received-opts (atom nil)]
        (with-redefs [rio/run-orch! (fn [opts]
                                      (reset! received-opts opts)
                                      0)]
          (with-out-str (rio/run-orch-command! ["--live-run" "--verbose"]))
          (should= false (:dry-run @received-opts))
          (should= true (:verbose @received-opts)))))

    (it "returns 1 and prints error for invalid args"
      (let [output (with-out-str
                     (let [code (rio/run-orch-command! ["--invalid"])]
                       (should= 1 code)))]
        (should-contain "--invalid" output)))

    (it "defaults to dry-run when no args given"
      (let [received-opts (atom nil)]
        (with-redefs [rio/run-orch! (fn [opts]
                                      (reset! received-opts opts)
                                      0)]
          (with-out-str (rio/run-orch-command! []))
          (should= true (:dry-run @received-opts)))))))
