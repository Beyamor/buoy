(ns sinai.util)

(defn signum
  [x]
  (cond
    (> x 0) 1
    (< x 0) -1
    :else 0))
