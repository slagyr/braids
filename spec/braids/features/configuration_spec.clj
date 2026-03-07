(ns braids.features.configuration-spec
  (:require [speclj.core :refer :all]))

(describe "Configuration"

  (context "Config list shows all keys sorted alphabetically"
    (it "Config list shows all keys sorted alphabetically"
      ;; Given a config with values:
      ;; When listing the config
      ;; Then the output should contain "bd-bin = bd"
      ;; And the output should contain "braids-home = ~/Projects"
      ;; And the output should contain "openclaw-bin = openclaw"
      ;; And "bd-bin" should appear before "braids-home" in the output
      ;; And "braids-home" should appear before "openclaw-bin" in the output
      (pending "not yet implemented")))

  (context "Config get returns value for existing key"
    (it "Config get returns value for existing key"
      ;; Given a config with values:
      ;; When getting config key "braids-home"
      ;; Then the result should be ok with value "/custom/path"
      (pending "not yet implemented")))

  (context "Config get returns error for missing key"
    (it "Config get returns error for missing key"
      ;; Given a config with values:
      ;; When getting config key "nonexistent"
      ;; Then the result should be an error
      ;; And the error message should contain "nonexistent"
      ;; And the error message should contain "not found"
      (pending "not yet implemented")))

  (context "Config set updates value"
    (it "Config set updates value"
      ;; Given a config with values:
      ;; When setting config key "braids-home" to "/new/path"
      ;; Then the config should have "braids-home" set to "/new/path"
      (pending "not yet implemented")))

  (context "Config defaults applied on parse"
    (it "Config defaults applied on parse"
      ;; Given an empty config string
      ;; When parsing the config
      ;; Then the config should have "braids-home" set to "~/Projects"
      ;; And the config should have "bd-bin" set to "bd"
      ;; And the config should have "openclaw-bin" set to "openclaw"
      (pending "not yet implemented")))

  (context "Config help output"
    (it "Config help output"
      ;; When requesting config help
      ;; Then the output should contain "Usage: braids config"
      ;; And the output should contain "list"
      ;; And the output should contain "get <key>"
      ;; And the output should contain "set <key> <val>"
      (pending "not yet implemented"))))
