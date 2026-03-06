(ns braids.config-command-spec
  (:require [speclj.core :refer :all]
            [braids.config :as config]
            [braids.config-io :as config-io]
            [braids.core :as core]
            [babashka.fs :as fs]))

(describe "braids config command"

  (with-all tmp-dir (str (fs/create-temp-dir {:prefix "braids-config-cmd-test"})))
  (with-all config-path (str @tmp-dir "/config.edn"))

  (around [it]
    (with-redefs [config-io/default-config-path @config-path]
      (it)))

  (context "config list"
    (it "lists all config keys and values"
      (spit @config-path (pr-str {:braids-home "/my/projects"}))
      (should-contain "braids-home"
                      (with-out-str (core/run ["config" "list"]))))

    (it "shows defaults when no config file"
      (fs/delete-if-exists @config-path)
      (let [output (with-out-str (core/run ["config" "list"]))]
        (should-contain "braids-home" output)
        (should-contain "~/Projects" output))))

  (context "config get"
    (it "gets a specific key"
      (spit @config-path (pr-str {:braids-home "/custom/path"}))
      (should-contain "/custom/path"
                      (with-out-str (core/run ["config" "get" "braids-home"]))))

    (it "shows default when key not explicitly set"
      (spit @config-path (pr-str {}))
      (should-contain "~/Projects"
                      (with-out-str (core/run ["config" "get" "braids-home"]))))

    (it "shows error for missing key"
      (spit @config-path (pr-str {}))
      (should-contain "not found"
                      (with-out-str (core/run ["config" "get" "nonexistent"]))))

    (it "returns exit code 1 for missing key"
      (spit @config-path (pr-str {}))
      (let [exit (atom nil)]
        (with-out-str (reset! exit (core/run ["config" "get" "nonexistent"])))
        (should= 1 @exit)))

    (it "prints usage when no key given"
      (should-contain "Usage"
                      (with-out-str (core/run ["config" "get"])))))

  (context "config set"
    (it "sets a key value"
      (spit @config-path (pr-str {:braids-home "~/Projects"}))
      (with-out-str (core/run ["config" "set" "braids-home" "/new/path"]))
      (should= "/new/path" (:braids-home (config-io/load-config @config-path))))

    (it "adds a new key"
      (spit @config-path (pr-str {:braids-home "~/Projects"}))
      (with-out-str (core/run ["config" "set" "custom-key" "custom-val"]))
      (should= "custom-val" (:custom-key (config-io/load-config @config-path))))

    (it "prints usage when missing args"
      (should-contain "Usage"
                      (with-out-str (core/run ["config" "set"])))))

  (context "config (no subcommand)"
    (it "shows config help"
      (should-contain "config get"
                      (with-out-str (core/run ["config"]))))))
