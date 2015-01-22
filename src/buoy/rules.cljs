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
           entity << (r/get-entities-with #{:velocity :keyboard-walker})
           (r/update-entity entity
                             (assoc-in entity [:velocity :x]
                                        (* dx (-> entity :keyboard-walker :speed)))))))

(let [gravity 4
      max-velocity 50]
  (defrule gravity-pulls-things-down
    :on :frame-entered
    (r/do entity << (r/get-entities-with #{:velocity :gravity :hitbox})
          entities <- (r/get-in-scene :entities)
          (r/when (not (e/collides-with? entities
                                         entity
                                         :below))
            (r/update-entity entity
                             (update-in entity [:velocity :y]
                                        #(-> %
                                             (+ gravity)
                                             (min max-velocity))))))))

(defrule walls-stop-things
  :on :collision
  :between [#{:velocity} :as mover
            :and
            #{:wall}]
  (r/do (r/stop mover)))

(defrule the-keyboard-jumps
  :on :frame-entered
  (r/do input <- (r/get-in-app :input)
        (r/when (i/was-pressed? input :jump)
          entities <- (r/get-in-scene :entities)
          jumper << (r/get-entities-with #{:velocity :keyboard-jumper})
          (r/when (e/collides-with? entities
                                    jumper
                                    :below)
            (r/update-entity jumper
                             (update-in jumper [:velocity :y]
                                        - (-> jumper :keyboard-jumper :speed)))))))

;  (defrule player-collects-coins
;    :on :collision
;    :between [#{:player}
;              #{:coin} :as coin]
;    (do (destroy coin)
;        increase-score))
