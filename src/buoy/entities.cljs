(ns buoy.entities)

(def player
  {:position {:x 0
              :y 0}
   :velocity {:x 0
              :y 0}
   :hitbox {:width 48
            :height 48}
   :keyboard-walker {:speed 5}
   :gravity true
   :debug-color :blue})
