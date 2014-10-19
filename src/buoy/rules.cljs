(ns buoy.rules
  (:require [sinai.rules :as r]
            [sinai.input :as i])
  (:require-macros [sinai.rules.macros :as mr :refer [defrule]]))

(defn accelerate
  [entity axis amount]
  (r/update-entity entity update-in [:velocity axis] + amount))

(defrule velocity-is-integrated
  :on :frame-entered
  (mr/do entity << (r/get-entities-with #{:position :velocity})
         (mr/update-entity entity
                           (-> entity
                               (update-in [:position :x]
                                          + (-> entity :velocity :x))
                               (update-in [:position :y]
                                          + (-> entity :velocity :y))))))

(defrule the-keyboard-moves-left-and-right
  :on :frame-entered
  (mr/do input <- (r/get-in-app :input)
         (mr/let [dx (+ (if (i/is-down? input :right) 1 0)
                        (if (i/is-down? input :left) -1 0))]
           entity << (r/get-entities-with #{:position :keyboard-walker})
           (mr/update-entity entity
                             (update-in entity [:position :x]
                                        + (* dx (-> entity :keyboard-walker :speed)))))))

(let [gravity 1]
  (defrule gravity-pulls-things-down
    :on :frame-entered
    (mr/do entity << (r/get-entities-with #{:velocity :gravity :hitbox})
           ;:when (not (collides-with entity (entities-with #{:wall}) :below))
           (accelerate entity :y gravity))))

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
