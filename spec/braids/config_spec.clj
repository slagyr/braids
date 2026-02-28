(ns braids.config-spec
  (:require [speclj.core :refer :all]
            [braids.config :as config]))

(describe "braids.config"

  (describe "parse-config"
    (it "parses a config EDN string"
      (should= {:braids-home "/custom/path" :orchestrator-channel nil}
               (config/parse-config "{:braids-home \"/custom/path\"}")))

    (it "applies defaults for missing keys"
      (should= {:braids-home "~/Projects" :orchestrator-channel nil}
               (config/parse-config "{}")))

    (it "preserves extra keys"
      (should= {:braids-home "~/Projects" :orchestrator-channel nil :extra "val"}
               (config/parse-config "{:extra \"val\"}"))))

  (describe "serialize-config"
    (it "round-trips through parse"
      (let [cfg {:braids-home "/foo/bar" :orchestrator-channel nil}]
        (should= cfg (config/parse-config (config/serialize-config cfg)))))))
