Bond [![CircleCI Status](https://circleci.com/gh/circleci/bond.png?style=badge)](https://circleci.com/gh/circleci/bond) [![codecov.io](https://codecov.io/github/circleci/bond/coverage.svg?branch=master)](https://codecov.io/github/circleci/bond?branch=master)
====

Bond is a spying and stubbing library, primarily intended for tests.

```clojure

[circleci/bond "0.2.9"]
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

Bond also provides with-stub. It works the same as with-spy, but redefines the fn to return `(constantly nil)` (default), while also spying on it. You can specify an arbitrary function instead of the default `(constantly nil)` by providing a `[fn-var replacement-fn]` vector in place of just the fn name:

```clojure
(ns test.foo
  (:require [bond.james :as bond :refer [with-stub]))

(defn foo [x] ...)

(defn bar [y] ...)

(deftest foo-is-called
  (with-stub [[foo (fn [x] "foo")]
              [bar (fn [y] "bar")]]
    (is (= ["foo" "bar"] [(foo 1) (bar 2)]))))
    
(deftest consecutive-stubbing
  (with-stub [[foo [(fn [x] "foo1") 
                    (fn [x] "foo2") 
                    (fn [x] "foo3")]]
              [bar (fn [y] "bar")]]
    (is (= ["foo1" "foo2" "foo3" "bar"] [(foo 1) (foo 1) (foo 1) (bar 2)]))))
    
```

License
-------

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
