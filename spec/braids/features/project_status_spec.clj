(ns braids.features.project-status-spec
  (:require [speclj.core :refer :all]))

(describe "Project status"

  (context "Dashboard includes all projects with enriched data"
    (it "Dashboard includes all projects with enriched data"
      ;; Given a registry with projects:
      ;; And project configs:
      ;; And active iterations:
      ;; And active workers:
      ;; When building the dashboard
      ;; Then the dashboard should have 3 projects
      ;; And project "alpha" should have status "active"
      ;; And project "alpha" should have iteration number "009"
      ;; And project "alpha" should have workers 1 of 2
      ;; And project "beta" should have status "paused"
      ;; And project "beta" should have no iteration
      (pending "not yet implemented")))

  (context "Dashboard handles missing iterations"
    (it "Dashboard handles missing iterations"
      ;; Given a registry with projects:
      ;; And project configs:
      ;; And no active iterations
      ;; And no active workers
      ;; When building the dashboard
      ;; Then project "proj" should have no iteration
      (pending "not yet implemented")))

  (context "Project detail shows iteration progress and stories"
    (it "Project detail shows iteration progress and stories"
      ;; Given a dashboard project "alpha" with:
      ;; And project "alpha" has iteration:
      ;; And project "alpha" has stories:
      ;; When formatting project detail for "alpha"
      ;; Then the output should contain "alpha"
      ;; And the output should contain "1/3"
      ;; And the output should contain "33%"
      ;; And the output should contain "a-001"
      ;; And the output should contain "Do thing"
      (pending "not yet implemented")))

  (context "Project detail shows no-iteration fallback"
    (it "Project detail shows no-iteration fallback"
      ;; Given a dashboard project "beta" with:
      ;; And project "beta" has no iteration
      ;; When formatting project detail for "beta"
      ;; Then the output should contain "beta"
      ;; And the output should contain "no active iteration"
      (pending "not yet implemented")))

  (context "Dashboard JSON output includes all project data"
    (it "Dashboard JSON output includes all project data"
      ;; Given a registry with projects:
      ;; And project configs:
      ;; And active iterations:
      ;; And active workers:
      ;; When building the dashboard
      ;; And formatting the dashboard as JSON
      ;; Then the JSON should contain 1 project
      ;; And the JSON project "alpha" should have status "active"
      ;; And the JSON project "alpha" should have iteration percent 33
      (pending "not yet implemented")))

  (context "Dashboard handles empty registry"
    (it "Dashboard handles empty registry"
      ;; Given an empty registry
      ;; When building the dashboard
      ;; And formatting the dashboard
      ;; Then the output should be "No projects registered."
      (pending "not yet implemented"))))
