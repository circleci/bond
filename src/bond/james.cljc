(ns bond.james)

(def ^:dynamic local-redefinitions {})
(def ^:dynamic call-tracker nil)

(defn spy
  "wrap f, returning a new fn that keeping track of its call count and arguments"
  [f]
  (let [calls (atom [])]
    (with-meta (fn [& args]
                 (try
                   (let [old-f (if (vector? f) (nth f (count @calls)) f)
                         resp (apply old-f args)]
                     (swap! calls conj {:args args
                                        :return resp})
                     resp)
                   (catch #?(:clj Exception :cljs js/Error) e
                     (swap! calls conj {:args args
                                        :throw e})
                     (throw e))))
      {::calls calls})))

(defn calls
  "Takes one arg, a fn that has previously been spied. Returns a seq
  of maps, one per call. Each map contains the keys :args and :return
  or :throw"
  [f]
  (some-> (if (var? f) @f f)
          meta
          ::calls
          deref))

(defn ns->fn-symbols
  "A utility function to get a sequence of fully-qualified symbols for all the
  functions in a namespace."
  [ns]
  (->> (ns-publics ns)
       vals
       (remove (comp :macro meta))
       (filter (comp fn? deref))
       (map #(symbol (str (:ns (meta %)))
                     (str (:name (meta %)))))))

