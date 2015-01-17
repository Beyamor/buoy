(ns sinai.scenes
  (:require [sinai.rules :as r]
            [sinai.entities :as e]))

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
  (reduce (fn [app mover]
            (update-in app [:scene :entities]
                       e/update mover
                       #(-> mover
                            (update-in [:position :x]
                                       + (-> mover :velocity :x))
                            (update-in [:position :y]
                                       + (-> mover :velocity :y)))))
          app
          (-> app :scene :entities (e/get-with #{:position :velocity}))))

(defrecord StandardScene
  [rules handlers entities]
  Scene
  (update [_ app]
    (let [messages (r/apply-rules app (:frame-entered rules))]
      (-> app
        (apply-handlers handlers messages)
        apply-physics))))

(defn create-scene
  [& {:keys [rules handlers entities]}]
  (let [rules (r/collect rules)]
    (->StandardScene rules handlers entities)))
