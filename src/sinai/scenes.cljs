(ns sinai.scenes
  (:require [sinai.rules :as r]))

(defprotocol Scene
  (update [_ app]))

(defn apply-handlers
  [state handlers messages]
  (reduce (fn [state [message-type & data]]
            (if-let [handle (get handlers message-type)]
              (apply handle state data)
              (throw (str "Unhandled message type: " message-type))))
          state messages))

(defrecord StandardScene
  [rules handlers entities]
  Scene
  (update [_ app]
    (let [state {:app app}
          messages (r/apply-rules state (:frame-entered rules))]
      (apply-handlers app handlers messages))))

(defn create-scene
  [& {:keys [rules handlers entities]}]
  (let [rules (r/collect rules)]
    (->StandardScene rules handlers entities)))
