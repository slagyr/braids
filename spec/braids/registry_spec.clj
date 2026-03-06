(ns braids.registry-spec
  (:require [speclj.core :refer :all]
            [braids.registry :as registry]))

(describe "braids.registry"

  (context "parse-registry"

    (it "parses a valid registry.edn"
      (let [edn-str (pr-str {:projects [{:slug "my-project"
                                          :status :active
                                          :priority :normal
                                          :path "~/Projects/my-project"}]})
            result (registry/parse-registry edn-str)]
        (should= 1 (count (:projects result)))
        (should= "my-project" (:slug (first (:projects result))))
        (should= :active (:status (first (:projects result))))))

    (it "parses multiple projects"
      (let [edn-str (pr-str {:projects [{:slug "alpha" :status :active :priority :high :path "~/Projects/alpha"}
                                         {:slug "beta" :status :paused :priority :low :path "~/Projects/beta"}]})
            result (registry/parse-registry edn-str)]
        (should= 2 (count (:projects result)))))

    (it "returns empty projects for empty registry"
      (let [result (registry/parse-registry (pr-str {:projects []}))]
        (should= [] (:projects result)))))

  (context "validate-registry"

    (it "validates a correct registry"
      (let [reg {:projects [{:slug "test" :status :active :priority :normal :path "~/Projects/test"}]}]
        (should= [] (registry/validate-registry reg))))

    (it "rejects invalid status"
      (let [reg {:projects [{:slug "test" :status :done :priority :normal :path "~/Projects/test"}]}]
        (should-not= [] (registry/validate-registry reg))))

    (it "rejects invalid priority"
      (let [reg {:projects [{:slug "test" :status :active :priority :urgent :path "~/Projects/test"}]}]
        (should-not= [] (registry/validate-registry reg))))

    (it "rejects missing slug"
      (let [reg {:projects [{:status :active :priority :normal :path "~/Projects/test"}]}]
        (should-not= [] (registry/validate-registry reg))))

    (it "rejects duplicate slugs"
      (let [reg {:projects [{:slug "test" :status :active :priority :normal :path "~/Projects/test"}
                             {:slug "test" :status :paused :priority :low :path "~/Projects/test2"}]}]
        (should-not= [] (registry/validate-registry reg)))))

  (context "registry->edn-string"

    (it "round-trips through parse"
      (let [reg {:projects [{:slug "test" :status :active :priority :normal :path "~/Projects/test"}]}
            edn-str (registry/registry->edn-string reg)
            parsed (registry/parse-registry edn-str)]
        (should= reg parsed)))))
