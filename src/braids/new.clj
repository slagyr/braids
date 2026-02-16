(ns braids.new
  (:require [clojure.string :as str]
            [braids.project-config :as pc]))

(def slug-pattern #"^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")

(defn validate-slug [slug]
  (cond
    (or (nil? slug) (str/blank? slug))
    ["Slug is required"]

    (not (re-matches slug-pattern slug))
    [(str "Invalid slug: '" slug "' — must be lowercase alphanumeric with hyphens, no leading/trailing hyphens")]

    :else []))

(defn validate-new-params [{:keys [slug name goal]}]
  (concat
    (validate-slug slug)
    (when-not name ["Name is required"])
    (when-not goal ["Goal is required"])))

(defn build-project-config [{:keys [name priority autonomy checkin channel
                                     max-workers worker-timeout]}]
  {:name name
   :status :active
   :priority (or priority :normal)
   :autonomy (or autonomy :full)
   :checkin (or checkin :on-demand)
   :channel channel
   :max-workers (or max-workers 1)
   :worker-timeout (or worker-timeout 3600)
   :notifications pc/default-notifications})

(defn build-registry-entry [{:keys [slug priority]} path]
  {:slug slug
   :status :active
   :priority (or priority :normal)
   :path path})

(defn build-iteration-content []
  {:number 1
   :status :planning
   :stories []})

(defn build-agents-md
  ([] (build-agents-md {}))
  ([{:keys [goal guardrails]}]
   (str "# AGENTS.md\n\n"
        "This project is managed by the **braids** skill. Config: `.braids/config.edn`. Goals and guardrails live in this file.\n\n"
        "## Goal\n\n"
        (or goal "TODO: Describe the project goal.") "\n\n"
        "## Guardrails\n\n"
        (if (seq guardrails)
          (str/join "\n" (map #(str "- " %) guardrails))
          "- None yet")
        "\n\n"
        "## How to Work on This Project\n\n"
        "**If you were spawned by the orchestrator** (your task message includes `Project:` and `Bead:` fields):\n"
        "→ Follow `~/.openclaw/skills/braids/references/worker.md`\n\n"
        "**If you're here on your own** (manual session, human asked you to help, etc.):\n"
        "1. Read `.braids/config.edn` — understand the project settings\n"
        "2. Read this file (AGENTS.md) — for goals, guardrails, and conventions\n"
        "3. Find the active iteration: look in `.braids/iterations/*/ITERATION.md` for `Status: active`\n"
        "4. Run `bd ready` to see available work\n"
        "5. Pick a bead, then follow the worker workflow: `~/.openclaw/skills/braids/references/worker.md`\n\n"
        "## Quick Reference\n\n"
        "```bash\n"
        "bd ready              # List unblocked tasks\n"
        "bd show <id>          # View task details\n"
        "bd update <id> --claim  # Claim a task\n"
        "bd update <id> -s closed  # Close completed task\n"
        "bd list               # List all tasks\n"
        "bd dep list <id>      # List dependencies\n"
        "```\n\n"
        "## Session Completion\n\n"
        "Work is NOT complete until `git push` succeeds.\n\n"
        "```bash\n"
        "git add -A && git commit -m \"<summary> (<bead-id>)\"\n"
        "git pull --rebase\n"
        "bd sync\n"
        "git push\n"
        "```\n")))

(defn add-to-registry [registry entry]
  (let [existing-slugs (set (map :slug (:projects registry)))]
    (when (contains? existing-slugs (:slug entry))
      (throw (Exception. (str "Project '" (:slug entry) "' already exists in registry"))))
    (update registry :projects conj entry)))
