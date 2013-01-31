(ns shoreleave.server-helpers)

(defn safe-read [s]
  ;; can we please have a civilization!?
  (binding [*read-eval* false]
    (read-string s)))
 
