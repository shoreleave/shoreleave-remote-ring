# shoreleave-remote-ring

[shoreleave-remote-ring](http://github.com/cemerick/shoreleave-remote-ring) is
an alternative implementation of
[shoreleave-remote-noir](https://github.com/shoreleave/shoreleave-remote-noir),
aiming to be a well-behaved Ring/Compojure library and middleware.

## "Installation"

shoreleave-remote-ring is available in Clojars.  Add it to your Maven project's
`pom.xml`:

```xml
<dependency>
  <groupId>com.cemerick</groupId>
  <artifactId>shoreleave-remote-ring</artifactId>
  <version>0.0.1</version>
</dependency>
```

or your leiningen project.clj:

```clojure
[com.cemerick/shoreleave-remote-ring "0.0.2"]
```

## Usage

First, you need to be using (or, aiming to use)
[shoreleave-remote](https://github.com/shoreleave/shoreleave-remote), a snazzy
ClojureScript library implementing a variety of remote call operations.

shoreleave-remote-ring provides the server-side implementation for the HTTP RPC
option (implemented in `shoreleave.remotes.http-rpc`).  In general, it supports
all the same things that shoreleave-remote-noir does, but:

1. It doesn't depend upon Noir.
2. It is just a couple of functions, including a bit of middleware so you can
   easily (and functionally) put the remoting support anywhere in your
Ring/Compojure stack.
3. It elides some wonky features (looking at you,
   `noir.shoreleave.rpc/remote-ns`!).
4. Its `defremote` macro is a more faithful drop-in `defn` replacement, and
   offers some additional knobs.

(FYI, this library and shoreleave-remote-noir may or may not be compatible with
[Fetch](https://github.com/ibdknox/fetch), the library that apparently inspired
the genesis of shoreleave-remote.)

### 1. Define your remotes (server-side, in Clojure)

```clojure
(ns ...
  (:require [port79.rpc :refer (defremote)]))

(defremote remote-fn [arg1 arg2 ...] ...)
```

### 2. Mix in the `wrap-rpc` middleware (still server-side, still Clojure)

With bare Ring:

```clojure
(ns ...
  (:require [port79.rpc :as rpc])
  (:use [ring.middleware params
                         keyword-params
                         nested-params
                         ...]))

(def app (-> #'your-top-level-handler
           rpc/wrap-rpc
           wrap-keyword-params
           wrap-nested-params
           wrap-params
           ...))
```

…or, if you're using Compojure:

```clojure
(ns ...
  (:require [port79.rpc :as rpc]
            [compojure.handler :as handler]))

(def app (-> #'your-top-level-handler
           rpc/wrap-rpc
           handler/site
           ...))
```

### 3. Call your remotes (client-side now, ClojureScript)

```clojure
(ns ...
  (:require-macros [shoreleave.remotes.macros :as macros])
  (:require [shoreleave.remotes.http-rpc :as rpc]))

(rpc/remote-callback :remote-fn [arg1 arg2 ...] #(js/alert %))
```

Of course, you can use the shoreleave macro (`macros/rpc` given the `:require`
above) if you prefer.

### 4. Give your functions whatever names you like (server-side again, Clojure)

Note the correspondence between the name of the remote function (`remote-fn`)
and the name passed to `remote-callback`).  This is convenient, but will cause
confusion and perhaps collisions in large enough codebases.  The escape hatch
is that `defremote` can bind a Clojure function to any remote name you like,
with just a bit of metadata:

```clojure
(ns ...
  (:require [port79.rpc :refer (defremote)]))

(defremote ^{:remote-name :validations/is-email?} remote-fn
  [arg1 arg2 ...]
  ...)
```

Now, `remote-fn` is not the remote name of this function;
`validations/is-email?` is:

```clojure
(ns ...
  (:require-macros [shoreleave.remotes.macros :as macros])
  (:require [shoreleave.remotes.http-rpc :as rpc]))

(rpc/remote-callback :validations/is-email? ["foo@bar.com"] #(js/alert %))
```

The value of `:remote-name` can be any string, keyword, or symbol, but the
convention of namespaced keywords (or symbols) is a good one for alleviating
issues of collision and confusion.  Auto-namespaced keywords (e.g.
`::is-email?`) can also be used so as to automatically prepend the name of the
current namespace (though the notion of exposing the names of server-side
namespaces might rightly worry you).

## Need Help?

Ping `cemerick` on freenode irc or twitter if you have questions or would like
to contribute patches.

## License

Copyright © 2012 Chas Emerick

Licensed under the EPL. (See the file epl-v10.html.)

