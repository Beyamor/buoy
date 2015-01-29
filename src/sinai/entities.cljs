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
  (or (-> e :position :x) 0))

(defn right
  [e]
  (+ (left e)
     (or (-> e :hitbox :width) 0)))

(defn top
  [e]
  (or  (-> e :position :y) 0))

(defn bottom
  [e]
  (+ (top e)
     (or (-> e :hitbox :height) 0)))

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

(defn create-spatial-indexing-grid
  [grid-width]
  {:grid-width grid-width
   :ids {}})

(defn ->spatial-grid-index
  [value grid-width]
  (-> value (/ grid-width) floor))

(defn reduce-over-spatial-grid-coordinates
  [f result left right top bottom grid-width]
  (let [grid-left (->spatial-grid-index left grid-width)
        grid-right (->spatial-grid-index right grid-width)
        grid-top (->spatial-grid-index top grid-width)
        grid-bottom (->spatial-grid-index bottom grid-width)]
    (loop [x grid-left result result]
      (if (> x grid-right)
        result
        (recur (inc x)
               (loop [y grid-top result result]
                 (if (> y grid-bottom)
                   result
                   (recur (inc y)
                          (f result x y)))))))))

(defn reduce-over-spatial-grid-coordinates-for-entity
  [f result entity grid-width]
  (reduce-over-spatial-grid-coordinates
    f result
    (left entity)
    (right entity)
    (top entity)
    (bottom entity)
    grid-width))

(defn add-to-spatial-indexing-grid
  [{:keys [grid-width] :as index} entity]
  (let [id (get-id entity)]
    (reduce-over-spatial-grid-coordinates-for-entity
      (fn [index x y]
        (update-in index [:ids x y] (fnil conj #{}) id))
      index
      entity
      grid-width)))

(defn add-all-to-spatial-indexing-grid
  [index entities]
  (reduce add-to-spatial-indexing-grid index entities))

(defn remove-from-spatial-indexing-grid
  [{:keys [grid-width] :as index} entity]
  (let [id (get-id entity)]
    (reduce-over-spatial-grid-coordinates-for-entity
      (fn [index x y]
        (-> index
            (->/in [:ids]
                   (update-in [x y] disj id)
                   (->/as ids
                          (->/when (-> ids (get x) (get y) empty?)
                            (->/in [x] (dissoc y))))
                   (->/as ids
                          (->/when (-> ids (get x) empty?)
                            (dissoc x))))))
      index
      entity
      grid-width)))

(defn get-spatial-grid-indices-in-range
  [{:keys [grid-width] :as grid} left right top bottom]
  (reduce-over-spatial-grid-coordinates
    (fn [ids x y]
      (into ids
            (some-> grid :ids (get x) (get y))))
    #{}
    left right top bottom
    grid-width))

(defn update-spatial-indexing-grid-for-entity
  [{:keys [grid-width] :as grid} before after]
  (-> grid
      (->/when (or (not= (->spatial-grid-index (left before) grid-width)
                         (->spatial-grid-index (left after) grid-width))
                   (not= (->spatial-grid-index (right before) grid-width)
                         (->spatial-grid-index (right after) grid-width))
                   (not= (->spatial-grid-index (top before) grid-width)
                         (->spatial-grid-index (top after) grid-width))
                   (not= (->spatial-grid-index (bottom before) grid-width)
                         (->spatial-grid-index (bottom after) grid-width)))
        (remove-from-spatial-indexing-grid before)
        (add-all-to-spatial-indexing-grid after))))

(defrecord SpatiallyIndexedEntities
  [entities spatial-indexing-grid]

  Entities
  (add-all [this entities-to-add]
    (-> this
        (update-in [:entities] add-all entities-to-add)
        (update-in [:spatial-indexing-grid] add-all-to-spatial-indexing-grid entities-to-add)))
  (get-all-ids [this]
    (get-all-ids entities))
  (-get [this id]
    (-get entities id))
  (-update [this id f]
    (let [before (get entities id)
          entities' (-update entities id f)
          after (get entities id)]
      (-> this
          (assoc :entities entities')
          (->/in [:spatial-indexing-grid]
                 (update-spatial-indexing-grid-for-entity before after)))))
  (get-with [this components]
    (get-with entities components))

  SpatialAccess
  (in-region [this left right top bottom]
    (get-spatial-grid-indices-in-range
      spatial-indexing-grid
      left right top bottom)))
