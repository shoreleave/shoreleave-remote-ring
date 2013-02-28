(ns shoreleave.server-helpers
  "Simple functions to smooth over server idioms"
  (:require [clojure.tools.reader.edn :as edn]))

;; This causes precious-file.txt to be created if it doesn't
;; exist, or if it does exist, its contents will be erased (given
;; appropriate JVM sandboxing permissions, and underlying OS file
;; permissions).
;`(old-safe-read "#java.io.FileWriter[\"precious-file.txt\"]")`

;    (defn old-safe-read [s]
;      (binding [*read-eval* false]
;        (read-string s)))

(defn safe-read
  "This is a data-only // edn-only read.
  It takes a string of data (s) and returns Clojure/EDN data"
  [s]
  (edn/read-string s))

