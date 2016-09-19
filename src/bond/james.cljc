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
                                        }))))))
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
