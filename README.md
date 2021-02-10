Bond [![CircleCI Status](https://circleci.com/gh/circleci/bond.png?style=badge)](https://circleci.com/gh/circleci/bond) [![codecov.io](https://codecov.io/github/circleci/bond/coverage.svg?branch=master)](https://codecov.io/github/circleci/bond?branch=master)
====

Bond is a spying and stubbing library, primarily intended for tests.

```clojure
[circleci/bond "0.5.0"]
```

```clojure

(ns test.foo
  (:require [bond.james :as bond :refer [with-spy]]))

(defn foo [x]
  (let [shaken (with-out-str (prn :martini))]
    [shaken])

(defn bar [y]
  (foo y))

(deftest foo-is-called
  (with-spy [foo]
    (bar 2)
    (is (= 1 (-> foo bond/calls count)))))
```

Bond provides one main macro, `with-spy`. It takes a vector of defn vars (vars that resolve to fns). Each var will be redefined for the scope of the macro, wrapping the function to track arguments and call counts. At any point during the scope, you can call `(bond/calls f)`, where `f` is a spied fn. `calls` returns a seq of maps, one for each call to `f`b. Each map contains the keys `:args`, a seq of args the fn was called with, and one of `:return` or `:throw`.

Bond also provides `with-stub!`. It works the same as `with-spy`, but redefines the function to return `(constantly nil)` (default), while also spying on it. This is generally preferable to Clojure's built-in `with-redefs` macro since it will throw an exception if the mocked function is called with the wrong number of arguments. You can specify an arbitrary function instead of the default `(constantly nil)` by providing a `[fn-var replacement-fn]` vector in place of just the fn name:

```clojure
(ns test.foo
  (:require [bond.james :as bond :refer [with-stub!]]))

(defn foo [x] ...)

(defn bar [y] ...)

(deftest foo-is-called
  (with-stub! [[foo (fn [x] "foo")]
               [bar (fn [y] "bar")]]
    (is (= ["foo" "bar"] [(foo 1) (bar 2)]))))
    
(deftest consecutive-stubbing
  (with-stub! [[foo [(fn [x] "foo1") 
                     (fn [x] "foo2") 
                     (fn [x] "foo3")]]
               [bar (fn [y] "bar")]]
    (is (= ["foo1" "foo2" "foo3" "bar"] [(foo 1) (foo 1) (foo 1) (bar 2)]))))
    
```

Private functions can also be stubbed or spyed:

``` clojure
(ns test.foo)

(defn- foo [x] ...)
```

``` clojure
(ns test.bar
  (:require [bond.james :as bond :refer [with-stub!]]
            [test.foo :as foo]))

(deftest foo-is-called
  (with-stub! [[foo/foo (fn [x] "foo")]]
    (is (= "foo" (#'foo/foo 1)))
    (is (= [1] (-> #'foo/foo bond/calls first :args)))))
```

There is also a `with-stub` macro which works like `with-stub!` but omits the argument check.

In addition to `with-spy` and `with-stub!`, Bond also provides `with-spy-ns`
and `with-stub-ns` which can spy/stub every function in a namespace in one go:

```clojure
(ns test.foo
  (:require [bond.james :as bond]
            [clojure.test :refer (deftest is)]))

(defn foo [] :foo)

(defn bar [] :bar)

(deftest you-can-stub-entire-ns
  (is (= :foo (foo)))
  (is (= :bar (bar)))
  (bond/with-stub-ns [[foo (constantly :baz)]]
    (is (= :baz (foo)))
    (is (= :baz (bar)))))
```

Releasing
---------

New git tags are automatically published to [clojars](https://clojars.org/circleci/bond).

The following should be updated on the main/master branch before tagging:

- `project.clj` - version
- `README.md` - dependency coordinates
- `CHANGELOG.md` - summary of changes

License
-------

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
