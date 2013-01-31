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

(defn call-remote
  [remote-key params]
  (if-let [func (@remotes remote-key)]
    (let [result (apply func params)]
      {:status 202
       :headers {"Content-Type" "application/edn; charset=utf-8"}
       :body (pr-str result)})
    {:status 404}))

(defn handle-rpc
  [{{:keys [params remote]} :params :as request}]
  (call-remote (keyword remote) (safe-read params)))

(defn wrap-rpc
  ([app] (wrap-rpc app default-remote-uri))
  ([app remote-uri]
    (fn [{:keys [request-method uri] :as request}]
      (if (and (= :post request-method) (= remote-uri uri))
        (handle-rpc request)
        (app request)))))

