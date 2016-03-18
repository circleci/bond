(ns bond.test.james
  (:require #?(:clj [clojure.test :refer (deftest is testing)])
            [bond.james :as bond])
  #?(:cljs (:require-macros [cljs.test :refer (is are deftest testing)])))


(defn foo [x] (* 2 x))

(defn bar [x] (println "bar!") (* 2 x))

(deftest spy-logs-args-and-results []
  (bond/with-spy [foo]
    (foo 1)
    (foo 2)
    (let [exception (is (thrown? clojure.lang.ArityException (foo 3 4)))]
      (is (= [{:args [1] :return 2}
              {:args [2] :return 4}
              {:args [3 4] :throw exception}]
             (bond/calls foo))))))

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