(defmacro with-spy
  "Takes a vector of fn vars (vars that resolve to fns). Modifies the
  fn to track call counts, but does not change the fn's behavior"
  [vs & body]
  `(with-redefs ~(->> (mapcat (fn [v]
                                [v `(spy (deref ~(list 'var v)))]) vs)
                      (vec))
     (do ~@body)))

(defmacro with-spy-ns
  "Like with-spy but takes a vector of namespaces. Spies on every function in
  the namespace."
  [namespaces & body]
  `(with-spy ~(mapcat ns->fn-symbols namespaces)
     (do ~@body)))

(defmacro with-stub
  "Takes a vector of fn vars and/or [fn replacement] vectors.

  Replaces each fn with it's replacement. If a replacement is not specified for
  a fn it's replaced with one that takes any number of args and returns nil.
  Also spies the stubbed-fn"
  [vs & body]

  `(with-redefs ~(->> (mapcat (fn [v]
                                (if (vector? v)
                                  [(first v) `(spy ~(second v))]
                                  [v `(spy (constantly nil))])) vs)
                      (vec))
     ~@body))

(defn- arglist-match? [arg-count arglist]
  (let [[regular-args var-args] (split-with (complement #{'&}) arglist)]
    (if (empty? var-args)
      (= arg-count (count regular-args))
      (>= arg-count (count regular-args)))))

(defn- args-match? [arg-count arglists]
  (some (partial arglist-match? arg-count) arglists))

(defn stub! [v replacement]
  (when (empty? (:arglists (meta v)))
    (throw (new #?(:clj IllegalArgumentException :cljs js/Error)
                "stub!/with-stub! may only be used on functions which include :arglists in their metadata. Use stub/with-stub instead.")))
  (let [f (spy replacement)]
    (with-meta (fn [& args]
                 (if (args-match? (count args) (:arglists (meta v)))
                   (apply f args)
                   (throw (new #?(:clj clojure.lang.ArityException :cljs js/Error)
                               (count args) (str (:ns (meta v)) "/"
                                                 (:name (meta v)))))))
      (meta f))))

(defmacro with-stub!
  "Like with-stub, but throws an exception upon arity mismatch."
  [vs & body]
  `(with-redefs ~(->> (mapcat (fn [v]
                                (if (vector? v)
                                  [(first v) `(stub! (var ~(first v))
                                                     ~(second v))]
                                  [v `(stub! (var ~v) (constantly nil))])) vs)
                      (vec))
     ~@body))

(defmacro with-stub-ns
  "Takes a vector of namespaces and/or [namespace replacement] vectors.

  Replaces every fn in each namespace with its replacement. If a replacement is
  not specified for a namespace every fn in that namespace is replaced with
  (constantly nil). All replaced functions are also spied."
  [namespaces & body]
  `(with-stub ~(mapcat (fn [n]
                         (if (vector? n)
                           (map #(vec [% (second n)]) (ns->fn-symbols (first n)))
                           (ns->fn-symbols n)))
                       namespaces)
     (do ~@body)))

(defn current->original-definition
  [v]
  (when (var? v)
    (get (meta v) :original)))

(defn redef-binding->originals
  [x]
  (if (vector? x)
    `(current->original-definition (var ~(first x)))
    `(current->original-definition (var ~x))))

(defn redefiniton-fn
  [a-var]
  (fn [& args]
    (let [original-f (current->original-definition a-var)
          current-f (get local-redefinitions
                         a-var
                         original-f)]
      (try
        (let [result (apply current-f args)]
          (when (and (thread-bound? #'call-tracker)
                     (vector? (get @call-tracker original-f)))
            (swap! call-tracker
                   update
                   original-f
                   (fnil conj [])
                   {:args args
                    :return result}))
          result)
        (catch #?(:clj Exception :cljs js/Error) e
          (when (and (thread-bound? #'call-tracker)
                     (vector? (get @call-tracker original-f)))
            (swap! call-tracker
                   update
                   original-f
                   (fnil conj [])
                   {:args args
                    :throw e}))
          (throw e))))))

(defn dynamic-redefs
  [vars func]
  (let [un-redefs (remove #(:already-bound? (meta %)) vars)]
    (doseq [a-var un-redefs]
      (locking a-var
        (when-not (:already-bound? (meta a-var))
          (let [old-val (.getRawRoot ^clojure.lang.Var a-var)
                metadata {:already-bound? true
                          :original old-val}]
            (.bindRoot ^clojure.lang.Var a-var
                       (with-meta (redefiniton-fn a-var) metadata))
            (alter-meta! a-var
                         (fn [m]
                           (merge m metadata))))))))
  (func))

(defn xs->map
  [xs]
  (persistent!
   (reduce (fn [acc [k v]] (assoc! acc `(var ~k) v))
           (transient {})
           (partition 2 xs))))

(defmacro with-dynamic-redefs
  [bindings & body]
  (let [map-bindings (xs->map bindings)]
    `(let [old-rebindings# local-redefinitions]
       (binding [local-redefinitions (merge old-rebindings# ~map-bindings)]
         (dynamic-redefs ~(vec (keys map-bindings))
                         (fn [] ~@body))))))

(defn local-spy
  "Same as spy, but does it on the current clojure thread"
  [v f]
  (let [number-of-calls (atom 0)]
    (fn [& args]
      (let [spied-f (cond (nil? f) (current->original-definition v)
                          (vector? f) (nth f @number-of-calls)
                          :else f)]
        (try (apply spied-f args)
             (finally (swap! number-of-calls inc)))))))

(defn local-calls
  "Same as calls, but for those symbols which were locally spied or
  stubbed."
  [f]
  (let [me (if (var? f) @f f)]
    (if (thread-bound? #'call-tracker)
      (get @call-tracker (:original (meta me)) [])
      [])))

(defmacro with-local-spy
  "Same as with-spy but spies only on the current clojure thread."
  [vs & body]
  `(let [current-call-tracker# (when (thread-bound? #'call-tracker)
                                (deref call-tracker))]
     (with-dynamic-redefs ~(->> (mapcat (fn [v]
                                          [v `(local-spy ~(list 'var v)
                                                         (local-redefinitions
                                                          ~(list 'var v)))])
                                        vs)
                                (vec))
       (binding [call-tracker (if current-call-tracker#
                               (do (swap! call-tracker
                                          merge
                                          ~(zipmap (map redef-binding->originals vs)
                                                   (repeat [])))
                                   call-tracker)
                               (atom ~(zipmap (map redef-binding->originals vs)
                                              (repeat []))))]
         ~@body))))

(defmacro with-local-spy-ns
  "Same as with-spy but does it on the current clojure thread."
  [namespaces & body]
  `(with-local-spy ~(mapcat ns->fn-symbols namespaces)
     (do ~@body)))

(defmacro with-local-stub
  "Same as with-stub but only stubs it on the local clojure thread."
  [vs & body]
  `(let [current-call-tracker# (when (thread-bound? #'call-tracker)
                                (deref call-tracker))]
     (with-dynamic-redefs ~(->> (mapcat (fn [v]
                                          (if (vector? v)
                                            [(first v) `(local-spy ~(list 'var (first v)) ~(second v))]
                                            [v `(local-spy ~(list 'var v) (constantly nil))])) vs)
                                (vec))
       (binding [call-tracker (if current-call-tracker#
                               (do (swap! call-tracker
                                          merge
                                          ~(zipmap (map redef-binding->originals vs)
                                                   (repeat [])))
                                   call-tracker)
                               (atom ~(zipmap (map redef-binding->originals vs)
                                              (repeat []))))]
         ~@body))))

(defn local-stub! [v replacement]
  (when (empty? (:arglists (meta v)))
    (throw (new #?(:clj IllegalArgumentException :cljs js/Error)
                "stub!/with-stub! may only be used on functions which include :arglists in their metadata. Use stub/with-stub instead.")))
  (let [f (local-spy v replacement)]
    (with-meta (fn [& args]
                 (if (args-match? (count args) (:arglists (meta v)))
                   (apply f args)
                   (throw (new #?(:clj clojure.lang.ArityException :cljs js/Error)
                               (count args) (str (:ns (meta v)) "/"
                                                 (:name (meta v)))))))
      (meta f))))

(defmacro with-local-stub!
  "Like with-stub!, but only works for thread-locally"
  [vs & body]
  `(let [current-call-tracker# (when (thread-bound? #'call-tracker)
                                 (deref call-tracker))]
     (with-dynamic-redefs ~(->> (mapcat (fn [v]
                                          (if (vector? v)
                                            [(first v) `(local-stub! (var ~(first v))
                                                                     ~(second v))]
                                            [v `(local-stub! (var ~v) (constantly nil))]))
                                        vs)
                                (vec))
       (binding [call-tracker (if current-call-tracker#
                                (do (swap! call-tracker
                                           merge
                                           ~(zipmap (map redef-binding->originals vs)
                                                    (repeat [])))
                                    call-tracker)
                                (atom ~(zipmap (map redef-binding->originals vs)
                                               (repeat []))))]

         ~@body))))
