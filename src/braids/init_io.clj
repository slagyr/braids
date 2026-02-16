(ns braids.init-io
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [braids.init :as init]
            [braids.registry :as registry]))

(def default-braids-dir (str (System/getProperty "user.home") "/.openclaw/braids"))
(def default-braids-home (or (System/getenv "BRAIDS_HOME")
                                (str (System/getProperty "user.home") "/Projects")))
(def default-registry-path (str default-braids-dir "/registry.edn"))

(defn parse-init-args [args]
  (loop [remaining (vec args)
         result {}]
    (if (empty? remaining)
      result
      (let [[flag val & rest-args] remaining]
        (case flag
          "--force" (recur (vec (rest remaining)) (assoc result :force? true))
          "--braids-home" (recur (vec rest-args) (assoc result :braids-home val))
          (recur (vec (rest remaining)) result))))))

(defn bd-available? []
  (try
    (let [result (proc/shell {:out :string :err :string :continue true} "which" "bd")]
      (zero? (:exit result)))
    (catch Exception _ false)))

(defn run-init
  ([args] (run-init args {}))
  ([args {:keys [braids-dir braids-home registry-path]}]
   (let [params (parse-init-args args)
         braids-dir (or braids-dir (:braids-dir params) default-braids-dir)
         braids-home (or braids-home (:braids-home params) default-braids-home)
         registry-path (or registry-path default-registry-path)
         prereq-errors (init/check-prerequisites
                         {:braids-dir-exists? (fs/exists? braids-dir)
                          :registry-exists? (fs/exists? registry-path)
                          :bd-available? (bd-available?)
                          :force? (:force? params)})]
     (if (seq prereq-errors)
       {:exit 1 :message (init/format-result {:success? false :errors prereq-errors})}
       (let [plan (init/plan-init {:braids-dir braids-dir
                                    :braids-home braids-home
                                    :registry-path registry-path
                                    :braids-dir-exists? (fs/exists? braids-dir)
                                    :braids-home-exists? (fs/exists? braids-home)})
             actions-taken (atom [])]
         ;; Execute plan
         (doseq [{:keys [action path]} plan]
           (case action
             :create-braids-dir (do (fs/create-dirs path) (swap! actions-taken conj action))
             :create-braids-home (do (fs/create-dirs path) (swap! actions-taken conj action))
             :create-registry (do (spit path (pr-str {:projects []}))
                                  (swap! actions-taken conj action))))
         {:exit 0
          :message (init/format-result {:success? true
                                         :braids-dir braids-dir
                                         :braids-home braids-home
                                         :registry-path registry-path
                                         :actions-taken @actions-taken})})))))
