(ns sinai.scenes
  (:require [sinai.rules :as r]
            [sinai.entities :as e]
            [sinai.util :as util]))

(defprotocol Scene
  (update [_ app]))

(defn apply-handlers
  [state handlers messages]
  (reduce (fn [state [message-type & data]]
            (if-let [handle (get handlers message-type)]
              (apply handle state data)
              (throw (str "Unhandled message type: " message-type))))
          state messages))

(defn apply-physics
  [app]
  (reduce (fn [app mover-id]
            (let [{:keys [rules entities]} (:scene app)
                  mover (e/get entities mover-id)
                  collision-rules (->> (:collision rules)
                                       (filter #(e/has-components? mover
                                                                   (:components %))))
                  x-steps (-> mover :velocity :x util/abs)
                  x-direction (util/signum x-steps)
                  y-steps (-> mover :velocity :y util/abs)
                  y-direction (util/signum y-steps)]
              (loop [app app, previous-collisions #{}, x-steps x-steps, y-steps y-steps]
                (if (and (zero? x-steps) (zero? y-steps))
                  app
                  (let [entities (-> app :scene :entities)
                        mover (e/get entities mover-id)
                        x-step (* x-direction (min x-steps 1))
                        y-step (* y-direction (min y-steps 1))
                        mover' (-> mover
                                   (update-in [:position :x] + x-step)
                                   (update-in [:position :y] + y-step))
                        collisions-and-messages (for [collision-rule collision-rules
                                                      other-id (e/get-with entities (:components2 collision-rule))
                                                      :when (not= mover-id other-id)
                                                      :let [other (e/get entities other-id)]
                                                      :when (e/collide? mover' other)
                                                      :let [action ((:action collision-rule)
                                                                    mover other)]
                                                      message (r/get-messages
                                                                (action app))]
                                                  [other-id message])
                        messages (->> collisions-and-messages
                                      (remove (fn [other-id message]
                                                (and (contains? previous-collisions other-id)
                                                     (not= :stop (first message)))))
                                      (map second))
                        previous-collisions (->> collisions-and-messages previous-collisions
                                                 (map first)
                                                 (into previous-collisions))
                        stopped? (some #(-> % first (= :stop)) messages)]
                    (if stopped?
                      app
                      (recur (update-in app [:scene :entities]
                                        e/update mover (constantly mover'))
                             previous-collisions
                             (-> x-steps dec (max 0))
                             (-> y-steps dec (max 0)))))))))
          app
          (-> app :scene :entities (e/get-with #{:position :velocity}))))

(defrecord StandardScene
  [rules handlers entities]
  Scene
  (update [_ app]
    (let [state {:app app}
          messages (r/apply-rules state (:frame-entered rules))]
      (-> app
        (apply-handlers handlers messages)
        apply-physics))))

(defn create-scene
  [& {:keys [rules handlers entities]}]
  (let [rules (r/collect rules)]
    (->StandardScene rules handlers entities)))
