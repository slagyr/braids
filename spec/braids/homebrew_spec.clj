(ns braids.homebrew-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def project-root (str (System/getProperty "user.dir")))
(def formula-path (str project-root "/Formula/braids.rb"))
(def formula-content (when (fs/exists? formula-path) (slurp formula-path)))
(def readme-path (str project-root "/README.md"))
(def readme-content (when (fs/exists? readme-path) (slurp readme-path)))

(describe "Homebrew formula"

  (it "exists at Formula/braids.rb"
    (should (fs/exists? formula-path)))

  (it "declares the correct class name"
    (should-contain "class Braids < Formula" formula-content))

  (it "has a description"
    (should-contain "desc " formula-content))

  (it "references the braids repo"
    (should-contain "slagyr/braids" formula-content))

  (it "supports HEAD install from main branch"
    (should-contain "head " formula-content)
    (should-contain "branch: \"main\"" formula-content))

  (it "depends on babashka"
    (should-contain "borkdude/brew/babashka" formula-content))

  (it "depends on beads (bd)"
    (should-contain "beads" formula-content))

  (it "installs a braids bin wrapper"
    (should-contain "bin/\"braids\"" formula-content))

  (it "wrapper invokes bb braids"
    (should-contain "bb" formula-content)
    (should-contain "braids" formula-content))

  (it "has a test block"
    (should-contain "test do" formula-content)))

(describe "README brew install"

  (it "has brew install as primary install method"
    (should (re-find #"brew install" readme-content)))

  (it "mentions slagyr/tap/braids"
    (should-contain "slagyr/tap/braids" readme-content))

  (it "still has manual install steps"
    (should (re-find #"install\.sh" readme-content))))
