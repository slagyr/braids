(ns braids.iteration-spec
  (:require [speclj.core :refer :all]
            [braids.iteration :as iter]))

(describe "braids.iteration"

  (context "parse-iteration-edn"

    (it "parses a valid iteration.edn string"
      (let [edn-str (pr-str {:number "009" :status :active
                              :stories [{:id "proj-abc" :title "Do the thing"}
                                        {:id "proj-def" :title "Another thing"}]
                              :guardrails ["TDD"]
                              :notes ["Theme: testing"]})
            parsed (iter/parse-iteration-edn edn-str)]
        (should= "009" (:number parsed))
        (should= :active (:status parsed))
        (should= 2 (count (:stories parsed)))
        (should= ["TDD"] (:guardrails parsed))
        (should= ["Theme: testing"] (:notes parsed))))

    (it "applies defaults for missing guardrails and notes"
      (let [edn-str (pr-str {:number "001" :status :planning :stories []})
            parsed (iter/parse-iteration-edn edn-str)]
        (should= [] (:guardrails parsed))
        (should= [] (:notes parsed)))))

  (context "validate-iteration"

    (it "returns empty errors for valid iteration"
      (should= [] (iter/validate-iteration {:number "001" :status :active :stories []})))

    (it "catches missing number"
      (should-contain "Missing :number"
        (iter/validate-iteration {:status :active :stories []})))

    (it "catches missing status"
      (should-contain "Missing :status"
        (iter/validate-iteration {:number "001" :stories []})))

    (it "catches invalid status"
      (should-contain "Invalid status: :bogus"
        (iter/validate-iteration {:number "001" :status :bogus :stories []})))

    (it "catches missing stories"
      (should-contain "Missing or invalid :stories"
        (iter/validate-iteration {:number "001" :status :active}))))

  (context "iteration->edn-string"

    (it "round-trips through parse"
      (let [iter-map {:number "003" :status :active
                      :stories [{:id "abc" :title "Thing"}]
                      :guardrails ["TDD"] :notes ["note"]}
            edn-str (iter/iteration->edn-string iter-map)
            parsed (iter/parse-iteration-edn edn-str)]
        (should= "003" (:number parsed))
        (should= :active (:status parsed))
        (should= [{:id "abc" :title "Thing"}] (:stories parsed)))))

  (context "story-ids"

    (it "extracts ids from map stories"
      (should= ["abc" "def"]
               (iter/story-ids {:stories [{:id "abc" :title "A"} {:id "def" :title "B"}]})))

    (it "extracts ids from string stories"
      (should= ["abc" "def"]
               (iter/story-ids {:stories ["abc" "def"]}))))

  (context "annotate-stories"

    (it "annotates stories with bead status info"
      (let [stories [{:id "proj-abc" :title "Do thing"}
                     {:id "proj-def" :title "Other"}]
            beads [{"id" "proj-abc" "status" "open" "priority" 1 "dependencies" []}
                   {"id" "proj-def" "status" "closed" "priority" 1 "dependencies" []}
                   {"id" "proj-zzz" "status" "open" "priority" 2 "dependencies" []}]
            result (iter/annotate-stories stories beads)]
        (should= "open" (:status (first result)))
        (should= "closed" (:status (second result)))))

    (it "marks missing beads as unknown"
      (let [stories [{:id "proj-abc" :title "Do thing"}]
            result (iter/annotate-stories stories [])]
        (should= "unknown" (:status (first result)))))

    (it "handles string story ids (plain bead id format)"
      (let [stories ["proj-abc" "proj-def"]
            beads [{"id" "proj-abc" "status" "open" "priority" 1 "dependencies" []
                    "title" "Do thing"}
                   {"id" "proj-def" "status" "closed" "priority" 2 "dependencies" []
                    "title" "Other thing"}]
            result (iter/annotate-stories stories beads)]
        (should= "proj-abc" (:id (first result)))
        (should= "Do thing" (:title (first result)))
        (should= "open" (:status (first result)))
        (should= "proj-def" (:id (second result)))
        (should= "Other thing" (:title (second result)))
        (should= "closed" (:status (second result)))))

    (it "handles string story ids with missing beads"
      (let [stories ["proj-abc"]
            result (iter/annotate-stories stories [])]
        (should= "proj-abc" (:id (first result)))
        (should= "unknown" (:status (first result))))))

  (context "completion-stats"

    (it "calculates completion percentage"
      (let [stories [{:id "a" :status "closed"}
                     {:id "b" :status "open"}
                     {:id "c" :status "in_progress"}]]
        (should= {:total 3 :closed 1 :percent 33}
                 (iter/completion-stats stories))))

    (it "handles empty stories"
      (should= {:total 0 :closed 0 :percent 0}
               (iter/completion-stats []))))

  (context "format-iteration"

    (it "formats iteration for human-readable output"
      (let [data {:number "009"
                  :status "active"
                  :stories [{:id "proj-abc" :title "Do thing" :status "open" :priority 1 :deps []}
                            {:id "proj-def" :title "Done" :status "closed" :priority 1 :deps []}]
                  :stats {:total 2 :closed 1 :percent 50}}
            output (iter/format-iteration data)]
        (should-contain "Iteration 009" output)
        (should-contain "active" output)
        (should-contain "50%" output)
        (should-contain "proj-abc" output)
        (should-contain "proj-def" output)
        (should-contain "open" output)
        (should-contain "closed" output)
        (should-contain "P1" output)))

    (it "shows dependencies"
      (let [data {:number "001"
                  :status "active"
                  :stories [{:id "proj-abc" :title "Thing" :status "open" :priority 1
                             :deps ["proj-def"]}]
                  :stats {:total 1 :closed 0 :percent 0}}
            output (iter/format-iteration data)]
        (should-contain "proj-def" output)))

    (it "formats as JSON when requested"
      (let [data {:number "001"
                  :status "active"
                  :stories [{:id "a" :title "T" :status "open" :priority 1 :deps []}]
                  :stats {:total 1 :closed 0 :percent 0}}
            output (iter/format-iteration-json data)]
        (should-contain "\"number\"" output)
        (should-contain "\"stories\"" output)
        (should-contain "\"percent\"" output)))))
