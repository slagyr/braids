(ns braids.config-io-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [braids.config :as config]
            [braids.config-io :as config-io]))

(describe "braids.config-io"

  (with-all tmp-dir (str (fs/create-temp-dir {:prefix "braids-config-test"})))

  (describe "load-config"
    (it "returns defaults when file doesn't exist"
      (should= config/defaults
               (config-io/load-config (str @tmp-dir "/nonexistent.edn"))))

    (it "reads config from file"
      (let [path (str @tmp-dir "/config.edn")]
        (spit path "{:braids-home \"/custom\"}")
        (should= {:braids-home "/custom" :orchestrator-channel nil}
                 (config-io/load-config path)))))

  (describe "save-config!"
    (it "writes config to file"
      (let [path (str @tmp-dir "/save-test.edn")
            cfg {:braids-home "/my/projects" :orchestrator-channel nil}]
        (config-io/save-config! cfg path)
        (should (fs/exists? path))
        (should= cfg (config-io/load-config path)))))

  (describe "resolve-braids-home"
    (it "resolves from config file"
      (let [path (str @tmp-dir "/resolve-test.edn")]
        (spit path "{:braids-home \"/absolute/path\"}")
        (should= "/absolute/path"
                 (config-io/resolve-braids-home path))))

    (it "expands tilde in braids-home"
      (let [path (str @tmp-dir "/tilde-test.edn")
            home (System/getProperty "user.home")]
        (spit path "{:braids-home \"~/Projects\"}")
        (should= (str home "/Projects")
                 (config-io/resolve-braids-home path))))))
