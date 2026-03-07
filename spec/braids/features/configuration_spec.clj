(ns braids.features.configuration-spec
  (:require [speclj.core :refer :all]
            [braids.features.harness :as h]))

(describe "Configuration"

  (context "Config list shows all keys sorted alphabetically"
    (it "Config list shows all keys sorted alphabetically"
      (h/reset!)
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "~/Projects"] ["orchestrator-channel" ""] ["env-path" ""] ["bd-bin" "bd"] ["openclaw-bin" "openclaw"]])
      (h/list-config!)
      (should (clojure.string/includes? (h/output) "bd-bin = bd"))
      (should (clojure.string/includes? (h/output) "braids-home = ~/Projects"))
      (should (clojure.string/includes? (h/output) "openclaw-bin = openclaw"))
      (should (< (clojure.string/index-of (h/output) "bd-bin")
                 (clojure.string/index-of (h/output) "braids-home")))
      (should (< (clojure.string/index-of (h/output) "braids-home")
                 (clojure.string/index-of (h/output) "openclaw-bin")))))

  (context "Config get returns value for existing key"
    (it "Config get returns value for existing key"
      (h/reset!)
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "/custom/path"]])
      (h/get-config-key! "braids-home")
      (should= "/custom/path" (:ok (h/config-result)))))

  (context "Config get returns error for missing key"
    (it "Config get returns error for missing key"
      (h/reset!)
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "~/Projects"]])
      (h/get-config-key! "nonexistent")
      (should (:error (h/config-result)))
      (should (clojure.string/includes? (:error (h/config-result)) "nonexistent"))
      (should (clojure.string/includes? (:error (h/config-result)) "not found"))))

  (context "Config set updates value"
    (it "Config set updates value"
      (h/reset!)
      (h/set-config-from-table
        ["key" "value"]
        [["braids-home" "~/Projects"]])
      (h/set-config-key! "braids-home" "/new/path")
      (should= "/new/path" (str (get (h/current-config) (keyword "braids-home"))))))

  (context "Config defaults applied on parse"
    (it "Config defaults applied on parse"
      (h/reset!)
      (h/set-empty-config)
      (h/parse-config!)
      (should= "~/Projects" (str (get (h/current-config) (keyword "braids-home"))))
      (should= "bd" (str (get (h/current-config) (keyword "bd-bin"))))
      (should= "openclaw" (str (get (h/current-config) (keyword "openclaw-bin"))))))

  (context "Config help output"
    (it "Config help output"
      (h/reset!)
      (h/request-config-help!)
      (should (clojure.string/includes? (h/output) "Usage: braids config"))
      (should (clojure.string/includes? (h/output) "list"))
      (should (clojure.string/includes? (h/output) "get <key>"))
      (should (clojure.string/includes? (h/output) "set <key> <val>")))))
