(ns install-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (str (System/getProperty "user.dir")))
(def readme-path (str project-root "/README.md"))
(def install-path (str project-root "/install.sh"))
(def readme (when (fs/exists? readme-path) (slurp readme-path)))
(def install-script (when (fs/exists? install-path) (slurp install-path)))

(describe "Install"

  (describe "README.md"

    (it "has a one-line install command using curl"
      (should-contain "curl" readme)
      (should-contain "install.sh" readme))

    (it "has only one command in the install code block"
      (let [lines (str/split-lines readme)
            in-block (atom false)
            block-lines (atom [])]
        ;; find the first code block after ## Install
        (doseq [line lines]
          (when (and (not @in-block) (str/starts-with? line "```bash"))
            (reset! in-block true))
          (when (and @in-block (not (str/starts-with? line "```")))
            (swap! block-lines conj (str/trim line)))
          (when (and @in-block (str/starts-with? line "```") (pos? (count @block-lines)))
            (reset! in-block false)))
        (let [non-empty (filter #(not (str/blank? %)) @block-lines)]
          (should= 1 (count non-empty))))))

  (describe "install.sh"

    (it "exists and is executable"
      (should (fs/exists? install-path))
      (should (fs/executable? install-path)))

    (it "clones the repo"
      (should-contain "git clone" install-script))

    (it "creates the skill symlink"
      (should-contain "ln -s" install-script))

    (it "uses set -euo pipefail"
      (should-contain "set -euo pipefail" install-script))

    (it "supports BRAIDS_INSTALL_DIR override"
      (should-contain "BRAIDS_INSTALL_DIR" install-script))))
