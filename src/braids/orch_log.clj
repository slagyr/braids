(ns braids.orch-log
  (:require [clojure.string :as str]))

(defn format-log-lines
  "Pure function. Given a context map and timestamp string, returns a vector of log lines.
   Context keys: :registry :configs :iterations :open-beads :ready-beads :workers :zombies :tick-result"
  [{:keys [registry configs iterations open-beads ready-beads workers zombies tick-result]} timestamp]
  (let [projects (:projects registry)
        lines (transient [(str "=== orch-run " timestamp " ===")
                          (str "Registry: " (count projects) " projects")])]
    ;; Per-project details
    (doseq [{:keys [slug]} projects]
      (let [cfg (get configs slug)
            status (or (:status cfg) :active)
            iter (get iterations slug)
            max-w (or (:max-workers cfg) 1)
            cur-w (get workers slug 0)
            beads (get open-beads slug [])]
        (if iter
          (do
            (conj! lines (str "  " slug "  status=" (name status) "  iteration=" iter "  workers=" cur-w "/" max-w))
            (if (empty? beads)
              (conj! lines (str "    (no open beads)"))
              (doseq [b beads]
                (let [bs (or (some-> (:status b) name str/lower-case) "open")]
                  (conj! lines (str "    " (:id b) "  " bs (when (:title b) (str "  " (:title b)))))))))
          (conj! lines (str "  " slug "  status=" (name status) "  no active iteration")))))
    ;; Zombies
    (doseq [z zombies]
      (conj! lines (str "  Zombie: " (:bead z) " (" (:reason z) ")")))
    ;; Decision
    (let [{:keys [action reason disable-cron spawns]} tick-result]
      (if (= "spawn" action)
        (do
          (conj! lines (str "Decision: spawn " (count spawns) " worker(s)"))
          (doseq [s spawns]
            (conj! lines (str "  Spawn: " (:bead s)))))
        (conj! lines (str "Decision: idle (" reason ")"
                          (when (some? disable-cron)
                            (str "  disable_cron=" disable-cron))))))
    (persistent! lines)))

(defn write-log!
  "Append lines to the given log file path."
  [path lines]
  (spit path (str (str/join "\n" lines) "\n") :append true))
