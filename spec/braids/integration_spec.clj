(ns braids.integration-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [braids.core :as core]
            [braids.new :as new]
            [braids.config :as config]
            [braids.ready :as ready]
            [braids.list :as list]
            [braids.iteration :as iter]
            [braids.orch :as orch]))

(describe "Integration: braids CLI dispatch"

  (it "help command returns 0"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run ["help"])))
      (should= 0 @result)))

  (it "unknown command returns 1"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run ["nonexistent"])))
      (should= 1 @result)))

  (it "--help flag returns 0"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run ["--help"])))
      (should= 0 @result)))

  (it "no args returns 0 (shows help)"
    (let [result (atom nil)]
      (with-out-str (reset! result (core/run nil)))
      (should= 0 @result))))


(describe "Integration: Pure function contracts"

  (it "ready-beads returns empty for empty registry"
    (let [result (ready/ready-beads {:projects []} {} {} {})]
      (should= [] result)))

  (it "ready-beads respects max-workers"
    (let [reg {:projects [{:slug "p1" :status :active :priority :normal}]}
          configs {"p1" {:status :active :max-workers 1}}
          beads {"p1" [{:id "b1" :title "B1" :priority 2}]}
          ;; Already at max workers
          workers {"p1" 1}
          result (ready/ready-beads reg configs beads workers)]
      (should= [] result)))

  (it "ready-beads returns beads when capacity available"
    (let [reg {:projects [{:slug "p1" :status :active :priority :normal}]}
          configs {"p1" {:status :active :max-workers 2}}
          beads {"p1" [{:id "b1" :title "B1" :priority 2}]}
          workers {"p1" 0}
          result (ready/ready-beads reg configs beads workers)]
      (should= 1 (count result))
      (should= "b1" (:id (first result)))))

  (it "orch/tick returns idle with disable-cron when no projects"
    (let [result (orch/tick {:projects []} {} {} {} {} {})]
      (should= "idle" (:action result))
      (should= true (:disable-cron result))))

  (it "list/format-list handles empty projects"
    (should= "No projects registered." (list/format-list {:projects []})))

  (it "list/format-list formats projects as table"
    (let [output (list/format-list {:projects [{:slug "test" :status :active :priority :normal :path "/tmp/test"}]})]
      (should (str/includes? output "test"))
      (should (str/includes? output "active"))))

  (it "iteration/parse-iteration-edn applies defaults"
    (let [parsed (iter/parse-iteration-edn "{:number 1 :status :active :stories []}")]
      (should= 1 (:number parsed))
      (should= :active (:status parsed))
      (should= [] (:stories parsed))))

  (it "iteration/completion-stats calculates correctly"
    (let [stats (iter/completion-stats [{:status "closed"} {:status "open"} {:status "closed"}])]
      (should= 3 (:total stats))
      (should= 2 (:closed stats))
      (should= 66 (:percent stats))))

  (it "new/validate-slug accepts valid slugs"
    (should= [] (new/validate-slug "my-project"))
    (should= [] (new/validate-slug "abc123"))
    (should= [] (new/validate-slug "a")))

  (it "new/validate-slug rejects invalid slugs"
    (should (seq (new/validate-slug nil)))
    (should (seq (new/validate-slug "")))
    (should (seq (new/validate-slug "UPPERCASE")))
    (should (seq (new/validate-slug "-leading-hyphen")))
    (should (seq (new/validate-slug "trailing-hyphen-"))))

  (it "config/config-get returns value for known key"
    (let [result (config/config-get {:braids-home "/tmp"} "braids-home")]
      (should= "/tmp" (:ok result))))

  (it "config/config-get returns error for unknown key"
    (let [result (config/config-get {:braids-home "/tmp"} "nonexistent")]
      (should-not-be-nil (:error result))))

  (it "config/config-set updates config"
    (let [updated (config/config-set {:braids-home "/tmp"} "braids-home" "/new")]
      (should= "/new" (:braids-home updated)))))



