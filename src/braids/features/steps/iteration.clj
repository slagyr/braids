(ns braids.features.steps.iteration
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [braids.iteration :as iteration]
            [clojure.string :as str]
            [speclj.core :refer :all]))

;; --- Helper functions ---

(defn- parse-iteration-edn* []
  (let [edn-str (g/get :iter-edn-str)
        result (iteration/parse-iteration-edn edn-str)]
    (g/assoc! :iter-parsed result)))

(defn- validate-iteration* []
  (let [data (g/get :iter-data)
        errors (iteration/validate-iteration data)]
    (g/assoc! :validation-result errors)))

(defn- annotate-stories* []
  (let [stories (g/get :iter-stories)
        beads (g/get :iter-beads)
        result (iteration/annotate-stories stories beads)]
    (g/assoc! :iter-annotated result)))

(defn- calculate-completion-stats* []
  (let [stories (g/get :iter-annotated)
        result (iteration/completion-stats stories)]
    (g/assoc! :iter-stats result)))

(defn- format-iteration* []
  (let [data (g/get :iter-format-data)
        result (iteration/format-iteration data)]
    (g/assoc! :output result)))

(defn- format-iteration-json* []
  (let [data (g/get :iter-format-data)
        result (iteration/format-iteration-json data)]
    (g/assoc! :iter-json-output result)))

;; --- Given steps ---

(defgiven iteration-edn #"^iteration EDN with number \"([^\"]+)\" and status \"([^\"]+)\" and (\d+) stor(?:y|ies)$"
  [number status count]
  (let [story-count (parse-long count)
        stories (vec (repeat story-count (str "story-" (rand-int 10000))))
        edn-str (pr-str {:number number :status (keyword status) :stories stories})]
    (g/assoc! :iter-edn-str edn-str)))

(defgiven edn-no-guardrails-or-notes "the EDN has no guardrails or notes"
  []
  nil)

(defgiven iteration-with-status "an iteration with number {number:string} and status {status:string} and stories"
  [number status]
  (g/assoc! :iter-data {:number number :status (keyword status) :stories []}))

(defgiven iteration-no-number "an iteration with no number"
  []
  (g/assoc! :iter-data {:status :planning :stories []}))

(defgiven iteration-with-stories "an iteration with stories {id1:string} and {id2:string}"
  [id1 id2]
  (g/assoc! :iter-stories [id1 id2]))

(defgiven iteration-with-story "an iteration with story {story-id:string}"
  [story-id]
  (g/assoc! :iter-stories [story-id]))

(defgiven iter-bead-status "bead {bead-id:string} has status {status:string} and priority {priority:int}"
  [bead-id status priority]
  (g/update! :iter-beads (fnil conj []) {"id" bead-id "status" status "priority" priority}))

(defgiven no-bead-data "no bead data exists"
  []
  nil)

(defgiven annotated-stories "annotated stories with {closed:int} closed and {open:int} open out of {total:int} total"
  [closed open total]
  (let [closed-stories (repeat closed {:id "c" :status "closed"})
        open-stories (repeat open {:id "o" :status "open"})
        stories (vec (concat closed-stories open-stories))]
    (g/assoc! :iter-annotated stories)))

(defgiven iteration-no-stories "an iteration with no stories"
  []
  (g/assoc! :iter-stories []))

(defgiven iteration-number-status "an iteration {number:string} with status {status:string}"
  [number status]
  (g/assoc! :iter-format-data {:number number :status status :stories [] :stats nil}))

(defgiven story-with-status "a story {story-id:string} with status {status:string}"
  [story-id status]
  (g/update-in! [:iter-format-data :stories] conj
                {:id story-id :title story-id :status status :priority nil :deps []}))

(defgiven completion-stats "completion stats of {closed:int} closed out of {total:int}"
  [closed total]
  (let [percent (if (zero? total) 0 (int (* 100 (/ closed total))))]
    (g/assoc-in! [:iter-format-data :stats]
                 {:total total :closed closed :percent percent})))

;; --- When steps ---

(defwhen parse-iteration-edn "parsing the iteration EDN"
  []
  (parse-iteration-edn*))

(defwhen validate-iteration "validating the iteration"
  []
  (validate-iteration*))

(defwhen annotate-stories "annotating stories with bead data"
  []
  (annotate-stories*))

(defwhen calculate-completion-stats "calculating completion stats"
  []
  (calculate-completion-stats*))

(defwhen format-iteration "formatting the iteration"
  []
  (format-iteration*))

(defwhen format-iteration-json "formatting the iteration as JSON"
  []
  (format-iteration-json*))

;; --- Then steps ---

(defthen assert-iteration-number "the iteration number should be {expected:string}"
  [expected]
  (should= expected (:number (g/get :iter-parsed))))

(defthen assert-iteration-status "the iteration status should be {expected:string}"
  [expected]
  (should= expected (name (:status (g/get :iter-parsed)))))

(defthen assert-iteration-guardrails-empty "the iteration guardrails should be empty"
  []
  (should (empty? (:guardrails (g/get :iter-parsed)))))

(defthen assert-iteration-notes-empty "the iteration notes should be empty"
  []
  (should (empty? (:notes (g/get :iter-parsed)))))

(defthen assert-story-status "story {story-id:string} should have status {expected:string}"
  [story-id expected]
  (should= expected (:status (first (filter #(= story-id (:id %)) (g/get :iter-annotated))))))

(defthen assert-total "the total should be {expected:int}"
  [expected]
  (should= expected (:total (g/get :iter-stats))))

(defthen assert-closed-count "the closed count should be {expected:int}"
  [expected]
  (should= expected (:closed (g/get :iter-stats))))

(defthen assert-completion-percent "the completion percent should be {expected:int}"
  [expected]
  (should= expected (:percent (g/get :iter-stats))))

(defthen assert-json-contains #"^the JSON should contain \"([^\"]+)\"$"
  [expected]
  (should (str/includes? (g/get :iter-json-output) expected)))
