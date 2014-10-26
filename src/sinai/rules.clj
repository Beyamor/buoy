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
  [body form]
  (case (:type form)
    :normal
    `(sinai.rules/bind ~(:value form)
                       (fn [~(gensym)]
                         ~body))
    :binding
    `(sinai.rules/bind ~(:value form)
                       (fn [~(:binding form)]
                         ~body))

    :binding-each
    `(sinai.rules/bind-each ~(:value form)
                            (fn [~(:binding form)]
                              ~body))))

(defn build
  [[base & more-forms]]
  (reduce build-form (:value base) more-forms))

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
  `(clojure.core/when ~pred?
     (sinai.rules/do ~@body)))

(defmulti construct-rule :trigger)

(defmethod construct-rule :default
  [arg-map]
  arg-map)

(let [components-and-binding (fn [xs]
                               (cond (= :as (second xs))
                                     (let [[components _ binding & xs] xs]
                                       [components binding xs])
                                     :else
                                     (let [[components & xs] xs]
                                       [components (gensym) xs])))
      components-and-bindings (fn [xs]
                                (let [[components1 binding1 xs] (components-and-binding xs)
                                      xs (rest xs)
                                      [components2 binding2 xs] (components-and-binding xs)]
                                  [[components1 binding1] [components2 binding2]]))]
  (defmethod construct-rule :collision
    [arg-map]
    (let [[[components1 binding1] [components2 binding2]] (components-and-bindings
                                                            (:between arg-map))]
      (-> arg-map
          (merge {:components1 components1
                  :components2 components2
                  :action `(fn [~binding1 ~binding2]
                             ~(:action arg-map))})))))

(defmacro defrule
  [name & args]
  (let [arg-map (-> (apply hash-map
                       (concat (butlast args)
                               [:action (last args)]))
                    (u/swap-key :on :trigger))]
    `(def ~name ~(construct-rule arg-map))))

(defmacro update-entity
  [entity & body]
  `(sinai.rules/send-entity-update-message ~entity
                                           (fn [~entity] ~@body)))
