(ns braids.features.steps.ready-beads
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.ready :as ready]
            [clojure.string :as str]
            [speclj.core :refer :all]))

;; --- Helper functions ---

(defn- table-row->registry-project [headers row]
  (let [m (zipmap headers row)]
    {:slug (get m "slug")
     :status (keyword (get m "status"))
     :priority (keyword (get m "priority"))
     :path (or (get m "path") (str "/projects/" (get m "slug")))}))

(defn- table-row->ready-bead [headers row]
  (let [m (zipmap headers row)]
    {:id (get m "id")
     :title (get m "title")
     :priority (get m "priority")}))

(defn- compute-ready-beads* []
  (let [registry (g/get :ready-registry)
        configs (g/get :ready-configs)
        beads (g/get :ready-beads)
        workers (g/get :workers)
        result (ready/ready-beads registry configs beads workers)]
    (g/assoc! :ready-result result)))

(defn- format-ready-output* []
  (let [beads (g/get :ready-format-beads)
        output (ready/format-ready-output beads)]
    (g/assoc! :ready-output output :output output)))

;; --- Given steps ---

(defgiven registry-with-projects-table "a registry with projects:"
  [table]
  (let [{:keys [headers rows]} table
        projects (mapv #(table-row->registry-project headers %) rows)
        registry {:projects projects}]
    (g/assoc! :ready-registry registry :status-registry registry)))

(defgiven project-config-status-and-max-workers "project {slug:string} has config with status {status:string} and max-workers {max-workers:int}"
  [slug status max-workers]
  (let [parsed-config {:status (keyword status) :max-workers max-workers}]
    (g/assoc-in! [:ready-configs slug] (merge {:status :active} parsed-config))))

(defgiven project-config-max-workers "project {slug:string} has config with max-workers {max-workers:int}"
  [slug max-workers]
  (g/assoc-in! [:ready-configs slug] (merge {:status :active} {:max-workers max-workers})))

(defgiven project-ready-beads-table "project {slug:string} has ready beads:"
  [slug table]
  (let [{:keys [headers rows]} table
        beads (mapv #(table-row->ready-bead headers %) rows)]
    (g/assoc-in! [:ready-beads slug] beads)))

(defgiven no-active-workers "no active workers"
  []
  nil)

(defgiven ready-beads-to-format "ready beads to format:"
  [table]
  (let [{:keys [headers rows]} table
        beads (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:project (get m "project")
                         :id (get m "id")
                         :title (get m "title")
                         :priority (get m "priority")}))
                    rows)]
    (g/assoc! :ready-format-beads beads)))

(defgiven no-ready-beads-to-format "no ready beads to format"
  []
  (g/assoc! :ready-format-beads []))

;; --- When steps ---

(defwhen compute-ready-beads "computing ready beads"
  []
  (compute-ready-beads*))

(defwhen format-ready-output "formatting ready output"
  []
  (format-ready-output*))

;; --- Then steps ---

(defthen assert-result-contains-bead "the result should contain bead {bead-id:string}"
  [bead-id]
  (should (some #(= bead-id (:id %)) (g/get :ready-result))))

(defthen assert-result-not-contains-bead "the result should not contain bead {bead-id:string}"
  [bead-id]
  (should-not (some #(= bead-id (:id %)) (g/get :ready-result))))

(defthen assert-result-empty "the result should be empty"
  []
  (should (empty? (g/get :ready-result))))

(defthen assert-nth-result-project #"^the (first|second|third) result should be from project \"([^\"]+)\"$"
  [ordinal slug]
  (let [position (case ordinal "first" 1 "second" 2 "third" 3)]
    (should= slug (:project (nth (g/get :ready-result) (dec position))))))

(defthen assert-output-contains #"^the output should contain \"([^\"]+)\"$"
  [expected]
  (should (str/includes? (g/get :output) expected)))
