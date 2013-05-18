(ns q.gadgets-spec
  (:require [q.gadgets :as q]
            [speclj.core :refer :all]))

(defn foo [] :foo)
(defn bar [a b c] [c b a])
(defn err [a] (throw (Exception. "boo")))

(describe "q"

  (describe "spy"

    (with spied-foo (q/spy foo))
    (with spied-bar (q/spy bar))
    (with spied-err (q/spy err))

    (it "behaves as the normal function"
      (should= :foo (@spied-foo)))

    (it "retains arity"
      (should-throw (@spied-foo :arg)))

    (it "records return value"
      (@spied-foo)
      (should= :foo (-> @spied-foo q/calls first :return)))

    (it "records arguments as well"
      (@spied-bar :a :b :c)
      (let [call (-> @spied-bar q/calls first)]
        (should= [:c :b :a] (:return call))
        (should= [:a :b :c] (:args call))))

    (it "records thrown exceptions"
      (try
        (@spied-err :a)
        (catch Exception e))
      (let [call (-> @spied-err q/calls first)]
        (should= Exception (type (:throw call)))
        (should= [:a] (:args call))))

    (it "records multiple invocations"
      (@spied-bar :a :b :c)
      (@spied-bar :x :y :z)
      (should= {:args [:a :b :c] :return [:c :b :a]}
               (-> @spied-bar q/calls first))
      (should= {:args [:x :y :z] :return [:z :y :x]}
               (-> @spied-bar q/calls second)))

    )

  )
