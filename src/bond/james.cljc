(ns bond.james)

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
  (-> f (meta) ::calls (deref)))

(defmacro with-spy
  "Takes a vector of fn vars (vars that resolve to fns). Modifies the
  fn to track call counts, but does not change the fn's behavior"
  [vs & body]
  `(with-redefs ~(->> (mapcat (fn [v]
                                [v `(spy ~v)]) vs)
                      (vec))
     (do ~@body)))

(defmacro with-spy-ns
  "Like with-spy but takes a vector of namespaces. Spies on every function in
  the namespace."
  [namespaces & body]
  `(with-redefs ~(->> namespaces
                      (mapcat (fn [n]
                                (->> (ns-publics n)
                                     (remove (fn [[s v]]
                                            (:macro (meta v))))
                                     (filter (fn [[_ v]]
                                               (fn? @v)))
                                     (mapcat (fn [[s _]]
                                               [s `(spy ~s)])))))
                      vec)
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

(defmacro with-stub-ns
  "Takes a vector of namespaces and/or [namespace replacement] vectors.

  Replaces every fn in each namespace with its replacement. If a replacement is
  not specified for a namespace every fn in that namespace is replaced with
  (constantly nil). All replaced functions are also spied."
  [namespaces & body]
  `(with-redefs ~(->> namespaces
                      (mapcat (fn [n]
                                (let [[n stub-fn] (if (vector? n)
                                                    [(first n) `(spy ~(second n))]
                                                    [n `(spy (constantly nil))])]
                                  (->> (ns-publics n)
                                       (remove (fn [[s v]]
                                                 (:macro (meta v))))
                                       (filter (fn [[_ v]]
                                                 (fn? @v)))
                                       (mapcat (fn [[s _]]
                                                 [s stub-fn]))))))
                      vec)
     (do ~@body)))
