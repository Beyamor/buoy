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
            (let [collision-rules (->> (:scene app) :rules :collision
                                       (filter #(e/has-components? (-> app :scene :entities (e/get mover-id))
                                                                   (:components1 %))))]
              (loop [app app]
                (let [entities (-> app :scene :entities)
                      mover (e/get entities mover-id)
                      messages (for [collision-rule collision-rules
                                     other-id (e/get-with entities (:components2 collision-rule))
                                     :let [other (e/get entities other-id)]
                                     :when (e/collide? mover other)
                                     :let [action ((:action collision-rule)
                                                   mover other)]
                                     message (r/get-messages
                                               (action app))]
                                 message)
                      stopped? (some #(-> % first (= :stop)) messages)]
                  (if stopped?
                    app
                    (update-in app [:scene :entities]
                               e/update mover
                               #(-> %
                                    (update-in [:position :x]
                                               + (-> % :velocity :x util/signum))
                                    (update-in [:position :y]
                                               + (-> % :velocity :y util/signum)))))))))
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
