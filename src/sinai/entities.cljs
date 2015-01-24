(ns sinai.entities
  (:require clojure.set
            [sinai.util :as util :refer [floor]])
  (:refer-clojure :exclude [get = not=])
  (:require-macros [sinai.util :as util]
                   [lonocloud.synthread :as ->]))

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

(util/extend-maps
  Entities
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
  ([e1 e2]
   (collide? e1 e2 {:x identity :y identity}))
  ([e1 e2 {modify-x :x modify-y :y}]
   (not (or (< (modify-x (right e1))   (left e2))
            (> (modify-x (left e1))    (right e2))
            (< (modify-y (bottom e1))  (top e2))
            (> (modify-y (top e1))     (bottom e2))))))

(let [location-modifiers {:below {:x identity
                                  :y inc}}]
  (defn collides-with?
    [entities entity location-modifier]
    (let [entity (get entities entity)
          location-modifier (clojure.core/get location-modifiers location-modifier)]
      (->> (get-all entities)
           (filter #(not= entity %))
           (some #(collide? entity % location-modifier))))))

(defprotocol SpatialAccess
  (in-region [this left right top bottom]))

(extend-protocol SpatialAccess
  object
  (in-region [this left- right- top- bottom-]
    (for [id (get-all-ids this)
          :let [entity (get this id)]
          :when (not (or (< right- (left entity))
                         (> left- (right entity))
                         (< bottom- (top entity))
                         (> top- (bottom entity))))]
      id)))

(defn entities-colliding-with
  [entities entity]
  (let [entity-id (get-id entity)]
    (for [other-id (in-region entities
                              (left entity) (right entity)
                              (top entity) (bottom entity))
          :when (not= entity-id other-id)
          :let [other (get entities other-id)]
          :when (collide? entity other)]
      other-id)))

(defn create-spatial-index
  [grid-width]
  {:grid-width grid-width
   :ids {}})

(defn add-to-spatial-index
  [{:keys [grid-width] :as index} entity]
  (let [grid-left (-> entity left (/ grid-width) floor)
        grid-right (-> entity right (/ grid-width) floor)
        grid-top (-> entity top (/ grid-width) floor)
        grid-bottom (-> entity bottom (/ grid-width) floor)
        id (get-id entity)]
    (reduce (fn [index [x y]]
              (update-in index [:ids x y] (fnil conj #{}) id))
            index
            (for [x (range grid-left (inc grid-right))
                  y (range grid-top (inc grid-bottom))]
              [x y]))))

(defn remove-from-spatial-index
  [{:keys [grid-width] :as index} entity]
  (let [grid-left (-> entity left (/ grid-width) floor)
        grid-right (-> entity right (/ grid-width) floor)
        grid-top (-> entity top (/ grid-width) floor)
        grid-bottom (-> entity bottom (/ grid-width) floor)
        id (get-id entity)]
    (reduce (fn [index [x y]]
              (-> index
                  (->/in [:ids]
                         (update-in [x y] disj id)
                         (->/as ids
                                (->/when (empty? (get-in ids [x y]))
                                  (->/in [x] (dissoc y))))
                         (->/as ids
                                (->/when (empty? (get ids x))
                                  (dissoc x))))))
            index
            (for [x (range grid-left (inc grid-right))
                  y (range grid-top (inc grid-bottom))]
              [x y]))))
