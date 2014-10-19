(ns buoy.rules
  (:require [sinai.rules :as r]
            [sinai.input :as i])
  (:require-macros [sinai.rules.macros :as rm]))

(rm/defrule the-keyboard-moves-left-and-right
  :on :frame-entered
  (rm/do input <- (r/get-in-app :input)
         (rm/let [dx (+ (if (i/is-down? input :right) 1 0)
                        (if (i/is-down? input :left) -1 0))]
           entity << (r/get-entities-with #{:velocity :key-mover})
           (rm/let [speed 5] ;(-> entity :keyboard-walker :speed)]
             (r/update-entity entity update-in [:position :x] + (* speed dx))))))

(comment
  (defrule the-keyboard-jumps
    :on :frame-entered
    (do :when (key-pressed? :jump)
        entity <- (entities-with #{:velocity :keyboard-jumper})
        :when (collides-with entity (entities-with #{:wall} :below))
        :let [speed (-> entity :keyboard-jumper :speed)]
        (accelerate entity :y (- speed))))

  (let [gravity 10]
    (defrule gravity-pulls-things-down
      :on :frame-entered
      (do entity <- (entities-with #{:velocity :gravity :hitbox})
          :when (not (collides-with entity (entities-with #{:wall}) :below))
          (accelerate entity :y gravity))))

  (defrule walls-stop-things
    :on :collision
    :between [#{:velocity} :as mover
              #{:wall}]
    (do (stop mover)))

  (defrule player-collects-coins
    :on :collision
    :between [#{:player}
              #{:coin} :as coin]
    (do (destroy coin)
        increase-score))
  )
