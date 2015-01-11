(ns sinai.rules
  (:require [sinai.util :as u])
  (:refer-clojure :exclude [do let when]))

(defn group
  [forms]
  (loop [forms forms, groups []]
    (clojure.core/let [[group forms]
                       (cond
                         (= '<- (second forms))
                         [{:type    :binding
                           :binding (first forms)
                           :value   (nth forms 2)}
                          (drop 3 forms)]

                         (= '<< (second forms))
                         [{:type     :binding-each
                           :binding  (first forms)
                           :value    (nth forms 2)}
                          (drop 3 forms)]

                         :else
                         [{:type   :normal
                           :value  (first forms)}
                          (drop 1 forms)])
                       groups (conj groups group)]
      (if (empty? forms)
        groups
        (recur forms groups)))))

(defn build-form
  [body form state-var result-var]
  (case (:type form)
    :normal
    `(clojure.core/let [[~result-var messages#] (~(:value form) ~state-var)
                        [value# more-messages#] ~body]
       [value# (concat messages# more-messages#)])

    :binding
    `(clojure.core/let [[~result-var messages#] (~(:value form) ~state-var)
                        ~(:binding form) ~result-var
                        [return-value# more-messages#] ~body]
       [return-value# (concat messages# more-messages#)])

    :binding-each
    `(clojure.core/let [[~result-var messages#] (~(:value form) ~state-var)
                        more-messages# (for [~(:binding form) ~result-var]
                                        (second ~body))]
       [nil (apply concat messages# more-messages#)])))

(defn build
  [forms]
  (clojure.core/let [state-var (gensym)
                     result-var (gensym)]
    `(fn [~state-var]
       ~(reduce
          #(build-form %1 %2 state-var result-var)
          `[~result-var nil]
          forms))))

(defmacro do
  [& forms]
  (-> forms
      group
      reverse
      build))

(defmacro let
  [bindings & body]
  `(clojure.core/let ~bindings
     (sinai.rules/do ~@body)))

(defmacro when
  [pred? & body]
  `(if ~pred?
     (sinai.rules/do ~@body)
     (sinai.rules/return nil)))

(defmulti construct-rule
  (fn [arg-map]
    (spit "/tmp/trigger" (-> arg-map :trigger (str \newline)) :append true)
    (:trigger arg-map)))

(defmethod construct-rule :default
  [arg-map]
  arg-map)

(clojure.core/let [components-and-binding (fn [xs]
                                            (cond (= :as (second xs))
                                                  (clojure.core/let [[components _ binding & xs] xs]
                                                    [components binding xs])
                                                  :else
                                                  (clojure.core/let [[components & xs] xs]
                                                    [components (gensym) xs])))
                   components-and-bindings (fn [xs]
                                             (clojure.core/let [[components1 binding1 xs] (components-and-binding xs)
                                                                xs (rest xs)
                                                                [components2 binding2 xs] (components-and-binding xs)]
                                               [[components1 binding1] [components2 binding2]]))]
  (defmethod construct-rule :collision
    [arg-map]
    (clojure.core/let [[[components1 binding1] [components2 binding2]] (components-and-bindings
                                                                         (:between arg-map))]
      (-> arg-map
          (dissoc :between)
          (merge {:components1 components1
                  :components2 components2
                  :action `(fn [~binding1 ~binding2]
                             ~(:action arg-map))})))))

(defmacro defrule
  [name & args]
  (clojure.core/let [arg-map (-> (apply hash-map
                                        (concat (butlast args)
                                                [:action (last args)]))
                                 (u/swap-key :on :trigger))]
    `(def ~name ~(construct-rule arg-map))))

(defmacro update-entity
  [entity & body]
  `(sinai.rules/send-entity-update-message ~entity
                                           (fn [~entity] ~@body)))
