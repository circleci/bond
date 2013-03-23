(ns bond.test.james
  (:require [clojure.test :refer (deftest is)]
            [bond.james :as bond]))


(defn foo [x] (* 2 x))

(defn bar [x] (println "bar!") (* 2 x))

(defn baz [x] (throw (Exception. (str x))))

(deftest spy-works
  (bond/with-spy [foo]
    (foo 1)
    (foo 2)
    (is (= 2 (-> foo bond/calls count)))
    (is (= {:args [1] :return 2} (-> foo bond/calls first)))
    (is (= {:args [2] :return 4} (-> foo bond/calls second)))))

(deftest spy-catches
  (bond/with-spy [baz]
    (is (thrown? Exception (baz :a)))
    (is (= Exception (type (-> baz bond/calls first :throw))))))

(deftest stub-works
  (is (= ""
         (with-out-str
           (bond/with-stub [bar]
             (bar 3))))))

(deftest stub-maps
  (bond/with-stub {foo 1}
    (is (= 1 (foo 3)))
    (is (= {:args [3] :return 1} (-> foo bond/calls first)))))