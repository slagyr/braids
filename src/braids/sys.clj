(ns braids.sys
  "Subprocess environment helpers. Reads from global braids config.")

(defn subprocess-env
  "Returns env map for babashka.process :extra-env option.
   Merges :env-path from config into current PATH."
  [config]
  (let [extra (:env-path config)
        current (or (System/getenv "PATH") "/usr/bin:/bin")]
    (if extra
      {"PATH" (str extra ":" current)}
      {"PATH" current})))

(defn bd-bin
  "Returns bd binary name/path from config, defaulting to \"bd\"."
  [config]
  (or (:bd-bin config) "bd"))

(defn openclaw-bin
  "Returns openclaw binary name/path from config, defaulting to \"openclaw\"."
  [config]
  (or (:openclaw-bin config) "openclaw"))
