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
               (-> @spied-bar q/calls second))))

  (describe "with-spies"

    (it "wraps functions in spies"
      (q/with-spies [foo bar]
        (foo)
        (bar :a :b :c)
        (should= 1 (-> foo q/calls count))
        (should= 1 (-> bar q/calls count)))))

  (describe "spy call helpers"

    (around [it]
      (q/with-spies [foo bar err]
        (it)))

    (describe "calls"
      (it "gives a list of call maps"
        (bar :a :b :c)
        (bar :x :y :z)
        (let [calls (q/calls bar)]
          (should= 2 (count calls))
          (should= {:args [:a :b :c] :return [:c :b :a]} (first calls))
          (should= {:args [:x :y :z] :return [:z :y :x]} (second calls)))))

    (describe "called?"
      (it "returns false if not called"
        (should-not (q/called? foo)))

      (it "returns true if called"
        (foo)
        (should (q/called? foo))))

    (describe "called-once?"
      (it "returns false if not called"
        (should-not (q/called-once? foo)))

      (it "returns true if called once"
        (foo)
        (should (q/called-once? foo)))

      (it "returns false if called more than once"
        (foo)
        (foo)
        (should-not (q/called-once? foo))))

    (describe "call-count"
      (it "returns the number of times called"
        (should= 0 (q/call-count foo))
        (foo)
        (should= 1 (q/call-count foo))
        (foo)
        (should= 2 (q/call-count foo))
        (foo)
        (foo)
        (foo)
        (should= 5 (q/call-count foo))))

    (describe "call"
      (it "returns the call map for the nth call"
        (bar :a :b :c)
        (bar :x :y :z)
        (should= {:args [:a :b :c] :return [:c :b :a]} (q/call bar 0))
        (should= {:args [:x :y :z] :return [:z :y :x]} (q/call bar 1))))


    )

  (describe "with-stub"

    (it "uses [vector] make spies that return nil"
      (q/with-stubs [foo bar err]
        (let [err-result (err)]
          (should= nil err-result))
        (should= 1 (-> err q/calls count))))

    (it "uses {map} make spies that return canned values"
      (q/with-stubs {err :err}
        (let [err-result (err)]
          (should= :err err-result))
        (should= 1 (-> err q/calls count))))

    )

  )
