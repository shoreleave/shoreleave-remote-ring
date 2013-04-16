(ns
  ^{:doc "Server-side RPC support for use with Shoreleave,
         that works at the Ring level"}
  shoreleave.middleware.rpc
  (:require [shoreleave.server-helpers :refer [safe-read]]))

;; By default, remotes will hit `/_shoreleave` as their endpoint,
;; but this can be overriden in the middleware hookup itself.
;; For example: `(shoreleave.middleware.rpc/wrap-rpc "/_a_different_endpoint")`
(def default-remote-uri "/_shoreleave")

;; The remotes get collected in a hashmap: `{remote-name-kw remote-fn, ...}`
(def remotes (atom {}))

(defn add-remote [key func]
  (swap! remotes assoc key func))

(defmacro defremote
  "Same as defn, but also registers the defined function as a remote.
The name of the remote is the same as the function's name by default;
You can optionally specify a different name by adding :remote-name
metadata to the function name, e.g.:

  (defremote ^{:remote-name :your-fn} my-fn [] ...)"
  [& [name :as body]]
  `(do
     (defn ~@body)
     (add-remote
       (keyword (or (-> (var ~name) meta :remote-name)
                    '~name))
       ~name)
     (var ~name)))

(defn remote-ns
  "Exposes an entire namespace as a remote API
  and optionally aliases for use on the client side.

  For example: `(remote-ns 'baseline.controllers.api :as \"api\")`
  will allow you to call your client side API calls to look like `api/some-fn-there`"
  [namesp-sym & opts]
  (let [{:keys [as]} (apply hash-map opts)
        namesp (try
                 (require namesp-sym)
                 (find-ns namesp-sym)
                 (catch Exception e
                   (throw (Exception. (str "Could not locate a namespace when aliasing remotes: " namesp-sym))
                          e)))
        public-fns (ns-publics namesp)]
    (doseq [[fn-name fn-var] public-fns]
      (when (fn? (var-get fn-var))
        (add-remote (keyword (str (or as namesp-sym) "/" fn-name)) fn-var)))))

(defn call-remote
  [remote-key params]
  (if-let [func (@remotes remote-key)]
    (let [result (apply func params)]
      {:status 202
       :headers {"Content-Type" "application/edn; charset=utf-8"}
       :body (pr-str result)})
    {:status 404
     :body "Remote not found."}))

(def ^{:dynamic true
       :doc "Reference to current request, accessible in defremote through current-request"}
  *request* nil)

(defn current-request
  "Retrieve current request, providing access from defremote."
  []
  *request*)

(defn handle-rpc
  [{{:keys [params remote]} :params :as request}]
  (binding [*request* request]
    (call-remote (keyword remote) (safe-read params))))

(defn wrap-rpc
  "Top-level Ring middleware to enable Shoreleave RPC calls"
  ([app] (wrap-rpc app default-remote-uri))
  ([app remote-uri]
    (fn [{:keys [request-method uri] :as request}]
      (if (and (= :post request-method) (= remote-uri uri))
        (handle-rpc request)
        (app request)))))

