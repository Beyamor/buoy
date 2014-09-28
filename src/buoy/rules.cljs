(ns buoy.rules)

(comment
(defrule the-keyboard-moves-left-and-right
  :on :frame-entered
  (do :let [dx (+ (if (key-down? :right) 1 0)
                  (if (key-down? :left) -1 0))]
      entity <- (entities-with #{:velocity :keyboard-walker})
      :let [speed (-> entity :keyboard-walker :speed)]
      (update entity :velocity
              (assoc :x (* speed dx)))))

(defrule the-keyboard-jumps
  :on :frame-entered
  (do :when (key-pressed? :jump)
      (do entity <- (entities-with #{:velocity :keyboard-jumper})
          :let [speed (-> entity :keyboard-jumper :speed)]
          (update entity :velocity
                  (assoc :y (- speed))))))

(let [gravity 10]
  (defrule gravity-pulls-things-down
    :on :frame-entered
    (do entity <- (entities-with #{:velocity :gravity :hitbox})
        (if-not (collides-with entity (entities-with #{:wall}) :below)
          (update entity :velocity
                  (update-in [:y] + gravity))))))

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
