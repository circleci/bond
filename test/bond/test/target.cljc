(ns bond.test.target)

(defn foo
  [x]
  (* 2 x))

(defn bar
  [x]
  (println "bar!") (* 2 x))

(defmacro baz
  [x]
  `(* ~x 2))
