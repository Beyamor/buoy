(ns sinai.entities
  (:refer-clojure :exclude [get]))

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

(defprotocol Entities
  (add-all [this entities-to-add])
  (get-all-ids [this])
  (-get [this id])
  (-update [this id f]))

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
    (update-in this [id] f)))
