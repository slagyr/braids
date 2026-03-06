(ns braids.structural-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]))

;; NOTE: All tests in this file are PENDING (structural/integration tests).
;; They read real registry/project state, shell out to `bd`, `openclaw`,
;; and `git`, which can hang or fail when tools are unavailable.
;; Move to a separate `bb test:integration` task when ready.
;; See: braids-kog

(def project-root (System/getProperty "user.dir"))
(def skill-dir (str project-root "/braids"))

;; ── Legacy tests/ directory removed ──

(describe "Legacy tests/ directory"
  (xit "tests/ directory does not exist (replaced by spec/ with speclj)"))

;; ── Skill Symlink ──

(describe "Skill Symlink"
  (xit "symlink exists (skipped if OpenClaw not installed)")
  (xit "symlink target is valid directory (skipped if OpenClaw not installed)")
  (xit "symlink points to projects-skill/braids (skipped if OpenClaw not installed)"))

;; ── Skill Directory ──

(describe "Skill Directory"
  (xit "SKILL.md exists")
  (xit "references/ directory exists")
  (xit "all reference files exist"))

;; ── SKILL.md Format ──

(describe "SKILL.md Format"
  (xit "has YAML frontmatter")
  (xit "frontmatter has name field")
  (xit "frontmatter has description field"))

;; ── Registry ──

(describe "Registry"
  (xit "registry.edn exists (skipped if no registry)")
  (xit "is valid EDN with :projects key (skipped if no registry)")
  (xit "all registered projects are valid (skipped if no registry)"))

;; ── Spawn Config ──

(describe "Spawn Config"
  (xit "orchestrator cron job exists")
  (xit "active projects have valid spawn config (skipped if no registry)"))
