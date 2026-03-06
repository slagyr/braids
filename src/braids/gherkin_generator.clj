(ns braids.gherkin-generator
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn source->ns-name
  "Convert a feature source filename to a Clojure namespace name."
  [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str/replace #"_" "-")
      (->> (str "braids.features."))
      (str "-spec")))

(defn generate-ns-form
  "Generate the ns declaration string for a feature spec."
  [source]
  (str "(ns " (source->ns-name source) "\n"
       "  (:require [speclj.core :refer :all]))"))

(defn- format-steps
  "Format a sequence of step texts as comments with Given/When/Then prefixes.
   First step gets the keyword, subsequent get 'And'."
  [keyword steps]
  (when (seq steps)
    (let [first-line (str ";; " keyword " " (first steps))
          rest-lines (map #(str ";; And " %) (rest steps))]
      (str/join "\n" (cons first-line rest-lines)))))

(defn generate-step-comments
  "Generate step comments for a scenario, optionally including background."
  [scenario background]
  (let [bg-comments (when background
                      (let [bg-header ";; Background:"
                            bg-steps (format-steps "Given" (:givens background))]
                        (str bg-header "\n" bg-steps "\n;;")))
        given-comments (format-steps "Given" (:givens scenario))
        when-comments (format-steps "When" (:whens scenario))
        then-comments (format-steps "Then" (:thens scenario))
        parts (remove nil? [bg-comments given-comments when-comments then-comments])]
    (str/join "\n" parts)))

(defn generate-scenario
  "Generate a (context ...) block with a pending (it ...) for a scenario."
  [scenario background]
  (let [title (:scenario scenario)
        step-comments (generate-step-comments scenario background)
        indented-comments (->> (str/split-lines step-comments)
                               (map #(str "      " %))
                               (str/join "\n"))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         indented-comments "\n"
         "      (pending \"not yet implemented\")))")))

(defn generate-spec
  "Generate a complete speclj spec file string from an IR map."
  [ir]
  (let [{:keys [source feature scenarios background]} ir
        non-wip (remove :wip scenarios)
        ns-form (generate-ns-form source)
        scenario-blocks (->> non-wip
                             (map #(generate-scenario % background))
                             (str/join "\n\n"))]
    (str ns-form "\n\n"
         "(describe \"" feature "\"\n\n"
         scenario-blocks ")\n")))

(defn- source->spec-filename
  "Convert a source filename to a spec output filename."
  [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_spec.clj")))

(defn generate-features!
  "Read .edn IR files from edn-dir and write generated spec files to output-dir."
  [edn-dir output-dir]
  (let [dir (io/file edn-dir)
        edn-files (->> (.listFiles dir)
                       (filter #(str/ends-with? (.getName %) ".edn"))
                       (sort-by #(.getName %)))]
    (io/make-parents (io/file output-dir "dummy"))
    (doseq [f edn-files]
      (let [ir (edn/read-string (slurp f))
            out-name (source->spec-filename (:source ir))
            out-path (str output-dir "/" out-name)
            spec-str (generate-spec ir)]
        (println (str "Generating " out-path " from " (.getName f)))
        (spit out-path spec-str)
        (println (str "  " (count (remove :wip (:scenarios ir))) " scenarios generated"))))))
