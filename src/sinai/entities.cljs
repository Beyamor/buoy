(ns sinai.entities
  (:require clojure.set)
  (:refer-clojure :exclude [get = not=]))

(let [ids (atom 0)]
  (defn generate-id
    []
    (swap! ids inc)))

(def get-id ::id)

(defn create
  [components]
  (merge {::id (generate-id)}
         components))

(defn ->id
  [thing]
  (if (number? thing)
    thing
    (get-id thing)))

(defn has-components?
  [e components]
  (clojure.set/subset? components (set (keys e))))

(defprotocol Entities
  (add-all [this entities-to-add])
  (get-all-ids [this])
  (-get [this id])
  (-update [this id f])
  (get-with [this components]))

(defn get
  [this entity]
  (-get this (->id entity)))

(defn update
  [this entity f]
  (-update this (->id entity) f))

(extend-protocol Entities
  PersistentArrayMap
  (add-all [this entities]
    (reduce (fn [this entity]
              (assoc this (get-id entity) entity))
            this entities))

  (get-all-ids [this]
    (keys this))

  (-get [this id]
    (cljs.core/get this id))

  (-update [this id f]
    (update-in this [id] f))

  (get-with [this components]
    (let [components (set components)]
      (for [[id entity] this
            :when (has-components? entity components)]
        id))))

(defn get-all
  [entities]
  (for [id (get-all-ids entities)]
    (get entities id)))

(defn =
  [e1 e2]
  (clojure.core/= (->id e1) (->id e2)))

(def not= (complement =))

(defn left
  [e]
  (get-in e [:position :x] 0))

(defn right
  [e]
  (+ (left e)
     (get-in e [:hitbox :width] 0)))

(defn top
  [e]
  (get-in e [:position :y] 0))

(defn bottom
  [e]
  (+ (top e)
     (get-in e [:hitbox :height] 0)))

(defn collide?
  [e1 e2]
  (not (or (< (right e1) (left e2))
           (> (left e1) (right e2))
           (< (bottom e1) (top e2))
           (> (top e1) (bottom e2)))))

(defn collides-with?
  [entities entity]
  (let [entity (get entities entity)]
    (->> (get-all entities)
         (filter #(not= entity %))
         (some #(collide? entity %)))))
