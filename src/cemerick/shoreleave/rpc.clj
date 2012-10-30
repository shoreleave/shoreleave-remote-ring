(ns ^{:doc "Server-side RPC support for use with shoreleave (and maybe fetch?).
Mostly copied from https://github.com/shoreleave/shoreleave-remote-noir;
changed to eliminate the noir-isms..."}
     cemerick.shoreleave.rpc)

(def default-remote-uri "/_fetch")
(def remotes (atom {}))

(defn add-remote [key func]
  (swap! remotes assoc key func))

(defn safe-read [s]
  ;; can we please have a civilization!?
  (binding [*read-eval* false]
    (read-string s)))

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

(defn- map-w-session? [x]
  (and (instance? clojure.lang.PersistentArrayMap x)
       (contains? x :session)
       (contains? x :result)))

(defn call-remote
  [remote-key params request pass-request?]
  (if-let [func (@remotes remote-key)]
    (let [out (apply func (if pass-request?
                            (concat params [:request request])
                            params))
          result (if (map-w-session? out) (:result out) out)
          session (if (map-w-session? out) (:session out) (:session request))]
      {:status 202
       :headers {"Content-Type" "application/clojure; charset=utf-8"}
       :session session
       :body (pr-str result)})
    {:status 404}))

(defn handle-rpc
  [{{:keys [params remote]} :params :as request} pass-request?]
  (call-remote (keyword remote) (safe-read params) request pass-request?))

(defn wrap-rpc
  [app & {:keys [remote-uri pass-request?]
          :or {remote-uri default-remote-uri}}]
  (fn [{:keys [request-method uri] :as request}]
    (if (and (= :post request-method) (= remote-uri uri))
      (handle-rpc request pass-request?)
      (app request))))
