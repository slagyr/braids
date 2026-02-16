(ns braids.new-io-spec
  (:require [speclj.core :refer :all]
            [braids.new-io :as new-io]
            [babashka.fs :as fs]
            [clojure.edn :as edn]))

(describe "braids.new-io"

  (describe "parse-new-args"

    (it "parses slug as first positional arg"
      (let [result (new-io/parse-new-args ["my-project" "--name" "My Project" "--goal" "Build it"])]
        (should= "my-project" (:slug result))
        (should= "My Project" (:name result))
        (should= "Build it" (:goal result))))

    (it "parses all flags"
      (let [result (new-io/parse-new-args ["foo" "--name" "Foo" "--goal" "G"
                                            "--priority" "high" "--autonomy" "ask-first"
                                            "--channel" "123" "--max-workers" "3"])]
        (should= :high (:priority result))
        (should= :ask-first (:autonomy result))
        (should= "123" (:channel result))
        (should= 3 (:max-workers result))))

    (it "collects multiple guardrails"
      (let [result (new-io/parse-new-args ["foo" "--name" "F" "--goal" "G"
                                            "--guardrails" "No breaking changes"
                                            "--guardrails" "TDD always"])]
        (should= ["No breaking changes" "TDD always"] (:guardrails result)))))

  (describe "run-new"

    (with-all tmp-dir (str (fs/create-temp-dir {:prefix "braids-new-test"})))
    (with-all reg-file (str @tmp-dir "/registry.edn"))

    (before-all
      (spit @reg-file (pr-str {:projects []})))

    (after-all
      (fs/delete-tree @tmp-dir))

    (it "rejects missing required params"
      (let [result (new-io/run-new ["--braids-home" @tmp-dir] {:registry-file @reg-file})]
        (should= 1 (:exit result))
        (should-contain "Errors" (:message result))))

    (it "rejects missing name"
      (let [result (new-io/run-new ["test-proj" "--goal" "G" "--braids-home" @tmp-dir]
                                    {:registry-file @reg-file})]
        (should= 1 (:exit result))
        (should-contain "Name is required" (:message result))))

    (it "creates a project successfully"
      (let [result (new-io/run-new ["test-new-proj" "--name" "Test New" "--goal" "Test goal"
                                     "--braids-home" @tmp-dir]
                                    {:registry-file @reg-file})
            project-dir (str @tmp-dir "/test-new-proj")]
        (should= 0 (:exit result))
        (should (fs/exists? (str project-dir "/.project/project.edn")))
        (should (fs/exists? (str project-dir "/.project/iterations/001/ITERATION.md")))
        (should (fs/exists? (str project-dir "/AGENTS.md")))
        (should (fs/exists? (str project-dir "/.git")))
        ;; Verify project.edn content
        (let [config (edn/read-string (slurp (str project-dir "/.project/project.edn")))]
          (should= "Test New" (:name config))
          (should= "Test goal" (:goal config))
          (should= :active (:status config))
          (should= :full (:autonomy config)))
        ;; Verify registry was updated
        (let [reg (edn/read-string (slurp @reg-file))]
          (should= 1 (count (:projects reg)))
          (should= "test-new-proj" (:slug (first (:projects reg)))))))

    (it "rejects duplicate directory"
      (fs/create-dirs (str @tmp-dir "/existing-proj"))
      (let [result (new-io/run-new ["existing-proj" "--name" "E" "--goal" "G"
                                     "--braids-home" @tmp-dir]
                                    {:registry-file @reg-file})]
        (should= 1 (:exit result))
        (should-contain "already exists" (:message result))))

    (it "rejects duplicate slug in registry"
      (let [result (new-io/run-new ["test-new-proj" "--name" "Dup" "--goal" "G"
                                     "--braids-home" (str @tmp-dir "/other")]
                                    {:registry-file @reg-file})]
        (should= 1 (:exit result))
        (should-contain "already exists in registry" (:message result))))))
