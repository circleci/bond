Bond
====

Bond is a spying and stubbing library, primarily intended for tests.

```clojure

[bond "0.2.5"]
```

```clojure

(ns test.foo
  (:require [bond.james :as bond :refer [with-spy]))

(defn foo [x] ...)

(defn bar [y]
   (foo y))

(deftest foo-is-called
  (with-spy [foo]
    (bar 2)
    (is (= 1 (-> foo bond/calls count)))))
```

Bond provides one main macro, with-spy. It takes a vector of defn vars (vars that resolve to fns). Each var will be redef'd, wrapping the fn to track arguments and call counts. At any point during the scope, you can call (bond/calls f), where f is a spied fn. `calls` returns a seq of maps, one for each call to f. Each map contains the keys :args, a seq of args the fn was called with, and one of :return or :throw.

Bond also provides with-stub. It works the same as with-spy, but redefines the fn to return (constantly nil), while also spying on it.

License
-------

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
