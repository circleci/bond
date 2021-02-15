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

  `vargs` should be a vector of args [args-first-call args-second-call ...] to allow for the checking of multiple calls of `f`."
  [f vargs]
  (= vargs (->> f bond/calls (mapv :args))))
