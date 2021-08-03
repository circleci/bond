(ns bond.assertions
  (:require [bond.james :as bond]))

(defn called?
  "An assertion to check if `f` was called at least 1 time."
  [f]
  (pos? (-> f bond/calls count)))

(defn called-times?
  "An assertion to check if `f` was called number of `times`."
  [f times]
  (= times (-> f bond/calls count)))

(defn called-with-args?
  "An assertion to check if `f` was called with `vargs`.

  `vargs` should be a vector of args [args-first-call args-second-call ...] to allow for the checking of multiple calls of `f`.
   Note that this method asserts about every call to `f`."
  [f vargs]
  (= vargs (->> f bond/calls (mapv :args))))

(defn called-once-with-args?
  "An assertion to check if `f` was called with `args` strictly once.
   
   `args` should be a vector/coll of args [arg1 arg2 arg3] to compare directly to the value of `:args` from `bond/calls`"
  [f args]
  (and (called-times? f 1)
       (called-with-args? f [args])))

(defn called-at-least-once-with-args?
  "An assertion to check if `f` has been called at least once with `args`.

   `args` should be a vector/coll of args [arg1 arg2 {:arg3 arg3}] to compare directly to the value of `:args` from `bond/calls`"
  [f args]
  (boolean (some (fn [call] (= (:args call) args))
                 (bond/calls f))))
