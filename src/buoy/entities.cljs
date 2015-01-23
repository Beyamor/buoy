(ns buoy.entities)

(def player
  {:position {:x 0
              :y 0}
   :velocity {:x 0
              :y 0}
   :hitbox {:width 48
            :height 48}
   :keyboard-walker {:speed 5}
   :keyboard-jumper {:speed 30}
   :gravity true
   :debug-color :blue})

(defn wall
  [x y]
  {:position {:x x
              :y y}
   :hitbox {:width 48
            :height 48}
   :wall true})
