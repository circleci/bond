(ns bond.test.james
  (:require #?(:clj [clojure.test :refer (deftest is testing)])
            [bond.james :as bond :include-macros true])
  #?(:cljs (:require-macros [cljs.test :refer (is deftest testing)])))


(defn foo [x] (* 2 x))

(defn bar [x] (println "bar!") (* 2 x))

(deftest spy-logs-args-and-results []
  (bond/with-spy [foo]
    (foo 1)
    (foo 2)
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/calls foo)))
    ;; cljs doesn't throw artity exceptions
    #?(:clj (let [exception (is (thrown? clojure.lang.ArityException (foo 3 4)))]
              (is (= {:args [3 4] :throw exception}
                     (-> foo bond/calls last)))))))

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


(deftest iterator-style-stubbing-works []
  (bond/with-stub [foo
                   [bar [#(str "first arg is " %)
                         #(str "second arg is " %)
                         #(str "third arg is " %)]]]
    (testing "stubbing works"
      (is (nil? (foo 4)))
      (is (= "first arg is 3" (bar 3)))
      (is (= "second arg is 4" (bar 4)))
      (is (= "third arg is 5" (bar 5))))
    (testing "spying works"
      (is (= [4] (-> foo bond/calls first :args)))
      (is (= [3] (-> bar bond/calls first :args)))
      (is (= [4] (-> bar bond/calls second :args)))
      (is (= [5] (-> bar bond/calls last :args))))))
