(ns buoy.rules
  (:require [sinai.rules :as r :refer-macros [defrule]]
            [sinai.entities :as e]
            [sinai.input :as i]))

(defrule velocity-is-integrated
  :on :frame-entered
  (r/do entity << (r/get-entities-with #{:position :velocity})
         (r/update-entity entity
                           (-> entity
                               (update-in [:position :x]
                                          + (-> entity :velocity :x))
                               (update-in [:position :y]
                                          + (-> entity :velocity :y))))))

(defrule the-keyboard-moves-left-and-right
  :on :frame-entered
  (r/do input <- (r/get-in-app :input)
         (r/let [dx (+ (if (i/is-down? input :right) 1 0)
                        (if (i/is-down? input :left) -1 0))]
           entity << (r/get-entities-with #{:position :keyboard-walker})
           (r/update-entity entity
                             (update-in entity [:position :x]
                                        + (* dx (-> entity :keyboard-walker :speed)))))))

(let [gravity 1
      max-velocity 20]
  (defrule gravity-pulls-things-down
    :on :frame-entered
    (r/do entity << (r/get-entities-with #{:velocity :gravity :hitbox})
          entities <- (r/get-in-scene :entities)
          (if-not (e/collides-with? entities
                                    entity)
            (r/update-entity entity
                             (update-in entity [:velocity :y]
                                        #(-> %
                                             (+ gravity)
                                             (min max-velocity))))
            (r/update-entity entity
                             (assoc-in entity [:velocity :y] 0))))))

(comment
  (defrule the-keyboard-jumps
    :on :frame-entered
    (do :when (key-pressed? :jump)
        entity <- (entities-with #{:velocity :keyboard-jumper})
        :when (collides-with entity (entities-with #{:wall} :below))
        :let [speed (-> entity :keyboard-jumper :speed)]
        (accelerate entity :y (- speed))))

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
