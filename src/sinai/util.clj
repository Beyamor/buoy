(ns sinai.util)

(defn swap-key
  [m from to]
  (if (contains? m from)
    (-> m
        (assoc to (get m from))
        (dissoc from))
    m))

(defmacro extend-maps
  [& body]
  `(do
       (extend-type cljs.core.PersistentHashMap ~@body)
       (extend-type cljs.core.PersistentArrayMap ~@body)))
