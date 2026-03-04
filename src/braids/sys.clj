(ns braids.sys
  "System environment helpers for subprocess invocation.
   Temporary: will be superseded by braids-vnm (config-driven PATH + binary paths).")

(def path-extras
  "Additional directories prepended to PATH for all subprocesses.
   Covers the common locations for bb, bd, openclaw, and node on macOS."
  "/usr/local/bin:/Users/zane/.local/bin")

(defn subprocess-env
  "Returns an env map suitable for babashka.process options.
   Merges path-extras into the current PATH so subprocesses find all required binaries."
  []
  (let [current-path (or (System/getenv "PATH") "/usr/bin:/bin")]
    {"PATH" (str path-extras ":" current-path)}))
