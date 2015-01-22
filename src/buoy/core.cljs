(ns buoy.core
  (:require [sinai.app :as app]
            [sinai.scenes :as s]
            [sinai.rules :as r :include-macros true]
            [sinai.entities :as e]
            [sinai.input :as input]
            [buoy.entities :as b-entites]
            [buoy.rules :as b-rules]))

(defn wall
  [x y]
  {:position {:x x
              :y y}
   :hitbox {:width 48
            :height 48}
   :wall true})

(def handlers
  {:update-entity (fn [app entity f]
                    (update-in app [:scene :entities] e/update entity f))})

(input/bind
  :left 65
  :right 68
  :jump 87)

(def rules
  [b-rules/the-keyboard-moves-left-and-right
   b-rules/gravity-pulls-things-down
   b-rules/walls-stop-things
   b-rules/the-keyboard-jumps])

(app/launch
  :width 800
  :height 600
  :initial-scene (s/create-scene
                   :rules rules
                   :handlers handlers
                   :entities (e/add-all {}
                                        (map e/create (concat [b-entites/player]
                                                              (for [x (range 0 800 48)]
                                                                (wall x 500)))))))
