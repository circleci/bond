(ns bond.test.james
  (:require [clojure.test :refer (deftest is testing)]
            [bond.james :as bond]))


(defn foo [x] (* 2 x))

(defn bar [x] (println "bar!") (* 2 x))

(deftest spy-works []
  (bond/with-spy [foo]
    (foo 1)
    (foo 2)
    (is (= 2 (-> foo bond/calls count)))))

(deftest stub-works []
  (is (= ""
         (with-out-str
           (bond/with-stub [bar]
             (bar 3))))))

(deftest stub-with-replacement-works []
  (bond/with-stub [foo
                   [bar #(str "arg is " %)]]
    (testing "stubbing works"
      (is (nil? (foo 4)))
      (is (= "arg is 3" (bar 3))))
    (testing "spying works"
      (is (= [4] (-> foo bond/calls first :args)))
      (is (= [3] (-> bar bond/calls first :args))))))
