(ns braids.list-spec
  (:require [speclj.core :refer :all]
            [braids.list :as list]
            [clojure.string :as str]))

(def sample-projects
  [{:slug "alpha" :status :active :priority :high :path "~/Projects/alpha"
    :iteration {:number "009" :stats {:total 3 :closed 1 :percent 33}}
    :workers 1 :max-workers 2}
   {:slug "beta" :status :paused :priority :normal :path "~/Projects/beta"
    :iteration nil :workers 0 :max-workers 1}
   {:slug "gamma" :status :active :priority :low :path "~/Projects/gamma"
    :iteration {:number "002" :stats {:total 2 :closed 2 :percent 100}}
    :workers 0 :max-workers 1}])

(defn strip-ansi [s]
  (str/replace s #"\033\[[0-9;]*m" ""))

(describe "braids.list"

  (context "format-list"

    (it "returns 'No projects registered.' for empty projects"
      (should= "No projects registered." (list/format-list {:projects []})))

    (it "returns 'No projects registered.' for nil projects"
      (should= "No projects registered." (list/format-list {:projects nil})))

    (it "includes all column headers"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))]
        (should-contain "SLUG" output)
        (should-contain "STATUS" output)
        (should-contain "PRIORITY" output)
        (should-contain "ITERATION" output)
        (should-contain "PROGRESS" output)
        (should-contain "WORKERS" output)
        (should-contain "PATH" output)))

    (it "shows all project slugs"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))]
        (should-contain "alpha" output)
        (should-contain "beta" output)
        (should-contain "gamma" output)))

    (it "shows iteration numbers"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))]
        (should-contain "009" output)
        (should-contain "002" output)))

    (it "shows progress percentages"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))]
        (should-contain "1/3 (33%)" output)
        (should-contain "2/2 (100%)" output)))

    (it "shows worker counts"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))]
        (should-contain "1/2" output)
        (should-contain "0/1" output)))

    (it "shows dash for projects without iteration"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))]
        ;; beta has no iteration - should show dash
        (let [lines (str/split-lines output)
              beta-line (first (filter #(str/includes? % "beta") lines))]
          (should-not-be-nil beta-line)
          ;; Should have dashes for iteration and progress
          (should-contain "—" beta-line))))

    (it "has correct number of lines (header + separator + data rows)"
      (let [output (strip-ansi (list/format-list {:projects sample-projects}))
            lines (str/split-lines output)]
        (should= 5 (count lines)))) ;; header + separator + 3 rows

    (it "colorizes active status as green"
      (let [output (list/format-list {:projects [(first sample-projects)]})]
        ;; green ANSI code is \033[32m
        (should-contain "\033[32m" output)))

    (it "colorizes high priority as red"
      (let [output (list/format-list {:projects [(first sample-projects)]})]
        ;; red ANSI code is \033[31m
        (should-contain "\033[31m" output)))

    (it "colorizes low priority as yellow"
      (let [output (list/format-list {:projects [(nth sample-projects 2)]})]
        ;; yellow ANSI code is \033[33m
        (should-contain "\033[33m" output)))

    (it "colorizes paused status as yellow"
      (let [output (list/format-list {:projects [(second sample-projects)]})]
        (should-contain "\033[33m" output)))

    (it "colorizes 100% progress as green"
      (let [output (list/format-list {:projects [(nth sample-projects 2)]})]
        ;; gamma has 100% progress - should be green
        (should-contain "\033[32m" output))))

  (context "format-list-json"

    (it "returns JSON array of projects"
      (let [output (list/format-list-json {:projects sample-projects})]
        (should-contain "\"slug\"" output)
        (should-contain "\"alpha\"" output)
        (should-contain "\"active\"" output)))

    (it "includes iteration data in JSON"
      (let [output (list/format-list-json {:projects sample-projects})]
        (should-contain "\"iteration\"" output)
        (should-contain "\"009\"" output)))

    (it "includes worker data in JSON"
      (let [output (list/format-list-json {:projects sample-projects})]
        (should-contain "\"workers\"" output)
        (should-contain "\"max_workers\"" output)))

    (it "returns empty array for no projects"
      (should= "[]" (list/format-list-json {:projects []})))))
