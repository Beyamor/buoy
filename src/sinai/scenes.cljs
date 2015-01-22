(ns sinai.scenes
  (:require [sinai.rules :as r]
            [sinai.entities :as e]
            [sinai.util :as util])
  (:require-macros [lonocloud.synthread :as ->]))

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
                  x-direction (-> mover :velocity :x util/signum)
                  y-steps (-> mover :velocity :y util/abs)
                  y-direction (-> mover :velocity :y util/signum)
                  do-collision (fn [mover previous-collisions]
                                 (let [collisions-and-messages (for [collision-rule collision-rules
                                                                     other-id (e/get-with entities (:components2 collision-rule))
                                                                     :when (not= mover-id other-id)
                                                                     :let [other (e/get entities other-id)]
                                                                     :when (e/collide? mover other)
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
                                       collisions (->> collisions-and-messages previous-collisions
                                                       (map first))
                                       stopped? (some #(-> % first (= :stop)) messages)]
                                   [messages collisions stopped?]))]
              (loop [app app, previous-collisions #{}, x-steps x-steps, y-steps y-steps]
                (if-not (or (pos? x-steps) (pos? y-steps))
                  app
                  (let [entities (-> app :scene :entities)
                        mover (e/get entities mover-id)
                        x-step (* x-direction (min x-steps 1))
                        y-step (* y-direction (min y-steps 1))
                        [x-messages x-collisions x-stopped?] (when (pos? x-steps)
                                                               (do-collision
                                                                 (update-in mover [:position :x] + x-step)
                                                                 previous-collisions))
                        x-steps (if x-stopped? 0 (max (dec x-steps) 0))
                        [y-messages y-collisions y-stopped?] (when (pos? y-steps)
                                                               (do-collision
                                                                 (update-in mover [:position :y] + y-step)
                                                                 previous-collisions))
                        y-steps (if y-stopped? 0 (max (dec y-steps) 0))
                        collisions (-> previous-collisions
                                       (into x-collisions)
                                       (into y-collisions))]
                    (recur (update-in app [:scene :entities]
                                      e/update mover 
                                      #(-> %
                                          (->/in [:position]
                                                 (->/when (not x-stopped?)
                                                   (->/in [:x] (+ x-step)))
                                                 (->/when (not y-stopped?)
                                                   (->/in [:y] (+ y-step))))))
                           collisions
                           x-steps
                           y-steps))))))
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
