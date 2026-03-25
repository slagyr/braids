(ns braids.features.steps.orch-output
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.orch :as orch]
            [braids.orch-runner :as orch-runner]
            [clojure.string :as str]
            [speclj.core :refer :all]))

;; --- Helper functions ---

(defn- strip-ansi [s]
  (str/replace s #"\033\[[0-9;]*m" ""))

(defn- configure-projects-impl [headers rows]
  (doseq [row rows]
    (let [m (zipmap headers row)
          slug (get m "slug")
          has-status? (some #(= "status" %) headers)
          status (when has-status? (keyword (get m "status")))
          priority (when (some #(= "priority" %) headers) (keyword (get m "priority")))
          max-workers (when (some #(= "max-workers" %) headers) (parse-long (get m "max-workers")))
          active-iteration (get m "active-iteration")
          active-workers (when (some #(= "active-workers" %) headers) (parse-long (get m "active-workers")))
          path (get m "path")
          worker-timeout (when (some #(= "worker-timeout" %) headers) (get m "worker-timeout"))
          worker-agent (when (some #(= "worker-agent" %) headers) (get m "worker-agent"))
          worker-model (when (some #(= "worker-model" %) headers) (get m "worker-model"))
          worker-thinking (when (some #(= "worker-thinking" %) headers) (get m "worker-thinking"))
          channel (when (some #(= "channel" %) headers) (get m "channel"))]
      (when has-status?
        (let [new-entry {:slug slug :status (or status :active) :priority (or priority :normal)
                         :path (or path (str "/projects/" slug))}]
          (g/update-in! [:registry :projects]
                        (fn [projects]
                          (let [without (filterv #(not= slug (:slug %)) (or projects []))]
                            (conj without new-entry))))))
      (when (and path (not has-status?))
        (g/update-in! [:registry :projects]
                      (fn [projects]
                        (mapv (fn [p] (if (= slug (:slug p)) (assoc p :path path) p))
                              (or projects [])))))
      (let [base-config (cond-> {}
                          status (assoc :status status)
                          max-workers (assoc :max-workers max-workers)
                          (and worker-timeout (seq worker-timeout)) (assoc :worker-timeout (parse-long worker-timeout))
                          (and worker-agent (seq worker-agent)) (assoc :worker-agent worker-agent)
                          (and worker-model (seq worker-model)) (assoc :worker-model worker-model)
                          (and worker-thinking (seq worker-thinking)) (assoc :worker-thinking worker-thinking)
                          channel (assoc :channel channel))]
        (g/update-in! [:configs slug] merge base-config))
      (when (and active-iteration (seq active-iteration))
        (g/assoc-in! [:iterations slug] active-iteration))
      (when (and (some #(= "active-iteration" %) headers)
                 (or (nil? active-iteration) (empty? active-iteration)))
        (g/update! :iterations dissoc slug))
      (when active-workers
        (g/assoc-in! [:workers slug] active-workers)))))

(defn- set-project-beads-impl [slug headers rows]
  (let [beads (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:id (get m "id")
                         :title (get m "title")
                         :status (get m "status")}))
                    rows)
        ready-beads (filterv #(= "ready" (:status %)) beads)]
    (g/assoc-in! [:open-beads slug] beads)
    (g/assoc-in! [:beads slug] ready-beads)))

(defn- output-contains-line? [text]
  (when-let [output (g/get :tick-output)]
    (some #(str/includes? (strip-ansi %) text) (str/split-lines output))))

(defn- output-contains-line-matching? [pattern-str]
  (when-let [output (g/get :tick-output)]
    (let [pattern (re-pattern pattern-str)]
      (some #(re-find pattern (strip-ansi %)) (str/split-lines output)))))

(defn- output-contains? [text]
  (when-let [output (g/get :tick-output)]
    (str/includes? output text)))

(defn- output-has-before? [a b]
  (when-let [output (g/get :tick-output)]
    (let [idx-a (str/index-of output a)
          idx-b (str/index-of output b)]
      (and idx-a idx-b (< idx-a idx-b)))))

;; --- Given steps ---

(defgiven configured-projects-table "configured projects:"
  [table]
  (let [{:keys [headers rows]} table]
    (configure-projects-impl headers rows)))

(defgiven project-has-beads-table "project {slug:string} has beads:"
  [slug table]
  (let [{:keys [headers rows]} table]
    (set-project-beads-impl slug headers rows)))

;; --- Then steps ---

(defthen output-contains-lines-matching "the output contains lines matching"
  [table]
  (let [{:keys [rows]} table]
    (doseq [row rows]
      (should (output-contains-line? (first row))))))

(defthen output-contains-a-line-matching "the output contains a line matching"
  [table]
  (let [{:keys [rows]} table]
    (doseq [row rows]
      (should (output-contains-line-matching? (first row))))))

(defthen output-does-not-contain "the output does not contain"
  [table]
  (let [{:keys [rows]} table]
    (doseq [row rows]
      (should-not (output-contains? (first row))))))

(defthen output-has-before "the output has {first-text:string} before {second-text:string}"
  [first-text second-text]
  (should (output-has-before? first-text second-text)))
