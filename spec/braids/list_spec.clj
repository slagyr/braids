(ns braids.list-spec
  (:require [speclj.core :refer :all]
            [braids.list :as list]))

(describe "braids.list"

  (describe "format-list"

    (it "returns 'No projects registered.' for empty registry"
      (should= "No projects registered." (list/format-list {:projects []})))

    (it "returns 'No projects registered.' for nil projects"
      (should= "No projects registered." (list/format-list {:projects nil})))

    (it "formats a single project as a table"
      (let [reg {:projects [{:slug "my-project"
                              :status :active
                              :priority :high
                              :path "~/Projects/my-project"}]}
            output (list/format-list reg)]
        (should-contain "my-project" output)
        (should-contain "active" output)
        (should-contain "high" output)
        (should-contain "~/Projects/my-project" output)))

    (it "formats multiple projects"
      (let [reg {:projects [{:slug "alpha" :status :active :priority :high :path "~/Projects/alpha"}
                             {:slug "beta" :status :paused :priority :low :path "~/Projects/beta"}]}
            output (list/format-list reg)]
        (should-contain "alpha" output)
        (should-contain "beta" output)
        (should-contain "paused" output)))

    (it "includes a header row"
      (let [reg {:projects [{:slug "test" :status :active :priority :normal :path "~/Projects/test"}]}
            output (list/format-list reg)]
        (should-contain "SLUG" output)
        (should-contain "STATUS" output)
        (should-contain "PRIORITY" output)
        (should-contain "PATH" output)))

    (it "aligns columns"
      (let [reg {:projects [{:slug "short" :status :active :priority :high :path "~/a"}
                             {:slug "a-very-long-slug" :status :paused :priority :low :path "~/b"}]}
            lines (clojure.string/split-lines (list/format-list reg))]
        ;; Header and two data rows (plus separator)
        (should= 4 (count lines)))))

  (describe "format-list-json"

    (it "returns JSON array of projects"
      (let [reg {:projects [{:slug "test" :status :active :priority :normal :path "~/Projects/test"}]}
            output (list/format-list-json reg)]
        (should-contain "\"slug\"" output)
        (should-contain "\"test\"" output)
        (should-contain "\"active\"" output)))

    (it "returns empty array for no projects"
      (should= "[]" (list/format-list-json {:projects []})))))
