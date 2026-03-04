(ns braids.sys-spec
  (:require [speclj.core :refer :all]
            [braids.sys :as sys]))

(describe "braids.sys"

  (describe "subprocess-env"
    (it "returns PATH with env-path prepended when set"
      (let [env (sys/subprocess-env {:env-path "/custom/bin"})]
        (should (clojure.string/starts-with? (get env "PATH") "/custom/bin:"))
        (should (clojure.string/includes? (get env "PATH") ":"))))

    (it "returns current PATH when env-path is nil"
      (let [env (sys/subprocess-env {:env-path nil})
            current (or (System/getenv "PATH") "/usr/bin:/bin")]
        (should= {"PATH" current} env)))

    (it "returns current PATH when env-path is absent"
      (let [env (sys/subprocess-env {})
            current (or (System/getenv "PATH") "/usr/bin:/bin")]
        (should= {"PATH" current} env))))

  (describe "bd-bin"
    (it "returns configured value"
      (should= "/usr/local/bin/bd" (sys/bd-bin {:bd-bin "/usr/local/bin/bd"})))

    (it "defaults to bd when nil"
      (should= "bd" (sys/bd-bin {:bd-bin nil})))

    (it "defaults to bd when absent"
      (should= "bd" (sys/bd-bin {}))))

  (describe "openclaw-bin"
    (it "returns configured value"
      (should= "/custom/openclaw" (sys/openclaw-bin {:openclaw-bin "/custom/openclaw"})))

    (it "defaults to openclaw when nil"
      (should= "openclaw" (sys/openclaw-bin {:openclaw-bin nil})))

    (it "defaults to openclaw when absent"
      (should= "openclaw" (sys/openclaw-bin {})))))
