(ns q.gadgets)

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
  "Returns a seq of call maps, one per call.
  Each map contains the keys :args and :return or :throw"
  [f]
  (-> f (meta) ::calls (deref)))

(defn call
  "Get the call map for the nth call"
  [f n]
  (-> f calls (nth n)))

(defn called?
  "Has the spied function been called?"
  [f]
  (-> f calls seq boolean))

(defn called-once?
  "Has the spied function been called once?"
  [f]
  (-> f calls count (= 1)))

(defn call-count
  "The number of times the spied function has been called"
  [f]
  (-> f calls count))

(defmacro with-spies
  "Takes a vector of fn vars (vars that resolve to fns). Modifies the
  fn to track call counts, but does not change the fn's behavior"
  [vs & body]
  `(with-redefs ~(vec (mapcat (fn [v] [v `(spy ~v)]) vs))
     ~@body))

(defn- prep-stubs
  [setup]
  (cond
    (vector? setup)
      (prep-stubs (zipmap setup (repeat nil)))
    (map? setup)
      (mapcat (fn [[f r]] [f `(spy (constantly ~r))]) setup)
    :else
      (throw (Exception. (str "Unexpected binding: " setup)))))

(defmacro with-stubs
  "Can either take a vector of fn vars, or a map of fn vars to return values.
  Replaces each fn with one that takes any number of args and returns nil or
  the mapped return value. Also spies the stubbed-fn"
  [setup & body]

  `(with-redefs ~(vec (prep-stubs setup))
     ~@body))