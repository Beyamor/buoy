(ns buoy.core
  (:require [sinai.app :as app]
            [sinai.scenes :as s]
            [sinai.rules :as r]
            [sinai.entities :as e]
            [sinai.input :as input]
            [buoy.entities :as b-entites]
            [buoy.rules :as b-rules])
  (:require-macros [sinai.rules.macros :as rm]))

(defn random-entity
  []
  {:position {:x (* 800 (Math/random))
              :y (* 600 (Math/random))}
   :hitbox {:width 48
            :height 48}})

(def handlers
  {:update-entity (fn [app entity f]
                    (update-in app [:scene :entities] e/update entity f))})

(input/bind
  :left 65
  :right 68)

(def rules
  [b-rules/the-keyboard-moves-left-and-right])

(app/launch
  :width 800
  :height 600
  :initial-scene (s/create-scene
                   :rules rules
                   :handlers handlers
                   :entities (e/add-all {}
                                        (map e/create [b-entites/player
                                                       (random-entity)
                                                       (random-entity)
                                                       (random-entity)]))))
