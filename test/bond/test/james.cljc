(ns bond.test.james
  (:require #?(:clj [clojure.test :refer (deftest is testing)])
            [bond.james :as bond :include-macros true]
            [bond.test.target :as target])
  #?(:cljs (:require-macros [cljs.test :refer (is deftest testing)])))

(defn funk [& args] {:original-args args})
(defn dunk [& args] {:original-args args})

(deftest spy-logs-args-and-results
  (bond/with-spy [target/foo]
    (is (= 2 (target/foo 1)))
    (is (= 4 (target/foo 2)))
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/calls target/foo)))
    ;; cljs doesn't throw ArityException
    #?(:clj (let [exception (is (thrown? clojure.lang.ArityException (target/foo 3 4)))]
              (is (= {:args [3 4] :throw exception}
                     (-> target/foo bond/calls last)))))))

(deftest spy-can-spy-private-fns
  (bond/with-spy [target/private-foo]
    (is (= 4 (#'target/private-foo 2)))
    (is (= 6 (#'target/private-foo 3)))
    (is (= [{:args [2] :return 4}
            {:args [3] :return 6}]
           (bond/calls #'target/private-foo)))))

(deftest stub-works
  (is (= ""
         (with-out-str
           (bond/with-stub [target/bar]
             (target/bar 3))))))

(deftest stub-works-with-private-fn
  (testing "without replacement"
    (bond/with-stub [target/private-foo]
      (is (nil? (#'target/private-foo 3)))
      (is (= [3] (-> #'target/private-foo bond/calls first :args)))))
  (testing "with replacement"
    (bond/with-stub [[target/private-foo (fn [x] (* x x))]]
      (is (= 9 (#'target/private-foo 3)))
      (is (= [3] (-> #'target/private-foo bond/calls first :args))))))

(deftest stub-with-replacement-works
  (bond/with-stub [target/foo
                   [target/bar #(str "arg is " %)]]
    (testing "stubbing works"
      (is (nil? (target/foo 4)))
      (is (= "arg is 3" (target/bar 3))))
    (testing "spying works"
      (is (= [4] (-> target/foo bond/calls first :args)))
      (is (= [3] (-> target/bar bond/calls first :args))))))

(deftest iterator-style-stubbing-works
  (bond/with-stub [target/foo
                   [target/bar [#(str "first arg is " %)
                                #(str "second arg is " %)
                                #(str "third arg is " %)]]]
    (testing "stubbing works"
      (is (nil? (target/foo 4)))
      (is (= "first arg is 3" (target/bar 3)))
      (is (= "second arg is 4" (target/bar 4)))
      (is (= "third arg is 5" (target/bar 5))))
    (testing "spying works"
      (is (= [4] (-> target/foo bond/calls first :args)))
      (is (= [3] (-> target/bar bond/calls first :args)))
      (is (= [4] (-> target/bar bond/calls second :args)))
      (is (= [5] (-> target/bar bond/calls last :args))))))

(deftest stub!-complains-loudly-if-there-is-no-arglists
  (is (thrown? #?(:clj IllegalArgumentException :cljs js/Error)
               (bond/with-stub! [[target/without-arglists (constantly 42)]]
                 (is false)))))

(deftest stub!-throws-arity-exception
  (bond/with-stub! [[target/foo (constantly 9)]]
    (is (= 9 (target/foo 12)))
    (is (= [{:args [12] :return 9}] (bond/calls target/foo))))
  (bond/with-stub! [target/bar
                    target/quuk
                    [target/quux (fn [_ _ & x] x)]]
    (is (thrown? #?(:clj clojure.lang.ArityException :cljs js/Error)
                 (target/bar 1 2)))
    (is (thrown? #?(:clj clojure.lang.ArityException :cljs js/Error)
                 (target/quuk 1)))
    (is (= [6 5] (target/quux 8 7 6 5)))))

(deftest spying-entire-namespaces-works
  (bond/with-spy-ns [bond.test.target]
    (target/foo 1)
    (target/foo 2)
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/calls target/foo)))
    (is (= 0 (-> target/bar bond/calls count)))))

(deftest stubbing-entire-namespaces-works
  (testing "without replacements"
    (bond/with-stub-ns [bond.test.target]
      (is (nil? (target/foo 10)))
      (is (= [10] (-> target/foo bond/calls first :args)))))
  (testing "with replacements"
    (bond/with-stub-ns [[bond.test.target (constantly 3)]]
      (is (= 3 (target/foo 10)))
      (is (= [10] (-> target/foo bond/calls first :args))))))

(deftest local-spy-logs-args-and-results
  (bond/with-local-spy [target/foo]
    (is (= 2 (target/foo 1)))
    (is (= 4 (target/foo 2)))
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/local-calls target/foo)))
    ;; cljs doesn't throw ArityException
    #?(:clj (let [exception (is (thrown? clojure.lang.ArityException (target/foo 3 4)))]
              (is (= {:args [3 4] :throw exception}
                     (-> target/foo bond/local-calls last)))))))

(deftest local-spy-can-spy-private-fns
  (bond/with-local-spy [target/private-foo]
    (is (= 4 (#'target/private-foo 2)))
    (is (= 6 (#'target/private-foo 3)))
    (is (= [{:args [2] :return 4}
            {:args [3] :return 6}]
           (bond/local-calls #'target/private-foo)))))

(deftest local-stub-works
  (is (= ""
         (with-out-str
           (bond/with-local-stub [target/bar]
             (target/bar 3))))))

(deftest local-stub-works-with-private-fn
  (testing "without replacement"
    (bond/with-local-stub [target/private-foo]
      (is (nil? (#'target/private-foo 3)))
      (is (= [3] (-> #'target/private-foo bond/local-calls first :args)))))
  (testing "with replacement"
    (bond/with-local-stub [[target/private-foo (fn [x] (* x x))]]
      (is (= 9 (#'target/private-foo 3)))
      (is (= [3] (-> #'target/private-foo bond/local-calls first :args))))))

(deftest local-stub-with-replacement-works
  (bond/with-local-stub [target/foo
                   [target/bar #(str "arg is " %)]]
    (testing "stubbing works"
      (is (nil? (target/foo 4)))
      (is (= "arg is 3" (target/bar 3))))
    (testing "spying works"
      (is (= [4] (-> target/foo bond/local-calls first :args)))
      (is (= [3] (-> target/bar bond/local-calls first :args))))))

(deftest local-iterator-style-stubbing-works
  (bond/with-local-stub [target/foo
                   [target/bar [#(str "first arg is " %)
                                #(str "second arg is " %)
                                #(str "third arg is " %)]]]
    (testing "stubbing works"
      (is (nil? (target/foo 4)))
      (is (= "first arg is 3" (target/bar 3)))
      (is (= "second arg is 4" (target/bar 4)))
      (is (= "third arg is 5" (target/bar 5))))
    (testing "spying works"
      (is (= [4] (-> target/foo bond/local-calls first :args)))
      (is (= [3] (-> target/bar bond/local-calls first :args)))
      (is (= [4] (-> target/bar bond/local-calls second :args)))
      (is (= [5] (-> target/bar bond/local-calls last :args))))))

(deftest local-stub!-complains-loudly-if-there-is-no-arglists
  (is (thrown? #?(:clj IllegalArgumentException :cljs js/Error)
               (bond/with-local-stub! [[target/without-arglists (constantly 42)]]
                 (is false)))))

(deftest local-stub!-throws-arity-exception
  (bond/with-local-stub! [[target/foo (constantly 9)]]
    (is (= 9 (target/foo 12)))
    (is (= [{:args [12] :return 9}] (bond/local-calls target/foo))))
  (bond/with-local-stub! [target/bar
                    target/quuk
                    [target/quux (fn [_ _ & x] x)]]
    (is (thrown? #?(:clj clojure.lang.ArityException :cljs js/Error)
                 (target/bar 1 2)))
    (is (thrown? #?(:clj clojure.lang.ArityException :cljs js/Error)
                 (target/quuk 1)))
    (is (= [6 5] (target/quux 8 7 6 5)))))

(deftest local-spying-entire-namespaces-works
  (bond/with-local-spy-ns [bond.test.target]
    (target/foo 1)
    (target/foo 2)
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/local-calls target/foo)))
    (is (= 0 (-> target/bar bond/local-calls count)))))

(deftest test-with-dynamic-redefs
  (dotimes [i 100]
    (let [f1 (future (bond/with-dynamic-redefs [funk (constantly -100)]
                       (Thread/sleep (rand-int 100))
                       {:100 (funk) :t (.getName (Thread/currentThread))}))

          f2 (future (bond/with-dynamic-redefs [funk (constantly -200)]
                       (Thread/sleep (rand-int 100))
                       {:200 (funk 9) :t (.getName (Thread/currentThread))}))
          f3 (future (do
                       (Thread/sleep (rand-int 100))
                       {:orig (funk 9) :t (.getName (Thread/currentThread))}))]
      (is (and (= (:100 @f1) -100)
               (= (:200 @f2) -200)
               (= (:orig @f3) {:original-args '(9)}))))))

(deftest thread-safe-stub!
  (let [p (promise)
        f (future
            (bond/with-local-stub! [dunk [funk (constantly -1)]]
              (while (not (realized? p))
                (Thread/sleep 10))
              {:result [(funk) (dunk)]
               :calls [(bond/local-calls funk)
                       (bond/local-calls dunk)]}))]
    (is (= (funk 9) {:original-args '(9)}))
    (is (= (dunk 9) {:original-args '(9)}))
    (deliver p true)
    (is (= (:result @f) [-1 nil]))
    (is (= [[{:args nil :return -1}]
            [{:args nil :return nil}]]
           (:calls @f)))
    (is (= [] (bond/local-calls dunk)))
    (is (= [] (bond/local-calls funk))))

  (let [p (promise)
        f (future
            (bond/with-local-stub! [[funk (constantly -1)]]
              (while (not (realized? p))
                (Thread/sleep 10))
              {:result [(funk) (dunk)]
               :calls [(bond/local-calls funk)
                       (bond/local-calls dunk)]}))]
    (is (= (funk 9) {:original-args '(9)}))
    (is (= (dunk 9) {:original-args '(9)}))
    (deliver p true)
    (is (= (:result @f) [-1 {:original-args nil}]))
    (is (= [[{:args nil :return -1}]
            []]
           (:calls @f)))
    (is (= [] (bond/local-calls dunk)))
    (is (= [] (bond/local-calls funk)))))

(deftest thread-safe-stub
  (let [p (promise)
        f (future
            (bond/with-local-stub [dunk [funk (constantly -1)]]
              (while (not (realized? p))
                (Thread/sleep 10))
              {:result [(funk) (dunk)]
               :calls [(bond/local-calls funk)
                       (bond/local-calls dunk)]}))]
    (is (= (funk 9) {:original-args '(9)}))
    (is (= (dunk 9) {:original-args '(9)}))
    (deliver p true)
    (is (= (:result @f) [-1 nil]))
    (is (= [[{:args nil :return -1}]
            [{:args nil :return nil}]]
           (:calls @f)))
    (is (= [] (bond/local-calls dunk)))
    (is (= [] (bond/local-calls funk))))

  (let [p (promise)
        f (future
            (bond/with-local-stub [[funk (constantly -1)]]
              (while (not (realized? p))
                (Thread/sleep 10))
              {:result [(funk) (dunk)]
               :calls [(bond/local-calls funk)
                       (bond/local-calls dunk)]}))]
    (is (= (funk 9) {:original-args '(9)}))
    (is (= (dunk 9) {:original-args '(9)}))
    (deliver p true)
    (is (= (:result @f) [-1 {:original-args nil}]))
    (is (= [[{:args nil :return -1}]
            []]
           (:calls @f)))
    (is (= [] (bond/local-calls dunk)))
    (is (= [] (bond/local-calls funk)))))

(deftest thread-spy
  (let [p (promise)
        f (future
            (bond/with-local-spy [dunk funk]
              (while (not (realized? p))
                (Thread/sleep 10))
              {:result [(funk) (dunk)]
               :calls [(bond/local-calls funk)
                       (bond/local-calls dunk)]}))]
    (is (= (funk 9) {:original-args '(9)}))
    (is (= (dunk 9) {:original-args '(9)}))
    (deliver p true)
    (is (= (:result @f) [{:original-args nil} {:original-args nil}]))
    (is (= [[{:args nil :return {:original-args nil}}]
            [{:args nil :return {:original-args nil}}]]
           (:calls @f)))
    (is (= [] (bond/local-calls dunk)))
    (is (= [] (bond/local-calls funk))))

  (let [p (promise)
        f (future
            (bond/with-local-spy [funk]
              (while (not (realized? p))
                (Thread/sleep 10))
              {:result [(funk) (dunk)]
               :calls [(bond/local-calls funk)
                       (bond/local-calls dunk)]}))]
    (is (= (funk 9) {:original-args '(9)}))
    (is (= (dunk 9) {:original-args '(9)}))
    (deliver p true)
    (is (= (:result @f) [{:original-args nil} {:original-args nil}]))
    (is (= [[{:args nil :return {:original-args nil}}]
            []]
           (:calls @f)))
    (is (= [] (bond/local-calls dunk)))
    (is (= [] (bond/local-calls funk)))))

(deftest test-calls
  (is (fn? bond/local-calls))
  (bond/with-local-spy [funk dunk]
    (is (= (mapv bond/local-calls [funk dunk])
           [(bond/local-calls funk)
            (bond/local-calls dunk)]
           (mapv (fn [f] (bond/local-calls f))
                 [funk dunk])))))
