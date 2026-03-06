;; Step definitions for example features.
;; Uses an atom to track state across steps within a scenario.
(let [state (atom {})]
  {"Given the system is ready"    (fn [] (swap! state assoc :ready true))
   "And the user is logged in"    (fn [] (swap! state assoc :logged-in true))
   "When the action is performed" (fn [] (swap! state assoc :acted true))
   "But the retry flag is set"    (fn [] (swap! state assoc :retry true))
   "Then the result is successful" (fn [] (assert (:ready @state)))
   "And the audit log is updated" (fn [] (assert (:acted @state)))

   ;; Background steps
   "Given a fresh context"         (fn [] (reset! state {:initialized true}))
   "When the first action runs"    (fn [] (swap! state assoc :first true))
   "When the second action runs"   (fn [] (swap! state assoc :second true))
   "Then the context was initialized" (fn [] (assert (:initialized @state)))

   ;; Capture group steps (regex patterns as strings)
   "Given (\\d+) items in the cart"       (fn [n] (swap! state assoc :cart-count (parse-long n)))
   "Then the cart has (\\d+) items"        (fn [n] (assert (= (parse-long n) (:cart-count @state))))
   "Given a user named \"([^\"]+)\""      (fn [name] (swap! state assoc :user-name name))
   "Then the greeting is \"([^\"]+)\""    (fn [expected] (assert (= expected (str "Hello, " (:user-name @state)))))})
