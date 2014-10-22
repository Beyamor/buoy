(ns sinai.util)

(defmacro extend-maps
  [& body]
  `(do
       (extend-type cljs.core.PersistentHashMap ~@body)
       (extend-type cljs.core.PersistentArrayMap ~@body)))
