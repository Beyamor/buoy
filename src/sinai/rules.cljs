(ns sinai.rules
  (:require [sinai.entities :as e])
  (:require-macros [sinai.rules :as m]))

(defn collect
  [rules]
  (reduce (fn [actions rule]
            (update-in actions [(:trigger rule)] (fnil conj []) rule))
          {} rules)) 

(defn get-messages
  [[_ messages]]
  messages)

(defn get-state
  [state]
  [state nil])

(defn return
  [value]
  (fn [state]
    [value nil]))

(defn apply-rules
  [state rules]
  (apply concat
         (map #(get-messages ((:action %) state)) rules)))

(defn get-in-state
  [& path]
  (m/do state <- get-state
        (return (get-in state path))))

(defn get-in-app
  [& path]
  (m/do app <- (get-in-state :app)
        (return (get-in app path))))

(def get-scene
  (get-in-app :scene))

(defn get-in-scene
  [& path]
  (m/do scene <- get-scene
        (return (get-in scene path))))

(def get-entities
  (m/do entities <- (get-in-scene :entities)
        (return (e/get-all-ids entities))))

(defn get-entities-with
  [components]
  (m/do entities <- (get-in-scene :entities)
        (return (e/get-with entities components))))

(defn send-message
  [message-type & data]
  (fn [state]
    [nil [(concat [message-type] data)]]))

(defn send-entity-update-message
  [e f & args]
  (send-message :update-entity e #(apply f % args)))

(defn stop
  [e]
  (send-message :stop e))
