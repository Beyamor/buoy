(ns sinai.util)

(defn signum
  [x]
  (cond
    (> x 0) 1
    (< x 0) -1
    :else 0))

(def abs #(.abs js/Math %))
(def floor #(.floor js/Math %))
(def ceil #(.ceil js/Math %))
