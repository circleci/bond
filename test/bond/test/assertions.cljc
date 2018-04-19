(ns bond.test.assertions
  (:require #?(:clj [clojure.test :refer (deftest is testing)])
            [bond.assertions :as assertions :include-macros true]
            [bond.test.target :as target])
  #?(:cljs (:require-macros [cljs.test :refer (is deftest testing)])))

(deftest called?-works
  (testing "a spy was called"
    (is (assertions/called? target/foo (target/foo 1)))
    (is (assertions/called? target/foo (target/foo-caller 1))))

  (testing "a spy was not called"
    (is (not (assertions/called? target/foo (target/bar 1))))))

(deftest called-times?-works
  (testing "the number of times a spy was called"
    (is (assertions/called-times? target/foo 1 (target/foo-caller 1)))
    (is (assertions/called-times? target/foo 2 (do (target/foo-caller 1)
                                                   (target/foo-caller 2)))))

  (testing "the number of times a spy was not called"
    (is (not (assertions/called-times? target/foo 2 (target/foo-caller 1))))
    (is (not (assertions/called-times? target/foo 1 (do (target/foo-caller 1)
                                                        (target/foo-caller 2)))))))

(deftest called-with-args?-works
  (testing "an assertion for calling a spy with args"
    (is (assertions/called-with-args? target/foo [[1]] (target/foo-caller 1)))
    (is (not (assertions/called-with-args? target/foo [[1]] (target/bar 1))))
    (is (not (assertions/called-with-args? target/foo [[2]] (target/foo-caller 1))))
    (is (not (assertions/called-with-args? target/foo [[1 2]] (target/foo-caller 1)))))

  (testing "an assertion for calling a spy multiple times with args"
    (is (assertions/called-with-args? target/foo [[1] [2]] (do (target/foo-caller 1)
                                                               (target/foo-caller 2))))))
