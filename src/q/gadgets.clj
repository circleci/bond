(ns q.gadgets)

(defn spy
  "wrap f, returning a new fn that keeping track of its call count and arguments"
  [f]
  (let [calls (atom [])
        old-f f]
    (with-meta (fn spy [& args]
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

(defn last-call
  "Get the last call map"
  [f]
  (-> f calls last))

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

(defn called-with?
  "Has the spied function been called with these args?"
  [f args]
  (some #(= args (:args %)) (calls f)))

(defmacro with-spies
  "Takes a vector of fn vars (vars that resolve to fns). Modifies the
  fn to track call counts, but does not change the fn's behavior"
  [setup & body]
  `(with-fakes ~(zipmap setup setup)
     ~@body))

(defn- prep-stubs
  [setup]
  (cond
    (vector? setup)
      (zipmap setup (repeat `(constantly nil)))
    (map? setup)
      (zipmap (keys setup)
              (map (fn [v] `(constantly ~v)) (vals setup)))
    :else
      (throw (Exception. (str "Unexpected binding: " setup)))))

(defmacro with-stubs
  "Can either take a vector of fn vars, or a map of fn vars to return values.
  Replaces each fn with one that takes any number of args and returns nil or
  the mapped return value. Also spies the stubbed-fn"
  [setup & body]
  `(with-fakes ~(prep-stubs setup)
     ~@body))

(defmacro with-fakes
  "Takes a map of fn vars to fns.
  Replaces each fn with the new implementation and spies the stubbed-fn"
  [setup & body]
  `(with-redefs ~(vec (mapcat (fn [[f fake]] [f `(spy ~fake)]) setup))
     ~@body))