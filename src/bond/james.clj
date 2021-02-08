(ns bond.james)

(defn spy
  "wrap f, returning a new fn that keeps track of its call count and arguments."
  [f]
  (let [calls (atom [])]
    (with-meta (fn [& args]
                 (try
                   (let [old-f (if (vector? f) (nth f (count @calls)) f)
                         resp (apply old-f args)]
                     (swap! calls conj {:args args
                                        :return resp})
                     resp)
                   (catch Exception e
                     (swap! calls conj {:args args
                                        :throw e})
                     (throw e))))
      {::calls calls})))

(defn calls
  "Takes one arg, a fn that has previously been spied. Returns a seq
  of maps, one per call. Each map contains the keys :args and :return
  or :throw. If the fn has not been spied, throws an exception."
  [f]
  (if-some [calls (some-> (if (var? f) @f f)
                          meta
                          ::calls)]
    @calls
    (throw (new IllegalArgumentException
                "The argument is not a spied function. Calls of an unspied function are not tracked and are therefore not known."))))

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
  fn to track call counts, but does not change the fn's behavior."
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

  Replaces each fn with its replacement. If a replacement is not specified for
  a fn it is replaced with one that takes any number of args and returns nil.
  Also spies the stubbed-fn."
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
    (throw (new IllegalArgumentException
                "stub!/with-stub! may only be used on functions which include :arglists in their metadata. Use stub/with-stub instead.")))
  (let [f (spy replacement)]
    (with-meta (fn [& args]
                 (if (args-match? (count args) (:arglists (meta v)))
                   (apply f args)
                   (throw (new clojure.lang.ArityException
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
