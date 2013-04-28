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

(defn- prep-stubs
  [setup]
  (cond
    (vector? setup)
      (prep-stubs (zipmap setup (repeat nil)))
    (map? setup)
      (mapcat (fn [[f r]] [f `(spy (constantly ~r))]) setup)
    :else
      (throw (Exception. (str "Unexpected binding: " setup)))))

(defmacro with-stub
  "Can either take a vector of fn vars, or a map of fn vars to return values.
  Replaces each fn with one that takes any number of args and returns nil or
  the mapped return value. Also spies the stubbed-fn"
  [setup & body]

  `(with-redefs ~(vec (prep-stubs setup))
     ~@body))