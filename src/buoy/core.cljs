(ns buoy.core
  (:require [sinai.app :as app]
            [buoy.entities :as b-entites]))

(defn random-entity
  []
  {:position {:x (* 800 (Math/random))
              :y (* 600 (Math/random))}
   :hitbox {:width 48
            :height 48}})

(app/launch
  :width 800
  :height 600
  :initial-scene {:entities [b-entites/player
                             (random-entity)
                             (random-entity)
                             (random-entity)]})
