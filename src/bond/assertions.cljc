(ns bond.assertions
  (:require [bond.james :as bond])
  #?(:cljs (:require-macros [bond.james :as bond])))

(defmacro called?
  "TODO: write better documentation"
  [f & body]
  `(bond/with-spy [~f]
     (do ~@body)
     (pos? (-> ~f bond/calls count))))

(defmacro called-times?
  "TODO: write better documentation"
  [f times & body]
  `(bond/with-spy [~f]
     (do ~@body)
     (= ~times (-> ~f bond/calls count))))

(defmacro called-with-args?
  "TODO: write better documentation"
  [f vargs & body]
  `(bond/with-spy [~f]
     (do ~@body)
     (= ~vargs (mapv :args (-> ~f bond/calls)))))
