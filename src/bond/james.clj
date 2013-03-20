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

(defn with-stub*
  "Takes a seq of vars. spys on them, while also redefining them to return nil"
  [vs f]
  (let [binding-map (->> vs
                         (map (fn [v]
                                   [v (spy (constantly nil))]))
                         (into {}))]
    (with-redefs-fn binding-map f)))

(defmacro with-stub
  "Takes a vector of fn vars. Replaces each fn with one that takes any
  number of args and returns nil. Also spies the stubbed-fn"
  [vs & body]

  `(with-stub* ~vs (fn [] ~@body)))