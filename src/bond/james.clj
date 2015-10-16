(ns bond.james)

(defn spy
  "wrap f, returning a new fn that keeping track of its call count and arguments"
  [f]
  (let [calls (atom [])
        old-f f]
    (with-meta (fn [& args]
                 (try
                   (let [resp (apply old-f args)]
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

(defmacro with-trap
  "Calls body with the vector of fn vars stubbed and throws an exception if
  any of the stubbed fuctions were called when executing body.

  The exception that is thrown is an ex-info object. To inspect the arguments
  that were passed to the function to which a trap was added, you can inspect
  the :calls key of the ex-data of the thrown exception."
  [vs & body]
  `(with-stub ~vs
     (let [result# (do ~@body)]
       (doseq [v# ~vs]
         (when (-> v# calls count pos?)
           (throw (ex-info "fn was called" {:calls (calls v#)}))))
       result#)))
