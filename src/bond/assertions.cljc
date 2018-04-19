(ns bond.assertions
  (:require [bond.james :as bond])
  #?(:cljs (:require-macros [bond.james :as bond])))

(defmacro called?
  "An assertion to check if the spy was called at least 1 time."
  [f & body]
  `(bond/with-spy [~f]
     (do ~@body)
     (pos? (-> ~f bond/calls count))))

(defmacro called-times?
  "An assertion to check if the spy was called number of `times`."
  [f times & body]
  `(bond/with-spy [~f]
     (do ~@body)
     (= ~times (-> ~f bond/calls count))))

(defmacro called-with-args?
  "An assertion to check if the spy was called with `vargs`.

  `vargs` should be a vector of args [args-first-call args-second-call ...] to allow for the checking of multiple calls of the spy."
  [f vargs & body]
  `(bond/with-spy [~f]
     (do ~@body)
     (= ~vargs (mapv :args (-> ~f bond/calls)))))
