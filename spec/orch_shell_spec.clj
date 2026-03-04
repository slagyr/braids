(ns orch-shell-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def script-path (str (fs/canonicalize "braids/bin/braids-orch.sh")))

(describe "braids-orch.sh"

  (it "exists and is executable"
    (should (fs/exists? script-path))
    (should (fs/executable? script-path)))

  (it "has a proper shebang"
    (should (str/starts-with? (slurp script-path) "#!/usr/bin/env bash")))

  (it "supports --dry-run flag"
    (let [result (proc/shell {:out :string :err :string :continue true}
                             script-path "--dry-run")]
      ;; Should not crash with --dry-run (may idle if no beads)
      (should (contains? #{0 1} (:exit result)))))

  (it "rejects unknown arguments"
    (let [result (proc/shell {:out :string :err :string :continue true}
                             script-path "--bogus")]
      (should= 1 (:exit result))
      (should (str/includes? (:err result) "Unknown arg"))))

  (describe "with mock braids and openclaw"
    ;; Create a temp directory with mock braids and openclaw scripts
    ;; to simulate spawn and idle scenarios without real CLI calls.

    (with-all tmp-dir (str (fs/create-temp-dir {:prefix "braids-orch-test-"})))
    (with-all log-file (str @tmp-dir "/test-orch.log"))

    (after-all
      (when (fs/exists? @tmp-dir)
        (fs/delete-tree @tmp-dir)))

    (it "handles spawn action correctly in dry-run mode"
      (let [mock-bin (str @tmp-dir "/bin")
            _ (fs/create-dirs mock-bin)
            ;; Mock braids that returns spawn JSON
            spawn-json (json/generate-string
                         {:action "spawn"
                          :spawns [{:bead "test-abc"
                                    :path "~/Projects/test"
                                    :iteration "001"
                                    :channel "123456"
                                    :runTimeoutSeconds 1800
                                    :agentId "scrapper"
                                    :thinking "low"
                                    :label "project:test:test-abc"}]})
            _ (spit (str mock-bin "/braids")
                    (str "#!/bin/bash\necho '" spawn-json "'"))
            _ (fs/set-posix-file-permissions (str mock-bin "/braids") "rwxr-xr-x")
            result (proc/shell {:out :string :err :string :continue true
                                :env {"PATH" (str mock-bin ":" (System/getenv "PATH"))
                                      "BRAIDS_ORCH_LOG" @log-file}}
                               script-path "--dry-run")
            log-content (when (fs/exists? @log-file) (slurp @log-file))]
        (should= 0 (:exit result))
        (should-not-be-nil log-content)
        (should (str/includes? log-content "action=spawn"))
        (should (str/includes? log-content "Spawning 1 worker"))
        (should (str/includes? log-content "DRY-RUN: would spawn openclaw agent for test-abc"))))

    (it "handles idle action correctly"
      (let [mock-bin (str @tmp-dir "/bin-idle")
            _ (fs/create-dirs mock-bin)
            idle-json (json/generate-string
                        {:action "idle"
                         :reason "all-at-capacity"
                         :disable_cron false})
            _ (spit (str mock-bin "/braids")
                    (str "#!/bin/bash\necho '" idle-json "'"))
            _ (fs/set-posix-file-permissions (str mock-bin "/braids") "rwxr-xr-x")
            idle-log (str @tmp-dir "/idle.log")
            result (proc/shell {:out :string :err :string :continue true
                                :env {"PATH" (str mock-bin ":" (System/getenv "PATH"))
                                      "BRAIDS_ORCH_LOG" idle-log}}
                               script-path "--dry-run")
            log-content (when (fs/exists? idle-log) (slurp idle-log))]
        (should= 0 (:exit result))
        (should (str/includes? log-content "action=idle"))
        (should (str/includes? log-content "reason=all-at-capacity"))))

    (it "handles idle with disable_cron in dry-run mode"
      (let [mock-bin (str @tmp-dir "/bin-disable")
            _ (fs/create-dirs mock-bin)
            idle-json (json/generate-string
                        {:action "idle"
                         :reason "no-ready-beads"
                         :disable_cron true})
            _ (spit (str mock-bin "/braids")
                    (str "#!/bin/bash\necho '" idle-json "'"))
            _ (fs/set-posix-file-permissions (str mock-bin "/braids") "rwxr-xr-x")
            ;; Mock openclaw that returns a cron list
            _ (spit (str mock-bin "/openclaw")
                    (str "#!/bin/bash\n"
                         "if [ \"$1\" = \"cron\" ] && [ \"$2\" = \"list\" ]; then\n"
                         "  echo '{\"jobs\":[{\"id\":\"test-id\",\"name\":\"braids-orchestrator\"}]}'\n"
                         "elif [ \"$1\" = \"cron\" ] && [ \"$2\" = \"disable\" ]; then\n"
                         "  echo 'disabled'\n"
                         "fi\n"))
            _ (fs/set-posix-file-permissions (str mock-bin "/openclaw") "rwxr-xr-x")
            disable-log (str @tmp-dir "/disable.log")
            result (proc/shell {:out :string :err :string :continue true
                                :env {"PATH" (str mock-bin ":" (System/getenv "PATH"))
                                      "BRAIDS_ORCH_LOG" disable-log}}
                               script-path "--dry-run")
            log-content (when (fs/exists? disable-log) (slurp disable-log))]
        (should= 0 (:exit result))
        (should (str/includes? log-content "disable_cron=true"))
        (should (str/includes? log-content "DRY-RUN: would disable cron test-id"))))

    (it "handles zombies in output"
      (let [mock-bin (str @tmp-dir "/bin-zombie")
            _ (fs/create-dirs mock-bin)
            zombie-json (json/generate-string
                          {:action "idle"
                           :reason "all-at-capacity"
                           :disable_cron false
                           :zombies [{:slug "myproj"
                                      :bead "myproj-xyz"
                                      :label "project:myproj:myproj-xyz"
                                      :reason "bead-closed"}]})
            _ (spit (str mock-bin "/braids")
                    (str "#!/bin/bash\necho '" zombie-json "'"))
            _ (fs/set-posix-file-permissions (str mock-bin "/braids") "rwxr-xr-x")
            zombie-log (str @tmp-dir "/zombie.log")
            result (proc/shell {:out :string :err :string :continue true
                                :env {"PATH" (str mock-bin ":" (System/getenv "PATH"))
                                      "BRAIDS_ORCH_LOG" zombie-log}}
                               script-path "--dry-run")
            log-content (when (fs/exists? zombie-log) (slurp zombie-log))]
        (should= 0 (:exit result))
        (should (str/includes? log-content "1 zombie"))
        (should (str/includes? log-content "myproj-xyz"))))))
