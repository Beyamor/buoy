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
    (let [messages (r/apply-rules app rules)]
      (apply-handlers app handlers messages))))
