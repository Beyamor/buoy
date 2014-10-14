(ns sinai.rules.macros
  (:refer-clojure :exclude [do let]))

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
     (sinai.rules.macros/do ~@body)))
