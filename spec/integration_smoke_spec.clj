(ns integration-smoke-spec
  (:require [speclj.core :refer :all]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [braids.orch :as orch]))

;; NOTE: All tests in this file are PENDING (integration smoke tests).
;; They read real registry state, shell out to `bd show`, `git log`, etc.
;; per registered project, which can hang or fail when tools are unavailable.
;; Move to a separate `bb test:integration` task when ready.
;; See: braids-kog
;;
;; Fixed top-level side effects:
;;   - Removed: conditional `describe` blocks based on (fs/exists? registry)
;;     at load time — these read real filesystem state during ns loading

;; ── Integration tests per project (pending) ──

(describe "Integration smoke tests"
  (xit "git state is clean for registered projects")
  (xit "iterations are valid for registered projects")
  (xit "at most one active iteration per project")
  (xit "no orphaned deliverables"))

;; ── Cross-project checks ──

(describe "Cross-Project Checks"
  (xit "orchestrator self-disable: orch-tick idle results include disable-cron"))
