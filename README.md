# Q

Q provides gadgets for spies, and aims to make testing involving complex
dependencies easier. It provides helper functions for creating fakes, stubs and
spies - as well as ways to inspect what was called on them after-the-fact.

```clojure

[q "0.1.0"]
```

```clojure

(ns test.foo
  (:require [q.gadgets :as q]))

(defn foo [x] ...)
(defn bar [y] ...)

(deftest foo-is-called
  (q/with-spy [foo bar]
    (bar)
    (is (q/called-once bar))))
```

Q is based on [Bond](https://github.com/circleci/bond), but with a wider variety
of higher-level functions available. Much of the functionality is modelled on
[SinonJS](http://sinonjs.org/)

## Spies

### (spy fn) => spied-fn
Takes a function `fn`, and returns a `spied-fn` that has the same behaviour
except it records information about its invocations.

### (with-spy fns & body)
Takes a vector of function vars `fns` and executes `body` with those functions
replaced with spies.

### (calls spied-fn)
Returns a vector of function invocation maps, representing each call of `fn`.
Each map will contain `:args` a vector of the arguments it was called with, and
either `:return` for the return value or `:throw` for the exception thrown.

### (called spied-fn)
Returns the number of times `fn` was called as an integer.

### (called-once spied-fn)
Returns true if the function was called once

## Stubs

### (with-stubs fns & body)
When `fns` is a vector, accepts a list of function vars and executes `body` with
those functions replaced with spies that return nil.  
When `fns` is a map, accepts a mapping of function vars to return values, and
executes `body` with those functions replaced with spies that return the desired
return value.

### (with-fakes fns & body)
Accepts a map of function vars to functions, and executes `body` with the
original functions spied and replaced with the supplied implementations.


## Licence
Released under the
[MIT License](http://www.opensource.org/licenses/mit-license.php)