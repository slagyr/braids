(ns braids.config-spec
  (:require [speclj.core :refer :all]
            [braids.config :as config]))

(describe "braids.config"

  (context "parse-config"
    (it "parses a config EDN string"
      (should= {:braids-home "/custom/path" :orchestrator-channel nil
                :env-path nil :bd-bin "bd" :openclaw-bin "openclaw"}
               (config/parse-config "{:braids-home \"/custom/path\"}")))

    (it "applies defaults for missing keys"
      (should= {:braids-home "~/Projects" :orchestrator-channel nil
                :env-path nil :bd-bin "bd" :openclaw-bin "openclaw"}
               (config/parse-config "{}")))

    (it "preserves extra keys"
      (should= {:braids-home "~/Projects" :orchestrator-channel nil
                :env-path nil :bd-bin "bd" :openclaw-bin "openclaw" :extra "val"}
               (config/parse-config "{:extra \"val\"}"))))

  (context "serialize-config"
    (it "round-trips through parse"
      (let [cfg {:braids-home "/foo/bar" :orchestrator-channel nil
                 :env-path nil :bd-bin "bd" :openclaw-bin "openclaw"}]
        (should= cfg (config/parse-config (config/serialize-config cfg)))))))
