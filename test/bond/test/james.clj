(ns bond.test.james
  (:require [clojure.test :refer (deftest is testing)]
            [bond.james :as bond]))


(defn foo [x] (* 2 x))

(defn bar [x] (* 2 x))

(defn qux [x y]
  (bar (+ x (foo y))))

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


(deftest with-trap-works
  (testing "the result is returned when the trap is not triggered"
    (is (= 6 (bond/with-trap [foo]
               (bar 3)))))
  (testing "the macro works when there are no fns supplied"
    (bond/with-trap []
      (+ 1 2)))
  (testing "the trap works when the fn is called"
    (let [e (is (thrown-with-msg?
                  clojure.lang.ExceptionInfo #"fn was called"
                  (bond/with-trap [bar]
                    (qux 3 7))))]
      (testing "and args are recorded"
        (is (= {:calls [{:args [17] :return nil}]}
               (ex-data e)))))))
