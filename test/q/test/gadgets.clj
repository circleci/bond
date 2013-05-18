(ns q.test.gadgets
  (:require [clojure.test :refer (deftest is)]
            [q.gadgets :as q]))


(defn foo [x] (* 2 x))

(defn bar [x] (println "bar!") (* 2 x))

(defn baz [x] (throw (Exception. (str x))))

(deftest spy-works
  (q/with-spy [foo]
    (foo 1)
    (foo 2)
    (is (= 2 (-> foo q/calls count)))
    (is (= {:args [1] :return 2} (-> foo q/calls first)))
    (is (= {:args [2] :return 4} (-> foo q/calls second)))))

(deftest spy-catches
  (q/with-spy [baz]
    (is (thrown? Exception (baz :a)))
    (is (= Exception (type (-> baz q/calls first :throw))))))

(deftest stub-works
  (is (= ""
         (with-out-str
           (q/with-stub [bar]
             (bar 3))))))

(deftest stub-maps
  (q/with-stub {foo 1}
    (is (= 1 (foo 3)))
    (is (= {:args [3] :return 1} (-> foo q/calls first)))))