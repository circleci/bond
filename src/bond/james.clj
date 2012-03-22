(ns bond.james)

(defn with-spies)

(defn spy
  "wrap f, keeping track of its call count and arguments"
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

(defn calls [f]
  (-> f (meta) ::calls (deref)))

(defmacro with-spy
  "Takes a var pointing at a fn. Rebinds the fn to track call counts"
  [v & body]
  `(binding [~v (spy ~v)]
     (do ~@body)))

(defmacro with-spy-redef
  [v & body]
  `(with-redefs [~v (spy ~v)]
     (do ~@body)))