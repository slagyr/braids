(ns braids.features.steps.project-status
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.status :as status]
            [cheshire.core :as json]
            [speclj.core :refer :all]))

;; --- Helper functions ---

(defn- build-dashboard* []
  (let [status-registry (g/get :status-registry)
        ready-registry (g/get :ready-registry)
        configs (g/get :status-configs)
        iterations (g/get :status-iterations)
        workers (g/get :status-workers)
        registry (if (seq (:projects status-registry))
                   status-registry
                   ready-registry)
        result (status/build-dashboard registry configs iterations workers)]
    (g/assoc! :dashboard result)))

(defn- format-project-detail* [slug]
  (let [project (g/get-in [:detail-projects slug])
        result (status/format-project-detail project)]
    (g/assoc! :output result)))

(defn- format-dashboard* []
  (let [dash (g/get :dashboard)
        result (status/format-dashboard dash)]
    (g/assoc! :output result)))

(defn- format-dashboard-json* []
  (let [dash (g/get :dashboard)
        result (status/format-dashboard-json dash)
        parsed (json/parse-string result true)]
    (g/assoc! :dashboard-json-data parsed :output result :list-json-output result)))

(defn- dashboard-project [slug]
  (first (filter #(= slug (:slug %)) (:projects (g/get :dashboard)))))

(defn- dashboard-json-project [slug]
  (let [json-str (g/get :output)
        parsed (json/parse-string json-str)]
    (first (filter #(= slug (get % "slug")) (get parsed "projects")))))

;; --- Given steps ---

(defgiven project-configs-table "project configs:"
  [table]
  (let [{:keys [headers rows]} table
        configs (reduce (fn [acc row]
                          (let [m (zipmap headers row)
                                slug (get m "slug")
                                max-w (parse-long (get m "max-workers"))]
                            (assoc acc slug {:max-workers max-w})))
                        {}
                        rows)]
    (g/update! :status-configs (fnil merge {}) configs)))

(defgiven active-iterations-table "active iterations:"
  [table]
  (let [{:keys [headers rows]} table
        iterations (reduce (fn [acc row]
                             (let [m (zipmap headers row)
                                   slug (get m "slug")]
                               (assoc acc slug {:number (get m "number")
                                                :stats {:total (parse-long (get m "total"))
                                                        :closed (parse-long (get m "closed"))
                                                        :percent (parse-long (get m "percent"))}})))
                           {}
                           rows)]
    (g/update! :status-iterations (fnil merge {}) iterations)))

(defgiven active-workers-table "active workers:"
  [table]
  (let [{:keys [headers rows]} table
        workers (reduce (fn [acc row]
                          (let [m (zipmap headers row)]
                            (assoc acc (get m "slug") (parse-long (get m "count")))))
                        {}
                        rows)]
    (g/update! :status-workers (fnil merge {}) workers)))

(defgiven no-active-iterations "no active iterations"
  []
  nil)

(defgiven dashboard-project-given "a dashboard project {slug:string} with:"
  [slug table]
  (let [{:keys [headers rows]} table
        all-pairs (cons headers rows)
        m (reduce (fn [acc [k v]] (assoc acc k v)) {} (map vec all-pairs))]
    (g/assoc-in! [:detail-projects slug]
                 {:slug slug
                  :status (get m "status" "unknown")
                  :workers (when-let [w (get m "workers")] (parse-long w))
                  :max-workers (when-let [w (get m "max-workers")] (parse-long w))})))

(defgiven project-has-iteration "project {slug:string} has iteration:"
  [slug table]
  (let [{:keys [headers rows]} table
        all-pairs (cons headers rows)
        m (reduce (fn [acc [k v]] (assoc acc k v)) {} (map vec all-pairs))]
    (g/update-in! [:detail-projects slug]
                  assoc :iteration {:number (get m "number")
                                    :stats {:total (parse-long (get m "total"))
                                            :closed (parse-long (get m "closed"))
                                            :percent (parse-long (get m "percent"))}})))

(defgiven project-has-stories "project {slug:string} has stories:"
  [slug table]
  (let [{:keys [headers rows]} table
        stories (mapv (fn [row]
                        (let [m (zipmap headers row)]
                          {:id (get m "id")
                           :title (get m "title")
                           :status (get m "status")}))
                      rows)]
    (g/update-in! [:detail-projects slug :iteration] assoc :stories stories)))

(defgiven project-has-no-iteration "project {slug:string} has no iteration"
  [slug]
  (g/update-in! [:detail-projects slug] dissoc :iteration))

(defgiven empty-registry "an empty registry"
  []
  (g/assoc! :status-registry {:projects []} :ready-registry {:projects []}))

;; --- When steps ---

(defwhen build-dashboard "building the dashboard"
  []
  (build-dashboard*))

(defwhen format-project-detail "formatting project detail for {slug:string}"
  [slug]
  (format-project-detail* slug))

(defwhen format-dashboard-json "formatting the dashboard as JSON"
  []
  (format-dashboard-json*))

(defwhen format-dashboard "formatting the dashboard"
  []
  (format-dashboard*))

;; --- Then steps ---

(defthen assert-dashboard-project-count #"^the dashboard should have (\d+) projects?$"
  [cnt]
  (should= (parse-long cnt) (clojure.core/count (:projects (g/get :dashboard)))))

(defthen assert-project-status "project {slug:string} should have status {expected:string}"
  [slug expected]
  (should= expected (:status (dashboard-project slug))))

(defthen assert-project-iteration-number "project {slug:string} should have iteration number {expected:string}"
  [slug expected]
  (should= expected (get-in (dashboard-project slug) [:iteration :number])))

(defthen assert-project-workers "project {slug:string} should have workers {workers:int} of {max-workers:int}"
  [slug workers max-workers]
  (should= workers (:workers (dashboard-project slug)))
  (should= max-workers (:max-workers (dashboard-project slug))))

(defthen assert-project-no-iteration "project {slug:string} should have no iteration"
  [slug]
  (should-be-nil (:iteration (dashboard-project slug))))

(defthen assert-json-project-count #"^the JSON should contain (\d+) projects?$"
  [cnt]
  (should= (parse-long cnt) (clojure.core/count (:projects (g/get :dashboard-json-data)))))

(defthen assert-json-project-iteration-percent "the JSON project {slug:string} should have iteration percent {percent:int}"
  [slug percent]
  (should= percent (get-in (dashboard-json-project slug) ["iteration" "stats" "percent"])))
