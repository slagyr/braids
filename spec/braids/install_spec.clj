(ns braids.install-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (str (System/getProperty "user.dir")))
(def install-script (str project-root "/install.sh"))
(def readme-path (str project-root "/README.md"))
(def install-content (when (fs/exists? install-script) (slurp install-script)))
(def readme-content (when (fs/exists? readme-path) (slurp readme-path)))

(describe "install.sh"

  (it "exists"
    (should (fs/exists? install-script)))

  (it "is executable"
    (should (fs/executable? install-script)))

  (it "has a bash shebang"
    (should (str/starts-with? install-content "#!/usr/bin/env bash")))

  (it "uses set -euo pipefail"
    (should-contain "set -euo pipefail" install-content))

  (it "clones the correct repo"
    (should-contain "slagyr/braids" install-content))

  (it "creates the skill symlink"
    (should-contain ".openclaw/skills/braids" install-content))

  (it "supports updating an existing installation"
    (should-contain "pull" install-content)))

(describe "README install section"

  (it "has a one-liner install command"
    (should (re-find #"bash.*<.*curl.*install\.sh" readme-content)))

  (it "references the raw GitHub URL"
    (should (re-find #"raw\.githubusercontent\.com" readme-content))))
