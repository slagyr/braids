(ns braids.spawn-msg-spec
  (:require [speclj.core :refer :all]
            [braids.orch :as orch]))

(describe "braids.orch/spawn-msg"

  (it "generates spawn message from a spawn entry"
    (let [spawn {:project "my-proj"
                 :bead "my-proj-abc"
                 :iteration "008"
                 :channel "123456"
                 :path "/home/user/Projects/my-proj"
                 :label "project:my-proj:my-proj-abc"
                 :worker-timeout 3600}]
      (should= (str "Project: /home/user/Projects/my-proj\n"
                     "Bead: my-proj-abc\n"
                     "Iteration: 008\n"
                     "Channel: 123456")
               (orch/spawn-msg spawn))))

  (it "handles empty channel"
    (let [spawn {:project "proj"
                 :bead "proj-xyz"
                 :iteration "001"
                 :channel ""
                 :path "/tmp/proj"
                 :label "project:proj:proj-xyz"
                 :worker-timeout 3600}]
      (should= (str "Project: /tmp/proj\n"
                     "Bead: proj-xyz\n"
                     "Iteration: 001\n"
                     "Channel: ")
               (orch/spawn-msg spawn))))

  (describe "format-spawn-msg-json"

    (it "returns JSON with message and label"
      (let [spawn {:project "proj"
                   :bead "proj-abc"
                   :iteration "008"
                   :channel "123"
                   :path "/tmp/proj"
                   :label "project:proj:proj-abc"
                   :worker-timeout 3600}
            json-str (orch/format-spawn-msg-json spawn)]
        (should-contain "\"message\":" json-str)
        (should-contain "\"label\":" json-str)
        (should-contain "project:proj:proj-abc" json-str)
        (should-contain "Project: /tmp/proj" json-str)
        (should-contain "\"worker_timeout\":3600" json-str)))))
